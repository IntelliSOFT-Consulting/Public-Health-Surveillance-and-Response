package com.icl.nphi.monitor

data class PageInfo(
    val pageSize: Int = 50,
    val currentPage: Int = 0,
    val hasMore: Boolean = true
)
