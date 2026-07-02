package com.example.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

data class LenientString(val value: String?)

// Note: No @JsonClass annotation so Moshi uses our custom adapters in ShamelaApi.kt
data class Category(
    val id: Int,
    val name: String,
    val book_count: Int? = 0
)

// Note: No @JsonClass annotation so Moshi uses our custom adapters in ShamelaApi.kt
data class Author(
    val id: Int,
    val name: String,
    val death_hijri: Int? = null,
    val book_count: Int? = 0,
    val biography: String? = null
)

// Note: No @JsonClass annotation so Moshi uses our custom adapters in ShamelaApi.kt
data class Book(
    val id: Int,
    val title: String,
    val author_id: Int? = null,
    val author_name: String? = null,
    val category_id: Int? = null,
    val category_name: String? = null,
    val page_count: Int? = 0,
    val description: String? = null,
    val is_downloaded: Boolean? = false,
    val cover_url: String? = null
)

@JsonClass(generateAdapter = true)
data class DashboardData(
    val featuredCategories: List<Category>? = emptyList(),
    val recentBooks: List<Book>? = emptyList(),
    val notableBooks: List<Book>? = emptyList()
)

@JsonClass(generateAdapter = true)
data class ApiResponse<T>(
    val success: Boolean,
    val data: T
)

@JsonClass(generateAdapter = true)
data class ApiListMeta(
    val page: Int,
    val limit: Int,
    val total: Int,
    val totalPages: Int
)

@JsonClass(generateAdapter = true)
data class ApiListResponse<T>(
    val success: Boolean,
    val data: List<T>,
    val meta: ApiListMeta? = null
)

@JsonClass(generateAdapter = true)
data class TocItem(
    val title_id: Int? = 0,
    val title_text: String? = "",
    val target_global_order: Int? = 0,
    val parent_id: Int? = null,
    val children: List<TocItem>? = emptyList(),
    
    // Helper fields mapped inside ShamelaRepository for UI compatibility
    val id: Int = 0,
    val title: String = "",
    val level: Int = 0,
    val global_order: Int = 0,
    val page_num: Int = 0,
    val part: LenientString? = null
)

@JsonClass(generateAdapter = true)
data class PageNavigation(
    val hasNext: Boolean,
    val nextGlobalOrder: Int? = null,
    val prevGlobalOrder: Int? = null
)

@JsonClass(generateAdapter = true)
data class PageContent(
    val id: Int,
    val book_id: Int,
    val part: LenientString? = null,
    val page_num: Int,
    val global_order: Int,
    val content: String,
    val clean_content: String? = null,
    @Json(name = "footnotes") val footnote: String? = null,
    val section_title: String? = null,
    val navigation: PageNavigation? = null
)

@JsonClass(generateAdapter = true)
data class SearchResultItem(
    val book_id: Int,
    val book_title: String,
    val author_name: String? = null,
    val part: LenientString? = null,
    val page_num: Int? = null,
    val global_order: Int,
    val snippet: String? = null,
    val section_title: String? = null
)

@JsonClass(generateAdapter = true)
data class ResolveResult(
    val book_id: Int,
    val part: LenientString? = null,
    val page_num: Int? = null,
    val global_order: Int
)
