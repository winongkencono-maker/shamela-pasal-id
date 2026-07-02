package com.example.data

import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import retrofit2.http.Streaming
import java.util.concurrent.TimeUnit
import com.example.BuildConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

@JsonClass(generateAdapter = true)
data class GeminiPart(
    val text: String? = null
)

@JsonClass(generateAdapter = true)
data class GeminiContent(
    val parts: List<GeminiPart>
)

@JsonClass(generateAdapter = true)
data class GeminiRequest(
    val contents: List<GeminiContent>,
    val systemInstruction: GeminiContent? = null
)

@JsonClass(generateAdapter = true)
data class GeminiCandidate(
    val content: GeminiContent
)

@JsonClass(generateAdapter = true)
data class GeminiResponse(
    val candidates: List<GeminiCandidate>? = null
)

interface GeminiApiService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GeminiRequest
    ): GeminiResponse

    @POST("v1beta/models/gemini-3.5-flash:streamGenerateContent")
    @Streaming
    suspend fun generateContentStream(
        @Query("key") apiKey: String,
        @Query("alt") alt: String = "sse",
        @Body request: GeminiRequest
    ): ResponseBody
}

object GeminiApiClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    val service: GeminiApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GeminiApiService::class.java)
    }

    suspend fun getExplanation(textToExplain: String, contextBook: String, analysisType: String = "makna"): String {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return "Kunci API Gemini tidak diatur. Harap konfigurasi kunci di panel Secrets AI Studio."
        }

        val systemPrompt = when (analysisType) {
            "translate" -> """
                Anda adalah penerjemah ahli kitab klasik Arab (turats) ke dalam bahasa Indonesia yang sangat akurat, mengalir, dan ilmiah.
                Tugas Anda adalah menerjemahkan teks kitab klasik yang dikirim oleh pengguna ke dalam bahasa Indonesia dengan tingkat keterbacaan tinggi namun tetap mempertahankan keakuratan istilah fikih, hadis, ushul, maupun akidah.
                Sajikan:
                1. Terjemahan lengkap mengalir (paragraph-by-paragraph).
                2. Terjemahan per frasa/kosakata penting jika diperlukan untuk kejelasan santri.
                Tulis penjelasan dalam bahasa Indonesia yang santun dan rapi dengan format Markdown.
            """.trimIndent()
            
            "nahwu" -> """
                Anda adalah ahli linguistik bahasa Arab Klasik, Nahwu, dan Sharaf (morfologi dan sintaksis).
                Tugas Anda adalah menganalisis kaidah-kaidah tata bahasa (nahwu/sharaf/balaghah) penting yang terdapat pada teks Arab klasik yang dikirimkan.
                Sajikan:
                1. Penjelasan pola kata (wazan/sharaf) yang menarik dari kata-kata kunci.
                2. Kaidah sintaksis (nahwu) yang menonjol (seperti amil, ma'mul, marfu'at, mansubat, majrurat).
                3. Sentuhan keindahan bahasa (balaghah) jika ada.
                Tulis penjelasan dalam bahasa Indonesia yang mendidik, mudah dipahami santri, dan rapi menggunakan format Markdown.
            """.trimIndent()

            "irab" -> """
                Anda adalah ahli tata bahasa Arab khususnya bidang I'rab kalimat (analisis sintaksis detail).
                Tugas Anda adalah menjabarkan I'rab dari kata demi kata atau frasa demi frasa pada kalimat teks Arab klasik yang dikirimkan secara detail, sistematis, dan sangat mudah dipelajari.
                Sajikan dalam format yang sangat terstruktur (seperti tabel Markdown atau daftar poin yang rapi) menerangkan kedudukan masing-masing kata (fa'il, maf'ul bih, mubtada', khabar, mudhaf ilaih, dll) beserta harakat dan tanda i'rabnya (dhammah, fathah, kasrah, sukun, dll).
                Gunakan bahasa Indonesia yang santun dan jelas.
            """.trimIndent()

            "fiqh" -> """
                Anda adalah ulama ahli Fikih dan Ushul Fikih lintas madzhab (khususnya madzhab Syafii, Hanafi, Maliki, Hanbali).
                Tugas Anda adalah menganalisis kandungan hukum fikih, argumen hukum, atau implikasi syariah dari teks kitab klasik yang dikirimkan secara objektif, mendalam, dan ilmiah.
                Sajikan:
                1. Hukum fikih utama yang dikandung oleh teks tersebut.
                2. Dalil pendukung atau kaidah ushuliyyah yang berkaitan.
                3. Pandangan madzhab-madzhab fikih jika terdapat khilafiyah (perbedaan pendapat) yang relevan dengan teks.
                Tulis penjelasan dalam bahasa Indonesia yang ilmiah, moderat, dan rapi menggunakan format Markdown.
            """.trimIndent()

            else -> """
                Anda adalah asisten ahli studi turats (klasik Islam) untuk aplikasi Shamela.
                Tugas Anda adalah menjelaskan teks kitab klasik yang dikirim oleh pengguna secara mendalam, akurat, dan mudah dipahami.
                Sajikan penjelasan meliputi:
                1. Penjelasan kosakata sulit (jika ada).
                2. Makna secara umum dari kalimat tersebut.
                3. Konteks hukum, aqidah, tata bahasa (nahwu/sharaf), atau sastra jika relevan.
                Tulis penjelasan dalam bahasa Indonesia yang sopan, rapi, dan informatif. Gunakan format Markdown untuk keterbacaan yang sangat baik.
            """.trimIndent()
        }

        val userPrompt = "Kitab: $contextBook\n\nTeks yang ingin dianalisis:\n\"$textToExplain\""

        val request = GeminiRequest(
            contents = listOf(
                GeminiContent(parts = listOf(GeminiPart(text = userPrompt)))
            ),
            systemInstruction = GeminiContent(parts = listOf(GeminiPart(text = systemPrompt)))
        )

        return try {
            val response = service.generateContent(apiKey, request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: "Tidak ada tanggapan dari AI."
        } catch (e: Exception) {
            "Gagal menghubungi AI Assistant: ${e.localizedMessage}"
        }
    }

    suspend fun translateQueryToArabic(query: String): String {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return query
        }
        val systemPrompt = """
            Anda adalah pakar bahasa Arab klasik dan studi Islam.
            Tugas Anda adalah menganalisis kata kunci atau kalimat pencarian dari pengguna (biasanya dalam bahasa Indonesia atau campuran) dan menerjemahkannya menjadi kata atau frasa pencarian bahasa Arab klasik (tanpa harakat) yang paling akurat, relevan, dan lazim digunakan dalam kitab klasik (turats).
            
            Aturan penting:
            1. Hanya berikan hasil kata pencarian bahasa Arab (bisa berupa satu kata atau beberapa alternatif kata dipisahkan dengan spasi, atau frasa pendek).
            2. JANGAN sertakan penjelasan, pengantar, tanda baca tambahan, tanda kutip, atau harakat (syakal).
            3. Hanya teks Arab murni yang siap digunakan untuk mesin pencari teks eksak.
            
            Contoh:
            Input: cara wudhu yang benar
            Output: صفة الوضوء
            
            Input: hukum riba jual beli
            Output: حكم الربا في البيع
            
            Input: niat mandi wajib setelah haid
            Output: نية الغسل من الحيض
        """.trimIndent()

        val request = GeminiRequest(
            contents = listOf(
                GeminiContent(parts = listOf(GeminiPart(text = "Input: $query\nOutput:")))
            ),
            systemInstruction = GeminiContent(parts = listOf(GeminiPart(text = systemPrompt)))
        )

        return try {
            val response = service.generateContent(apiKey, request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text?.trim() ?: query
        } catch (e: Exception) {
            query
        }
    }

    suspend fun askAssistant(userQuery: String): String {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return "Kunci API Gemini tidak diatur. Harap konfigurasi kunci di panel Secrets AI Studio."
        }

        val systemPrompt = """
            Anda adalah asisten ahli studi turats (klasik Islam) untuk aplikasi Shamela.
            Bantu pengguna menjawab pertanyaan terkait keagamaan, fikih, hadits, tafsir, aqidah, tata bahasa Arab, atau sejarah Islam berdasarkan kitab-kitab klasik turats.
            Jawab dalam bahasa Indonesia yang ringkas, jelas, ilmiah, dan santun. Sebutkan rujukan kitab jika relevan. Gunakan format Markdown yang rapi.
        """.trimIndent()

        val request = GeminiRequest(
            contents = listOf(
                GeminiContent(parts = listOf(GeminiPart(text = userQuery)))
            ),
            systemInstruction = GeminiContent(parts = listOf(GeminiPart(text = systemPrompt)))
        )

        return try {
            val response = service.generateContent(apiKey, request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: "Tidak ada tanggapan dari AI."
        } catch (e: Exception) {
            "Gagal menghubungi AI Assistant: ${e.localizedMessage}"
        }
    }

    fun getExplanationStream(textToExplain: String, contextBook: String, analysisType: String = "makna"): Flow<String> = flow {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            emit("Kunci API Gemini tidak diatur. Harap konfigurasi kunci di panel Secrets AI Studio.")
            return@flow
        }

        val systemPrompt = when (analysisType) {
            "translate" -> """
                Anda adalah penerjemah ahli kitab klasik Arab (turats) ke dalam bahasa Indonesia yang sangat akurat, mengalir, dan ilmiah.
                Tugas Anda adalah menerjemahkan teks kitab klasik yang dikirim oleh pengguna ke dalam bahasa Indonesia dengan tingkat keterbacaan tinggi namun tetap mempertahankan keakuratan istilah fikih, hadis, ushul, maupun akidah.
                Sajikan:
                1. Terjemahan lengkap mengalir (paragraph-by-paragraph).
                2. Terjemahan per frasa/kosakata penting jika diperlukan untuk kejelasan santri.
                Tulis penjelasan dalam bahasa Indonesia yang santun dan rapi dengan format Markdown.
            """.trimIndent()
            
            "nahwu" -> """
                Anda adalah ahli linguistik bahasa Arab Klasik, Nahwu, dan Sharaf (morfologi dan sintaksis).
                Tugas Anda adalah menganalisis kaidah-kaidah tata bahasa (nahwu/sharaf/balaghah) penting yang terdapat pada teks Arab klasik yang dikirimkan.
                Sajikan:
                1. Penjelasan pola kata (wazan/sharaf) yang menarik dari kata-kata kunci.
                2. Kaidah sintaksis (nahwu) yang menonjol (seperti amil, ma'mul, marfu'at, mansubat, majrurat).
                3. Sentuhan keindahan bahasa (balaghah) jika ada.
                Tulis penjelasan dalam bahasa Indonesia yang mendidik, mudah dipahami santri, dan rapi menggunakan format Markdown.
            """.trimIndent()

            "irab" -> """
                Anda adalah ahli tata bahasa Arab khususnya bidang I'rab kalimat (analisis sintaksis detail).
                Tugas Anda adalah menjabarkan I'rab dari kata demi kata atau frasa demi frasa pada kalimat teks Arab klasik yang dikirimkan secara detail, sistematis, dan sangat mudah dipelajari.
                Sajikan dalam format yang sangat terstruktur (seperti tabel Markdown atau daftar poin yang rapi) menerangkan kedudukan masing-masing kata (fa'il, maf'ul bih, mubtada', khabar, mudhaf ilaih, dll) beserta harakat dan tanda i'rabnya (dhammah, fathah, kasrah, sukun, dll).
                Gunakan bahasa Indonesia yang santun dan jelas.
            """.trimIndent()

            "fiqh" -> """
                Anda adalah ulama ahli Fikih dan Ushul Fikih lintas madzhab (khususnya madzhab Syafii, Hanafi, Maliki, Hanbali).
                Tugas Anda adalah menganalisis kandungan hukum fikih, argumen hukum, atau implikasi syariah dari teks kitab klasik yang dikirimkan secara objektif, mendalam, dan ilmiah.
                Sajikan:
                1. Hukum fikih utama yang dikandung oleh teks tersebut.
                2. Dalil pendukung atau kaidah ushuliyyah yang berkaitan.
                3. Pandangan madzhab-madzhab fikih jika terdapat khilafiyah (perbedaan pendapat) yang relevan dengan teks.
                Tulis penjelasan dalam bahasa Indonesia yang ilmiah, moderat, dan rapi menggunakan format Markdown.
            """.trimIndent()

            else -> """
                Anda adalah asisten ahli studi turats (klasik Islam) untuk aplikasi Shamela.
                Tugas Anda adalah menjelaskan teks kitab klasik yang dikirim oleh pengguna secara mendalam, akurat, dan mudah dipahami.
                Sajikan penjelasan meliputi:
                1. Penjelasan kosakata sulit (jika ada).
                2. Makna secara umum dari kalimat tersebut.
                3. Konteks hukum, aqidah, tata bahasa (nahwu/sharaf), atau sastra jika relevan.
                Tulis penjelasan dalam bahasa Indonesia yang sopan, rapi, dan informatif. Gunakan format Markdown untuk keterbacaan yang sangat baik.
            """.trimIndent()
        }

        val userPrompt = "Kitab: $contextBook\n\nTeks yang ingin dianalisis:\n\"$textToExplain\""

        val request = GeminiRequest(
            contents = listOf(
                GeminiContent(parts = listOf(GeminiPart(text = userPrompt)))
            ),
            systemInstruction = GeminiContent(parts = listOf(GeminiPart(text = systemPrompt)))
        )

        try {
            val responseBody = service.generateContentStream(apiKey = apiKey, request = request)
            responseBody.byteStream().bufferedReader().use { reader ->
                var line: String?
                val responseAdapter = moshi.adapter(GeminiResponse::class.java)
                while (reader.readLine().also { line = it } != null) {
                    val trimmed = line?.trim() ?: continue
                    if (trimmed.startsWith("data: ")) {
                        val jsonStr = trimmed.substring(6).trim()
                        try {
                            val chunk = responseAdapter.fromJson(jsonStr)
                            val text = chunk?.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                            if (text != null) {
                                emit(text)
                            }
                        } catch (e: Exception) {
                            // ignore malformed chunks
                        }
                    }
                }
            }
        } catch (e: Exception) {
            emit("\n[Gagal menghubungi AI Assistant: ${e.localizedMessage}]")
        }
    }

    fun askAssistantStream(userQuery: String): Flow<String> = flow {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            emit("Kunci API Gemini tidak diatur. Harap konfigurasi kunci di panel Secrets AI Studio.")
            return@flow
        }

        val systemPrompt = """
            Anda adalah asisten ahli studi turats (klasik Islam) untuk aplikasi Shamela.
            Bantu pengguna menjawab pertanyaan terkait keagamaan, fikih, hadits, tafsir, aqidah, tata bahasa Arab, atau sejarah Islam berdasarkan kitab-kitab klasik turats.
            Jawab dalam bahasa Indonesia yang ringkas, jelas, ilmiah, dan santun. Sebutkan rujukan kitab jika relevan. Gunakan format Markdown yang rapi.
        """.trimIndent()

        val request = GeminiRequest(
            contents = listOf(
                GeminiContent(parts = listOf(GeminiPart(text = userQuery)))
            ),
            systemInstruction = GeminiContent(parts = listOf(GeminiPart(text = systemPrompt)))
        )

        try {
            val responseBody = service.generateContentStream(apiKey = apiKey, request = request)
            responseBody.byteStream().bufferedReader().use { reader ->
                var line: String?
                val responseAdapter = moshi.adapter(GeminiResponse::class.java)
                while (reader.readLine().also { line = it } != null) {
                    val trimmed = line?.trim() ?: continue
                    if (trimmed.startsWith("data: ")) {
                        val jsonStr = trimmed.substring(6).trim()
                        try {
                            val chunk = responseAdapter.fromJson(jsonStr)
                            val text = chunk?.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                            if (text != null) {
                                emit(text)
                            }
                        } catch (e: Exception) {
                            // ignore malformed chunks
                        }
                    }
                }
            }
        } catch (e: Exception) {
            emit("\n[Gagal menghubungi AI Assistant: ${e.localizedMessage}]")
        }
    }
}
