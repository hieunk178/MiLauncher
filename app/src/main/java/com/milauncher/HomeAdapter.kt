package com.milauncher

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.milauncher.models.AppItem
import com.milauncher.models.HomeCategory
import com.milauncher.util.AppLauncher
import com.milauncher.util.OtaUpdater
import com.milauncher.util.SettingsManager
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class HomeAdapter(private val context: Context, private var categories: List<HomeCategory>) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        const val VIEW_TYPE_FEATURED = 0
        const val VIEW_TYPE_ROW = 1
    }

    fun updateData(newCategories: List<HomeCategory>) {
        categories = newCategories
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        return if (categories[position].isFeatured && position == 0) VIEW_TYPE_FEATURED else VIEW_TYPE_ROW
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_FEATURED) {
            val view = LayoutInflater.from(context).inflate(R.layout.item_featured_banner, parent, false)
            FeaturedViewHolder(view)
        } else {
            val view = LayoutInflater.from(context).inflate(R.layout.item_home_category, parent, false)
            CategoryViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val category = categories[position]
        if (holder is FeaturedViewHolder) {
            bindFeatured(holder, category)
        } else if (holder is CategoryViewHolder) {
            bindCategory(holder, category)
        }
    }

    private fun bindFeatured(holder: FeaturedViewHolder, category: HomeCategory) {
        val firstItem = category.items.firstOrNull() ?: return
        holder.title.text = firstItem.title
        holder.subtitle.text = firstItem.subtitle ?: "Nhấn để mở"

        // Load banner or icon as featured image
        when {
            firstItem.bannerDrawable != null -> holder.image.setImageDrawable(firstItem.bannerDrawable)
            firstItem.iconDrawable != null -> holder.image.setImageDrawable(firstItem.iconDrawable)
            else -> holder.image.setImageResource(R.drawable.bg_card_hdmi)
        }

        holder.itemView.setOnFocusChangeListener { v, hasFocus ->
            val glow = holder.glow as? com.milauncher.util.GlowShadowView
            if (hasFocus) {
                v.animate().scaleX(1.03f).scaleY(1.03f).setDuration(200).start()
                glow?.animate()?.alpha(1f)?.setDuration(200)?.start()
                holder.playIcon.animate().alpha(1f).setDuration(200).start()
            } else {
                v.animate().scaleX(1f).scaleY(1f).setDuration(200).start()
                glow?.animate()?.alpha(0f)?.setDuration(200)?.start()
                holder.playIcon.animate().alpha(0f).setDuration(200).start()
            }
        }

        holder.itemView.setOnClickListener {
            firstItem.packageName?.let { pkg -> AppLauncher.launchApp(context, pkg) }
        }
    }

    private fun bindCategory(holder: CategoryViewHolder, category: HomeCategory) {
        holder.tvTitle.text = category.title

        val density = context.resources.displayMetrics.density
        val widthDp = when (category.cardSize) { 0 -> 140; 1 -> 196; else -> 260 }
        val marginDp = 12
        // Activity right panel is screen_width - sidebar(72dp) - paddings(48dp) = roughly 840dp on 1080p
        val availableWidthDp = 840
        val maxColumns = availableWidthDp / (widthDp + marginDp)
        val actualColumns = if (category.columns == 0 || category.columns > maxColumns) maxColumns else category.columns

        if (category.layoutType == 1) {
            holder.rvItems.layoutManager = GridLayoutManager(context, actualColumns)
        } else {
            holder.rvItems.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        }

        val itemAdapter = CategoryItemAdapter(context, category.items, category.cardSize)
        holder.rvItems.adapter = itemAdapter
    }

    override fun getItemCount() = categories.size

    class FeaturedViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val image: ImageView = itemView.findViewById(R.id.featured_banner_image)
        val title: TextView = itemView.findViewById(R.id.featured_title)
        val subtitle: TextView = itemView.findViewById(R.id.featured_subtitle)
        val glow: View? = itemView.findViewById(R.id.featured_glow)
        val playIcon: ImageView = itemView.findViewById(R.id.featured_play_icon)
    }

    class CategoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvTitle: TextView = itemView.findViewById(R.id.tv_category_title)
        val rvItems: RecyclerView = itemView.findViewById(R.id.rv_category_items)
    }
}

// ──────────────────────────────────────────────────────────────────────────────

class CategoryItemAdapter(
    private val context: Context,
    private val items: List<AppItem>,
    private val cardSize: Int
) : RecyclerView.Adapter<CategoryItemAdapter.ItemViewHolder>() {

    companion object {
        const val TYPE_BANNER = 0
        const val TYPE_ICON   = 1
        const val TYPE_HDMI   = 2
    }

    override fun getItemViewType(position: Int): Int {
        val item = items[position]
        return when (item.type) {
            AppItem.ItemType.HDMI -> TYPE_HDMI
            else -> if (item.bannerDrawable != null) TYPE_BANNER else TYPE_ICON
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val layoutId = when (viewType) {
            TYPE_BANNER -> R.layout.item_card_banner
            TYPE_HDMI   -> R.layout.item_card_hdmi
            else        -> R.layout.item_card_icon
        }
        val view = LayoutInflater.from(context).inflate(layoutId, parent, false)
        
        // Force width and height to prevent GridLayoutManager overlap or wrap_content issues
        val density = context.resources.displayMetrics.density
        // Increase width to make the cards wider (e.g. 196dp fits 4 columns perfectly on 840dp space)
        val widthDp = when (cardSize) { 0 -> 140; 1 -> 196; else -> 260 }
        // Adjust height to maintain a nice rectangle aspect ratio (closer to 16:9)
        val heightDp = when (cardSize) { 0 -> 80; 1 -> 110; else -> 146 }
        
        val lp = view.layoutParams
        if (lp != null) {
            lp.width = (widthDp * density).toInt()
            lp.height = (heightDp * density).toInt()
            view.layoutParams = lp
        }

        return ItemViewHolder(view, viewType)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        val item = items[position]
        val settings = SettingsManager(context)
        val density = context.resources.displayMetrics.density
        val accentColor = try { Color.parseColor(settings.accentColor) } catch (e: Exception) { Color.parseColor("#FFCC00") }

        // Apply rounded corner clipping to the icon/banner view
        holder.ivIcon?.apply {
            outlineProvider = object : android.view.ViewOutlineProvider() {
                override fun getOutline(view: android.view.View, outline: android.graphics.Outline) {
                    outline.setRoundRect(0, 0, view.width, view.height, 10f * density)
                }
            }
            clipToOutline = true
        }

        // Bind content
        holder.tvTitle?.text = item.title
        when (holder.viewType) {
            TYPE_BANNER -> {
                item.bannerDrawable?.let { holder.ivIcon?.setImageDrawable(it) }
                holder.tvTitle?.visibility = View.GONE
            }
            TYPE_HDMI -> {
                holder.ivIcon?.setImageResource(R.drawable.ic_hdmi)
            }
            TYPE_ICON -> {
                val drawable = item.iconDrawable
                if (drawable != null) holder.ivIcon?.setImageDrawable(drawable)
                else holder.ivIcon?.setImageResource(R.drawable.ic_app_default)
            }
        }

        // Focus animation
        holder.itemView.setOnFocusChangeListener { v, hasFocus ->
            val glow = holder.glowView as? com.milauncher.util.GlowShadowView
            val existingAnim = glow?.getTag(R.id.focus_animator_tag) as? android.animation.AnimatorSet
            existingAnim?.cancel()
            v.animate().cancel()

            if (hasFocus) {
                holder.tvTitle?.setTextColor(accentColor)
                v.translationZ = 12f
                v.animate().scaleX(1.15f).scaleY(1.15f).setDuration(180).start()
                if (glow != null) {
                    glow.alpha = 1f
                    when (settings.focusAnimationStyle) {
                        1 -> { // Heartbeat
                            val anim = android.animation.ObjectAnimator.ofFloat(glow, "glowRadius", 12f, 22f, 15f, 26f, 12f)
                            anim.duration = 800; anim.repeatCount = android.animation.ValueAnimator.INFINITE
                            val set = android.animation.AnimatorSet(); set.play(anim); set.start()
                            glow.setTag(R.id.focus_animator_tag, set)
                        }
                        2 -> { // Breathing
                            val r = android.animation.ObjectAnimator.ofFloat(glow, "glowRadius", 10f, 22f, 10f)
                            val a = android.animation.ObjectAnimator.ofFloat(glow, "alpha", 0.5f, 1f, 0.5f)
                            r.duration = 1500; a.duration = 1500
                            r.repeatCount = android.animation.ValueAnimator.INFINITE
                            a.repeatCount = android.animation.ValueAnimator.INFINITE
                            val set = android.animation.AnimatorSet(); set.playTogether(r, a); set.start()
                            glow.setTag(R.id.focus_animator_tag, set)
                        }
                        else -> glow.glowRadius = 15f
                    }
                }
            } else {
                holder.tvTitle?.setTextColor(Color.WHITE)
                v.translationZ = 0f
                v.animate().scaleX(1f).scaleY(1f).setDuration(180).start()
                glow?.alpha = 0f
                glow?.glowRadius = 12f
            }
        }

        // Click
        holder.itemView.setOnClickListener {
            when (item.type) {
                AppItem.ItemType.APP, AppItem.ItemType.PHONE_APP ->
                    item.packageName?.let { pkg -> AppLauncher.launchApp(context, pkg) }
                AppItem.ItemType.HDMI -> {
                    val port = when {
                        item.title.contains("1") -> 1
                        item.title.contains("2") -> 2
                        item.title.contains("3") -> 3
                        else -> 4
                    }
                    AppLauncher.launchHdmi(context, port)
                }
                AppItem.ItemType.SETTINGS ->
                    context.startActivity(android.content.Intent(context, SettingsActivity::class.java))
                AppItem.ItemType.STORE ->
                    AppLauncher.launchAppStore(context)
                AppItem.ItemType.OTA -> {
                    val lo = holder.itemView.findViewTreeLifecycleOwner()
                    lo?.lifecycleScope?.launch { OtaUpdater.checkAndUpdate(context) }
                }
            }
        }
    }

    override fun getItemCount() = items.size

    class ItemViewHolder(itemView: View, val viewType: Int) : RecyclerView.ViewHolder(itemView) {
        val ivIcon: ImageView? = itemView.findViewById(R.id.card_icon)
        val tvTitle: TextView? = itemView.findViewById(R.id.card_title)
        val glowView: View? = itemView.findViewById(R.id.card_glow)
    }
}
