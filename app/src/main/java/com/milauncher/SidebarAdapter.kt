package com.milauncher

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.milauncher.models.SidebarItem
import com.milauncher.util.SettingsManager

class SidebarAdapter(
    private val context: Context,
    private val items: List<SidebarItem>,
    private val onItemSelected: (SidebarItem) -> Unit
) : RecyclerView.Adapter<SidebarAdapter.SidebarViewHolder>() {

    private var selectedPosition = 0
    private val settings = SettingsManager(context)

    fun setSelected(position: Int) {
        val old = selectedPosition
        selectedPosition = position
        notifyItemChanged(old)
        notifyItemChanged(position)
    }

    fun getSelectedPosition() = selectedPosition

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SidebarViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_sidebar_item, parent, false)
        return SidebarViewHolder(view)
    }

    override fun onBindViewHolder(holder: SidebarViewHolder, position: Int) {
        val item = items[position]
        holder.icon.setImageResource(item.iconResId)
        holder.label.text = item.label

        val isSelected = position == selectedPosition
        val accentColor = try { Color.parseColor(settings.accentColor) } catch (e: Exception) { Color.parseColor("#FFCC00") }

        if (isSelected) {
            holder.icon.alpha = 1f
            holder.icon.setColorFilter(accentColor)
            holder.label.setTextColor(accentColor)
            holder.itemView.setBackgroundResource(R.drawable.bg_sidebar_item_focused)
        } else {
            holder.icon.alpha = 0.6f
            holder.icon.clearColorFilter()
            holder.label.setTextColor(Color.parseColor("#88FFFFFF"))
            holder.itemView.setBackgroundResource(R.drawable.bg_sidebar_item_normal)
        }

        holder.itemView.setOnFocusChangeListener { v, hasFocus ->
            val currentlySelected = (holder.adapterPosition == selectedPosition)
            if (hasFocus) {
                v.animate().scaleX(1.1f).scaleY(1.1f).setDuration(150).start()
                holder.icon.alpha = 1f
                if (!currentlySelected) holder.icon.clearColorFilter()
                
                if (!currentlySelected) {
                    v.post {
                        val pos = holder.adapterPosition
                        if (pos != RecyclerView.NO_POSITION && selectedPosition != pos) {
                            setSelected(pos)
                            onItemSelected(items[pos])
                        }
                    }
                }
            } else {
                v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(150).start()
                if (!currentlySelected) {
                    holder.icon.alpha = 0.6f
                }
            }
        }

        holder.itemView.setOnClickListener {
            setSelected(holder.adapterPosition)
            onItemSelected(item)
        }
    }

    override fun getItemCount() = items.size

    class SidebarViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val icon: ImageView = itemView.findViewById(R.id.sidebar_icon)
        val label: TextView = itemView.findViewById(R.id.sidebar_label)
    }
}
