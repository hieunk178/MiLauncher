package com.milauncher.models

data class AppItem(
    val id: Long,
    val title: String,
    val packageName: String? = null,
    val type: ItemType = ItemType.APP,
    val iconResId: Int = android.R.drawable.sym_def_app_icon // Default icon
) {
    enum class ItemType {
        HDMI, APP, SETTINGS, OTA
    }
}
