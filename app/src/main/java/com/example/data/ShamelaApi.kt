package com.example.data

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.*
import java.util.concurrent.TimeUnit

interface ShamelaApiService {
    @GET("dashboard")
    suspend fun getDashboard(): ApiResponse<DashboardData>

    @GET("categories")
    suspend fun getCategories(): ApiResponse<List<Category>>

    @GET("categories/{id}/books")
    suspend fun getCategoryBooks(
        @Path("id") id: Int,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20
    ): ApiListResponse<Book>

    @GET("books")
    suspend fun getBooks(
        @Query("page") page: Int = 1,
        @Query("search") search: String? = null,
        @Query("limit") limit: Int = 20
    ): ApiListResponse<Book>

    @GET("books/{id}")
    suspend fun getBookDetail(
        @Path("id") id: Int
    ): ApiResponse<Book>

    @GET("books/{id}/toc")
    suspend fun getToc(
        @Path("id") id: Int,
        @Query("flat") flat: Boolean = true
    ): ApiResponse<List<TocItem>>

    @GET("books/{id}/pages/{order}")
    suspend fun getPage(
        @Path("id") id: Int,
        @Path("order") order: Int,
        @Query("format") format: String = "html"
    ): ApiResponse<PageContent>

    @GET("books/{id}/resolve")
    suspend fun resolvePage(
        @Path("id") id: Int,
        @Query("part") part: String? = null,
        @Query("page_num") pageNum: Int? = null,
        @Query("global_order") globalOrder: Int? = null
    ): ApiResponse<ResolveResult>

    @GET("search")
    suspend fun search(
        @Query("q") q: String,
        @Query("scope") scope: String = "pages",
        @Query("book_id") bookId: Int? = null,
        @Query("limit") limit: Int = 20
    ): ApiListResponse<SearchResultItem>
}

object ShamelaApiClient {
    private const val BASE_URL = "https://winongkencono-shamelah.hf.space/api/"

    private object LenientBooleanAdapter {
        @com.squareup.moshi.FromJson
        fun fromJson(reader: com.squareup.moshi.JsonReader): Boolean {
            return when (reader.peek()) {
                com.squareup.moshi.JsonReader.Token.BOOLEAN -> reader.nextBoolean()
                com.squareup.moshi.JsonReader.Token.NUMBER -> {
                    val value = reader.nextDouble()
                    value != 0.0
                }
                com.squareup.moshi.JsonReader.Token.STRING -> {
                    val value = reader.nextString()
                    value.equals("true", ignoreCase = true) || value == "1"
                }
                com.squareup.moshi.JsonReader.Token.NULL -> {
                    reader.nextNull<Unit>()
                    false
                }
                else -> {
                    reader.skipValue()
                    false
                }
            }
        }

        @com.squareup.moshi.ToJson
        fun toJson(writer: com.squareup.moshi.JsonWriter, value: Boolean) {
            writer.value(value)
        }
    }

    private object LenientStringAdapter {
        @com.squareup.moshi.FromJson
        fun fromJson(reader: com.squareup.moshi.JsonReader): LenientString {
            val str = when (reader.peek()) {
                com.squareup.moshi.JsonReader.Token.NULL -> {
                    reader.nextNull<Unit>()
                    null
                }
                com.squareup.moshi.JsonReader.Token.STRING -> reader.nextString()
                com.squareup.moshi.JsonReader.Token.NUMBER -> {
                    val d = reader.nextDouble()
                    if (d % 1.0 == 0.0) {
                        d.toLong().toString()
                    } else {
                        d.toString()
                    }
                }
                com.squareup.moshi.JsonReader.Token.BOOLEAN -> reader.nextBoolean().toString()
                else -> {
                    reader.skipValue()
                    null
                }
            }
            return LenientString(str)
        }

        @com.squareup.moshi.ToJson
        fun toJson(writer: com.squareup.moshi.JsonWriter, value: LenientString?) {
            writer.value(value?.value)
        }
    }

    private val moshi = Moshi.Builder()
        .add(LenientBooleanAdapter)
        .add(LenientStringAdapter)
        .add(CategoryAdapter)
        .add(AuthorAdapter)
        .add(BookAdapter)
        .addLast(KotlinJsonAdapterFactory())
        .build()

    object CategoryAdapter {
        @com.squareup.moshi.FromJson
        fun fromJson(reader: com.squareup.moshi.JsonReader): Category {
            var id = 0
            var name = ""
            var bookCount: Int? = 0

            reader.beginObject()
            while (reader.hasNext()) {
                when (reader.nextName()) {
                    "id" -> {
                        id = if (reader.peek() == com.squareup.moshi.JsonReader.Token.NULL) {
                            reader.nextNull<Unit>()
                            0
                        } else {
                            reader.nextInt()
                        }
                    }
                    "name", "name_ar" -> {
                        name = if (reader.peek() == com.squareup.moshi.JsonReader.Token.NULL) {
                            reader.nextNull<Unit>()
                            ""
                        } else {
                            reader.nextString()
                        }
                    }
                    "book_count" -> {
                        bookCount = if (reader.peek() == com.squareup.moshi.JsonReader.Token.NULL) {
                            reader.nextNull<Unit>()
                            0
                        } else {
                            reader.nextInt()
                        }
                    }
                    else -> reader.skipValue()
                }
            }
            reader.endObject()
            return Category(id = id, name = name, book_count = bookCount)
        }

        @com.squareup.moshi.ToJson
        fun toJson(writer: com.squareup.moshi.JsonWriter, value: Category) {
            writer.beginObject()
            writer.name("id").value(value.id)
            writer.name("name").value(value.name)
            writer.name("book_count").value(value.book_count)
            writer.endObject()
        }
    }

    object AuthorAdapter {
        @com.squareup.moshi.FromJson
        fun fromJson(reader: com.squareup.moshi.JsonReader): Author {
            var id = 0
            var name = ""
            var deathHijri: Int? = null
            var bookCount: Int? = 0
            var biography: String? = null

            reader.beginObject()
            while (reader.hasNext()) {
                when (reader.nextName()) {
                    "id", "author_id" -> {
                        id = if (reader.peek() == com.squareup.moshi.JsonReader.Token.NULL) {
                            reader.nextNull<Unit>()
                            0
                        } else {
                            reader.nextInt()
                        }
                    }
                    "name", "name_ar" -> {
                        name = if (reader.peek() == com.squareup.moshi.JsonReader.Token.NULL) {
                            reader.nextNull<Unit>()
                            ""
                        } else {
                            reader.nextString()
                        }
                    }
                    "death_hijri" -> {
                        deathHijri = when (reader.peek()) {
                            com.squareup.moshi.JsonReader.Token.NUMBER -> reader.nextInt()
                            com.squareup.moshi.JsonReader.Token.STRING -> {
                                val str = reader.nextString()
                                str.toIntOrNull()
                            }
                            com.squareup.moshi.JsonReader.Token.NULL -> {
                                reader.nextNull<Unit>()
                                null
                            }
                            else -> {
                                reader.skipValue()
                                null
                            }
                        }
                    }
                    "book_count" -> {
                        bookCount = if (reader.peek() == com.squareup.moshi.JsonReader.Token.NULL) {
                            reader.nextNull<Unit>()
                            0
                        } else {
                            reader.nextInt()
                        }
                    }
                    "biography" -> {
                        biography = if (reader.peek() == com.squareup.moshi.JsonReader.Token.NULL) {
                            reader.nextNull<Unit>()
                            null
                        } else {
                            reader.nextString()
                        }
                    }
                    else -> reader.skipValue()
                }
            }
            reader.endObject()
            return Author(id, name, deathHijri, bookCount, biography)
        }

        @com.squareup.moshi.ToJson
        fun toJson(writer: com.squareup.moshi.JsonWriter, value: Author) {
            writer.beginObject()
            writer.name("id").value(value.id)
            writer.name("name").value(value.name)
            writer.name("death_hijri").value(value.death_hijri)
            writer.name("book_count").value(value.book_count)
            writer.name("biography").value(value.biography)
            writer.endObject()
        }
    }

    object BookAdapter {
        @com.squareup.moshi.FromJson
        fun fromJson(reader: com.squareup.moshi.JsonReader): Book {
            var id = 0
            var title = ""
            var authorId: Int? = null
            var authorName: String? = null
            var categoryId: Int? = null
            var categoryName: String? = null
            var pageCount: Int? = 0
            var description: String? = null
            var isDownloaded: Boolean? = false
            var coverUrl: String? = null

            reader.beginObject()
            while (reader.hasNext()) {
                when (reader.nextName()) {
                    "id", "book_id" -> {
                        id = if (reader.peek() == com.squareup.moshi.JsonReader.Token.NULL) {
                            reader.nextNull<Unit>()
                            0
                        } else {
                            reader.nextInt()
                        }
                    }
                    "title", "title_ar" -> {
                        title = if (reader.peek() == com.squareup.moshi.JsonReader.Token.NULL) {
                            reader.nextNull<Unit>()
                            ""
                        } else {
                            reader.nextString()
                        }
                    }
                    "author_id", "main_author_id" -> {
                        authorId = if (reader.peek() == com.squareup.moshi.JsonReader.Token.NULL) {
                            reader.nextNull<Unit>()
                            null
                        } else {
                            reader.nextInt()
                        }
                    }
                    "author_name", "main_author_name_ar" -> {
                        authorName = if (reader.peek() == com.squareup.moshi.JsonReader.Token.NULL) {
                            reader.nextNull<Unit>()
                            null
                        } else {
                            reader.nextString()
                        }
                    }
                    "category_id" -> {
                        categoryId = if (reader.peek() == com.squareup.moshi.JsonReader.Token.NULL) {
                            reader.nextNull<Unit>()
                            null
                        } else {
                            reader.nextInt()
                        }
                    }
                    "category_name" -> {
                        categoryName = if (reader.peek() == com.squareup.moshi.JsonReader.Token.NULL) {
                            reader.nextNull<Unit>()
                            null
                        } else {
                            reader.nextString()
                        }
                    }
                    "page_count" -> {
                        pageCount = if (reader.peek() == com.squareup.moshi.JsonReader.Token.NULL) {
                            reader.nextNull<Unit>()
                            0
                        } else {
                            reader.nextInt()
                        }
                    }
                    "description", "betaka_text" -> {
                        description = if (reader.peek() == com.squareup.moshi.JsonReader.Token.NULL) {
                            reader.nextNull<Unit>()
                            null
                        } else {
                            reader.nextString()
                        }
                    }
                    "is_downloaded" -> {
                        isDownloaded = when (reader.peek()) {
                            com.squareup.moshi.JsonReader.Token.BOOLEAN -> reader.nextBoolean()
                            com.squareup.moshi.JsonReader.Token.NUMBER -> {
                                val num = reader.nextDouble()
                                num != 0.0
                            }
                            com.squareup.moshi.JsonReader.Token.STRING -> {
                                val str = reader.nextString()
                                str.equals("true", ignoreCase = true) || str == "1"
                            }
                            com.squareup.moshi.JsonReader.Token.NULL -> {
                                reader.nextNull<Unit>()
                                false
                            }
                            else -> {
                                reader.skipValue()
                                false
                            }
                        }
                    }
                    "cover_url" -> {
                        coverUrl = if (reader.peek() == com.squareup.moshi.JsonReader.Token.NULL) {
                            reader.nextNull<Unit>()
                            null
                        } else {
                            reader.nextString()
                        }
                    }
                    else -> reader.skipValue()
                }
            }
            reader.endObject()
            return Book(id, title, authorId, authorName, categoryId, categoryName, pageCount, description, isDownloaded, coverUrl)
        }

        @com.squareup.moshi.ToJson
        fun toJson(writer: com.squareup.moshi.JsonWriter, value: Book) {
            writer.beginObject()
            writer.name("id").value(value.id)
            writer.name("title").value(value.title)
            writer.name("author_id").value(value.author_id)
            writer.name("author_name").value(value.author_name)
            writer.name("category_id").value(value.category_id)
            writer.name("category_name").value(value.category_name)
            writer.name("page_count").value(value.page_count)
            writer.name("description").value(value.description)
            writer.name("is_downloaded").value(value.is_downloaded)
            writer.name("cover_url").value(value.cover_url)
            writer.endObject()
        }
    }

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .build()

    val service: ShamelaApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(ShamelaApiService::class.java)
    }
}
