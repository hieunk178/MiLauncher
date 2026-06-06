package com.milauncher

import android.content.Context
import android.content.Intent
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
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class HomeAdapter(private val context: Context, private var categories: List<HomeCategory>) :
    RecyclerView.Adapter<HomeAdapter.CategoryViewHolder>() {

    fun updateData(newCategories: List<HomeCategory>) {
        categories = newCategories
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_home_category, parent, false)
        return CategoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        val category = categories[position]
        holder.tvTitle.text = category.title

        val widthDp = when (category.cardSize) {
            0 -> 160
            1 -> 200
            else -> 240
        }
        val availableWidthDp = 864
        val marginDp = 16
        val maxColumns = availableWidthDp / (widthDp + marginDp)
        
        val actualColumns = if (category.columns == 0 || category.columns > maxColumns) {
            maxColumns
        } else {
            category.columns
        }

        if (category.layoutType == 1) {
            // Grid
            holder.rvItems.layoutManager = GridLayoutManager(context, actualColumns)
            val itemAdapter = CategoryItemAdapter(context, category.items, category.cardSize, 0)
            holder.rvItems.adapter = itemAdapter
        } else {
            // Row
            holder.rvItems.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            
            // Calculate stretched width so exactly actualColumns fit on screen
            val stretchedWidthDp = (availableWidthDp / actualColumns) - marginDp
            val itemAdapter = CategoryItemAdapter(context, category.items, category.cardSize, stretchedWidthDp)
            holder.rvItems.adapter = itemAdapter
        }
    }

    override fun getItemCount() = categories.size

    class CategoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvTitle: TextView = itemView.findViewById(R.id.tv_category_title)
        val rvItems: RecyclerView = itemView.findViewById(R.id.rv_category_items)
    }
}

class CategoryItemAdapter(
    private val context: Context,
    private val items: List<AppItem>,
    private val cardSize: Int,
    private val overrideWidthDp: Int
) : RecyclerView.Adapter<CategoryItemAdapter.ItemViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_glass_card, parent, false)
        
        // Calculate dimensions based on cardSize
        var width = when (cardSize) {
            0 -> 160 // Small
            1 -> 200 // Medium
            else -> 240 // Large
        }
        
        if (overrideWidthDp > 0) {
            width = overrideWidthDp
        }
        
        val height = when (cardSize) {
            0 -> 90 // Small
            1 -> 110 // Medium
            else -> 140 // Large
        }

        val density = context.resources.displayMetrics.density
        val pxWidth = (width * density).toInt()
        val pxHeight = (height * density).toInt()

        val layoutParams = view.layoutParams
        layoutParams.width = pxWidth
        layoutParams.height = pxHeight
        view.layoutParams = layoutParams

        return ItemViewHolder(view)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        val item = items[position]
        holder.tvTitle.text = item.title

        // Setup Icon and Background
        when (item.type) {
            AppItem.ItemType.HDMI -> {
                holder.ivIcon.setImageResource(R.drawable.ic_hdmi)
                holder.itemView.setBackgroundResource(R.drawable.bg_glass_card)
            }
            AppItem.ItemType.APP -> {
                holder.ivIcon.setImageResource(R.drawable.ic_app_default)
                holder.itemView.setBackgroundResource(R.drawable.bg_glass_card)
            }
            AppItem.ItemType.SETTINGS -> {
                holder.ivIcon.setImageResource(R.drawable.ic_settings)
                holder.itemView.setBackgroundResource(R.drawable.bg_glass_card)
            }
            AppItem.ItemType.OTA -> {
                holder.ivIcon.setImageResource(R.drawable.ic_settings)
                holder.itemView.setBackgroundResource(R.drawable.bg_glass_card)
            }
        }

        // Focus animation (Scale up 1.15x)
        holder.itemView.setOnFocusChangeListener { v, hasFocus ->
            if (hasFocus) {
                v.animate().scaleX(1.15f).scaleY(1.15f).translationZ(10f).setDuration(200).start()
                holder.tvTitle.setTextColor(Color.parseColor("#FFCC00")) // Highlight text
            } else {
                v.animate().scaleX(1.0f).scaleY(1.0f).translationZ(0f).setDuration(200).start()
                holder.tvTitle.setTextColor(Color.WHITE)
            }
        }

        // Click listener
        holder.itemView.setOnClickListener {
            when (item.type) {
                AppItem.ItemType.APP -> {
                    item.packageName?.let { pkg ->
                        AppLauncher.launchApp(context, pkg)
                    }
                }
                AppItem.ItemType.SETTINGS -> {
                    context.startActivity(Intent(context, SettingsActivity::class.java))
                }
                AppItem.ItemType.OTA -> {
                    val lifecycleOwner = holder.itemView.findViewTreeLifecycleOwner()
                    lifecycleOwner?.lifecycleScope?.launch {
                        OtaUpdater.checkAndUpdate(context)
                    }
                }
                AppItem.ItemType.HDMI -> {
                    val port = if (item.title.contains("1")) 1 else if (item.title.contains("2")) 2 else 3
                    AppLauncher.launchHdmi(context, port)
                }
            }
        }
    }

    override fun getItemCount() = items.size

    class ItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivIcon: ImageView = itemView.findViewById(R.id.card_icon)
        val tvTitle: TextView = itemView.findViewById(R.id.card_title)
    }
}
