package com.example.data

import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@JsonClass(generateAdapter = true)
data class OpenAiMessage(
    val role: String,
    val content: String
)

@JsonClass(generateAdapter = true)
data class OpenAiChatRequest(
    val model: String,
    val messages: List<OpenAiMessage>,
    val stream: Boolean = false
)

@JsonClass(generateAdapter = true)
data class OpenAiChatResponse(
    val choices: List<OpenAiChoice>? = null
)

@JsonClass(generateAdapter = true)
data class OpenAiChoice(
    val index: Int? = null,
    val message: OpenAiMessage? = null,
    val delta: OpenAiDelta? = null,
    val finish_reason: String? = null
)

@JsonClass(generateAdapter = true)
data class OpenAiDelta(
    val content: String? = null
)

@JsonClass(generateAdapter = true)
data class OpenAiModelItem(
    val id: String
)

@JsonClass(generateAdapter = true)
data class OpenAiModelsResponse(
    val data: List<OpenAiModelItem>? = null
)

object OpenAiApiClient {
    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun fetchModels(baseUrl: String, apiKey: String): List<String> = suspendCancellableCoroutine { continuation ->
        val cleanBaseUrl = baseUrl.trim().removeSuffix("/")
        val modelsUrl = if (cleanBaseUrl.contains("/models")) {
            cleanBaseUrl
        } else if (cleanBaseUrl.endsWith("/chat/completions")) {
            cleanBaseUrl.substringBeforeLast("/chat/completions") + "/models"
        } else {
            "$cleanBaseUrl/models"
        }

        val request = Request.Builder()
            .url(modelsUrl)
            .addHeader("Authorization", "Bearer $apiKey")
            .get()
            .build()

        val call = client.newCall(request)
        continuation.invokeOnCancellation { call.cancel() }

        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                if (continuation.isActive) {
                    continuation.resumeWithException(e)
                }
            }

            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) {
                    if (continuation.isActive) {
                        continuation.resumeWithException(IOException("HTTP Error: ${response.code} ${response.message}"))
                    }
                    response.close()
                    return
                }

                try {
                    val bodyString = response.body?.string()
                    if (bodyString != null) {
                        val adapter = moshi.adapter(OpenAiModelsResponse::class.java)
                        val modelResponse = adapter.fromJson(bodyString)
                        val modelList = modelResponse?.data?.map { it.id } ?: emptyList()
                        if (continuation.isActive) {
                            continuation.resume(modelList)
                        }
                    } else {
                        if (continuation.isActive) {
                            continuation.resume(emptyList())
                        }
                    }
                } catch (e: Exception) {
                    if (continuation.isActive) {
                        continuation.resumeWithException(e)
                    }
                } finally {
                    response.close()
                }
            }
        })
    }

    fun getExplanationStream(
        baseUrl: String,
        apiKey: String,
        model: String,
        textToExplain: String,
        contextBook: String,
        analysisType: String = "makna"
    ): Flow<String> = flow {
        val cleanBaseUrl = baseUrl.trim().removeSuffix("/")
        val completionsUrl = if (cleanBaseUrl.contains("/chat/completions")) {
            cleanBaseUrl
        } else {
            "$cleanBaseUrl/chat/completions"
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

        val chatRequest = OpenAiChatRequest(
            model = model,
            messages = listOf(
                OpenAiMessage(role = "system", content = systemPrompt),
                OpenAiMessage(role = "user", content = userPrompt)
            ),
            stream = true
        )

        val requestAdapter = moshi.adapter(OpenAiChatRequest::class.java)
        val requestJson = requestAdapter.toJson(chatRequest)

        val request = Request.Builder()
            .url(completionsUrl)
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(requestJson.toRequestBody(jsonMediaType))
            .build()

        try {
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                emit("Gagal menghubungi AI OpenAI-Compatible: HTTP ${response.code} ${response.message}")
                response.close()
                return@flow
            }

            val responseBody = response.body
            if (responseBody == null) {
                emit("Gagal menghubungi AI: Response body kosong.")
                response.close()
                return@flow
            }

            responseBody.byteStream().bufferedReader().use { reader ->
                var line: String?
                val chunkAdapter = moshi.adapter(OpenAiChatResponse::class.java)
                while (reader.readLine().also { line = it } != null) {
                    val trimmed = line?.trim() ?: continue
                    if (trimmed.startsWith("data: ")) {
                        val jsonStr = trimmed.substring(6).trim()
                        if (jsonStr == "[DONE]") {
                            break
                        }
                        try {
                            val chunk = chunkAdapter.fromJson(jsonStr)
                            val deltaContent = chunk?.choices?.firstOrNull()?.delta?.content
                            if (deltaContent != null) {
                                emit(deltaContent)
                            }
                        } catch (e: Exception) {
                            // ignore malformed chunks
                        }
                    }
                }
            }
        } catch (e: Exception) {
            emit("\n[Gagal menghubungi AI Assistant (OpenAI Compatible): ${e.localizedMessage}]")
        }
    }

    fun askAssistantStream(
        baseUrl: String,
        apiKey: String,
        model: String,
        userQuery: String
    ): Flow<String> = flow {
        val cleanBaseUrl = baseUrl.trim().removeSuffix("/")
        val completionsUrl = if (cleanBaseUrl.contains("/chat/completions")) {
            cleanBaseUrl
        } else {
            "$cleanBaseUrl/chat/completions"
        }

        val systemPrompt = """
            Anda adalah asisten ahli studi turats (klasik Islam) untuk aplikasi Shamela.
            Bantu pengguna menjawab pertanyaan terkait keagamaan, fikih, hadits, tafsir, aqidah, tata bahasa Arab, atau sejarah Islam berdasarkan kitab-kitab klasik turats.
            Jawab dalam bahasa Indonesia yang ringkas, jelas, ilmiah, dan santun. Sebutkan rujukan kitab jika relevan. Gunakan format Markdown yang rapi.
        """.trimIndent()

        val chatRequest = OpenAiChatRequest(
            model = model,
            messages = listOf(
                OpenAiMessage(role = "system", content = systemPrompt),
                OpenAiMessage(role = "user", content = userQuery)
            ),
            stream = true
        )

        val requestAdapter = moshi.adapter(OpenAiChatRequest::class.java)
        val requestJson = requestAdapter.toJson(chatRequest)

        val request = Request.Builder()
            .url(completionsUrl)
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(requestJson.toRequestBody(jsonMediaType))
            .build()

        try {
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                emit("Gagal menghubungi AI OpenAI-Compatible: HTTP ${response.code} ${response.message}")
                response.close()
                return@flow
            }

            val responseBody = response.body
            if (responseBody == null) {
                emit("Gagal menghubungi AI: Response body kosong.")
                response.close()
                return@flow
            }

            responseBody.byteStream().bufferedReader().use { reader ->
                var line: String?
                val chunkAdapter = moshi.adapter(OpenAiChatResponse::class.java)
                while (reader.readLine().also { line = it } != null) {
                    val trimmed = line?.trim() ?: continue
                    if (trimmed.startsWith("data: ")) {
                        val jsonStr = trimmed.substring(6).trim()
                        if (jsonStr == "[DONE]") {
                            break
                        }
                        try {
                            val chunk = chunkAdapter.fromJson(jsonStr)
                            val deltaContent = chunk?.choices?.firstOrNull()?.delta?.content
                            if (deltaContent != null) {
                                emit(deltaContent)
                            }
                        } catch (e: Exception) {
                            // ignore malformed chunks
                        }
                    }
                }
            }
        } catch (e: Exception) {
            emit("\n[Gagal menghubungi AI Assistant (OpenAI Compatible): ${e.localizedMessage}]")
        }
    }

    suspend fun translateQueryToArabic(
        baseUrl: String,
        apiKey: String,
        model: String,
        query: String
    ): String = withContext(Dispatchers.IO) {
        val cleanBaseUrl = baseUrl.trim().removeSuffix("/")
        val completionsUrl = if (cleanBaseUrl.contains("/chat/completions")) {
            cleanBaseUrl
        } else {
            "$cleanBaseUrl/chat/completions"
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

        val chatRequest = OpenAiChatRequest(
            model = model,
            messages = listOf(
                OpenAiMessage(role = "system", content = systemPrompt),
                OpenAiMessage(role = "user", content = "Input: $query\nOutput:")
            ),
            stream = false
        )

        val requestAdapter = moshi.adapter(OpenAiChatRequest::class.java)
        val requestJson = requestAdapter.toJson(chatRequest)

        val request = Request.Builder()
            .url(completionsUrl)
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(requestJson.toRequestBody(jsonMediaType))
            .build()

        try {
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                response.close()
                return@withContext query
            }

            val bodyString = response.body?.string()
            if (bodyString != null) {
                val responseAdapter = moshi.adapter(OpenAiChatResponse::class.java)
                val chatResponse = responseAdapter.fromJson(bodyString)
                val arabicResult = chatResponse?.choices?.firstOrNull()?.message?.content?.trim()
                if (!arabicResult.isNullOrEmpty()) {
                    return@withContext arabicResult
                }
            }
            response.close()
            query
        } catch (e: Exception) {
            query
        }
    }
}
