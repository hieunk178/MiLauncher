package com.milauncher.models

data class HomeCategory(
    val id: Int,
    val title: String,
    val layoutType: Int,   // 0 = Row, 1 = Grid
    val cardSize: Int,     // 0 = Small, 1 = Medium, 2 = Large
    val columns: Int,      // 0 = Auto, n = Fixed
    val items: List<AppItem>,
    val isFeatured: Boolean = false  // true = hiển thị dạng banner lớn đầu trang
)
