package com.milauncher.models

data class SidebarItem(
    val id: Int,
    val label: String,
    val iconResId: Int,
    val type: Type
) {
    enum class Type {
        HOME, HDMI, ALL_APPS, PHONE_APPS, SEARCH, APP_STORE, SETTINGS
    }
}
