package com.milauncher.models

import android.graphics.drawable.Drawable

data class AppItem(
    val id: Long,
    val title: String,
    val packageName: String? = null,
    val type: ItemType = ItemType.APP,
    val iconResId: Int = android.R.drawable.sym_def_app_icon,
    val iconDrawable: Drawable? = null,
    val bannerDrawable: Drawable? = null,
    val subtitle: String? = null,
    val isFeatured: Boolean = false,
    val deepLinkUri: String? = null
) {
    enum class ItemType {
        HDMI, APP, PHONE_APP, SETTINGS, OTA, STORE
    }
}
