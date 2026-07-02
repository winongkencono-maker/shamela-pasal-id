package com.example.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PasalWork(
    val id: Int?,
    @Json(name = "frbr_uri") val frbrUri: String,
    val title: String,
    val number: String?,
    val year: Int?,
    val status: String?, // berlaku, dicabut, diubah
    val type: String?, // UU, PP, etc.
    @Json(name = "content_verified") val contentVerified: Boolean? = false,
    @Json(name = "type_name") val typeName: String? = null
)

@JsonClass(generateAdapter = true)
data class PasalSearchMetadata(
    val type: String?,
    @Json(name = "node_type") val nodeType: String?,
    @Json(name = "node_number") val nodeNumber: String?
)

@JsonClass(generateAdapter = true)
data class PasalSearchResult(
    val id: Int,
    val snippet: String?,
    val metadata: PasalSearchMetadata?,
    val score: Double?,
    val work: PasalWork?
)

@JsonClass(generateAdapter = true)
data class PasalDidYouMean(
    @Json(name = "work_id") val workId: Int?,
    val similarity: Double?,
    val work: PasalWork?
)

@JsonClass(generateAdapter = true)
data class PasalSearchResponse(
    val query: String,
    val total: Int,
    val results: List<PasalSearchResult>?,
    @Json(name = "did_you_mean") val didYouMean: List<PasalDidYouMean>? = emptyList()
)

@JsonClass(generateAdapter = true)
data class PasalListResponse(
    val total: Int,
    val limit: Int,
    val offset: Int,
    val laws: List<PasalWork>?
)

@JsonClass(generateAdapter = true)
data class PasalArticle(
    val id: Int,
    val type: String, // bab, pasal, etc.
    val number: String?,
    val heading: String?,
    val content: String?,
    @Json(name = "parent_id") val parentId: Int?,
    @Json(name = "sort_order") val sortOrder: Int?
)

@JsonClass(generateAdapter = true)
data class PasalRelationship(
    val type: String?, // Mengubah, Amends, etc.
    @Json(name = "type_en") val typeEn: String?,
    @Json(name = "related_work") val relatedWork: PasalWork?
)

@JsonClass(generateAdapter = true)
data class PasalDetailAccess(
    val level: String?,
    @Json(name = "articles_included") val articlesIncluded: Boolean?,
    @Json(name = "access_via") val accessVia: String?
)

@JsonClass(generateAdapter = true)
data class PasalDetailResponse(
    val work: PasalWork,
    val articles: List<PasalArticle>?,
    @Json(name = "detail_access") val detailAccess: PasalDetailAccess?,
    val relationships: List<PasalRelationship>? = emptyList()
)
