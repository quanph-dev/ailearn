package com.example.basekmp.data

/**
 * Catalog entry for Video Shadowing catalog.
 * 100% migrated from original app's CatalogEntry.java.
 */
data class CatalogEntry(
    val id: String,
    val title: String,
    val titles: Map<String, String> = emptyMap(),
    val level: String,
    val category: String,
    val durationMs: Long = 0L,
    val clipUrl: String = "",
    val thumbUrl: String = "",
    val scriptUrl: String = "",
    val version: Int = 1,
    val minVersionCode: Int = 1
)
