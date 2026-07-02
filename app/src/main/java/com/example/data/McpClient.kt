package com.example.data

import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

@JsonClass(generateAdapter = true)
data class McpJsonRpcRequest(
    val jsonrpc: String = "2.0",
    val id: String? = null,
    val method: String,
    val params: Map<String, Any?>? = null
)

@JsonClass(generateAdapter = true)
data class McpJsonRpcResponse(
    val jsonrpc: String = "2.0",
    val id: String? = null,
    val result: Any? = null,
    val error: McpError? = null
)

@JsonClass(generateAdapter = true)
data class McpError(
    val code: Int,
    val message: String,
    val data: Any? = null
)

object McpClient {
    private const val DEFAULT_BASE_URL = "https://winongkencono-shamelah.hf.space"
    private const val DEFAULT_SSE_PATH = "/mcp/sse"

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()
    
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val idCounter = AtomicInteger(1)
    
    private var sseCall: Call? = null
    private var postUrl: String? = null
    private val pendingRequests = java.util.concurrent.ConcurrentHashMap<String, CompletableDeferred<String>>()
    private val mutex = Mutex()
    private var isConnected = false
    private var lastError: String? = null

    suspend fun getStatus(): String {
        mutex.withLock {
            return when {
                isConnected -> "Terhubung ke MCP Server"
                lastError != null -> "Error: $lastError"
                else -> "Belum terhubung"
            }
        }
    }

    fun startConnection(baseUrl: String = DEFAULT_BASE_URL) {
        scope.launch {
            mutex.withLock {
                if (isConnected) {
                    return@launch
                }
                lastError = null
            }
            
            try {
                connectSse(baseUrl)
            } catch (e: Exception) {
                mutex.withLock {
                    isConnected = false
                    lastError = "Koneksi gagal: ${e.localizedMessage}"
                }
            }
        }
    }

    private suspend fun connectSse(baseUrl: String) {
        val sseUrl = if (baseUrl.startsWith("http")) baseUrl else "https://$baseUrl"
        val fullUrl = if (sseUrl.endsWith(DEFAULT_SSE_PATH)) sseUrl else sseUrl.trimEnd('/') + DEFAULT_SSE_PATH

        val request = Request.Builder()
            .url(fullUrl)
            .header("Accept", "text/event-stream")
            .build()

        val call = okHttpClient.newCall(request)
        mutex.withLock {
            sseCall = call
        }

        withContext(Dispatchers.IO) {
            try {
                val response = call.execute()
                if (!response.isSuccessful) {
                    mutex.withLock {
                        isConnected = false
                        lastError = "HTTP ${response.code} ${response.message}"
                    }
                    response.close()
                    return@withContext
                }

                val body = response.body
                if (body == null) {
                    mutex.withLock {
                        isConnected = false
                        lastError = "Response body kosong"
                    }
                    response.close()
                    return@withContext
                }

                body.byteStream().bufferedReader().use { reader ->
                    var line: String?
                    var currentEvent: String? = null

                    while (reader.readLine().also { line = it } != null) {
                        val trimmed = line?.trim() ?: continue
                        if (trimmed.isEmpty()) {
                            currentEvent = null
                            continue
                        }

                        if (trimmed.startsWith("event:")) {
                            currentEvent = trimmed.substring(6).trim()
                        } else if (trimmed.startsWith("data:")) {
                            val data = trimmed.substring(5).trim()
                            handleSseEvent(currentEvent, data, sseUrl)
                        }
                    }
                }
            } catch (e: Exception) {
                mutex.withLock {
                    isConnected = false
                    lastError = "Koneksi terputus: ${e.localizedMessage}"
                }
            }
        }
    }

    private fun handleSseEvent(event: String?, data: String, baseSseUrl: String) {
        when (event) {
            "endpoint" -> {
                // The endpoint to post messages to
                val rawUrl = data
                val fullPostUrl = if (rawUrl.startsWith("http")) {
                    rawUrl
                } else {
                    val uri = baseSseUrl.toHttpUrlOrNull()
                    if (uri != null) {
                        val portStr = if (uri.port != 80 && uri.port != 443) ":${uri.port}" else ""
                        "${uri.scheme}://${uri.host}$portStr${if (rawUrl.startsWith("/")) "" else "/"}$rawUrl"
                    } else {
                        rawUrl
                    }
                }
                
                postUrl = fullPostUrl
                scope.launch {
                    initializeMcp()
                }
            }
            "message" -> {
                // Parse message to resolve corresponding deferred
                try {
                    val adapter = moshi.adapter(McpJsonRpcResponse::class.java)
                    val response = adapter.fromJson(data)
                    val id = response?.id
                    if (id != null) {
                        val deferred = pendingRequests.remove(id)
                        if (deferred != null) {
                            deferred.complete(data)
                        }
                    }
                } catch (e: Exception) {
                    // Ignore parsing error
                }
            }
        }
    }

    private suspend fun initializeMcp() {
        val url = postUrl ?: return
        try {
            val initId = idCounter.getAndIncrement().toString()
            val deferred = CompletableDeferred<String>()
            pendingRequests[initId] = deferred

            val request = McpJsonRpcRequest(
                id = initId,
                method = "initialize",
                params = mapOf(
                    "protocolVersion" to "2024-11-05",
                    "capabilities" to emptyMap<String, Any>(),
                    "clientInfo" to mapOf(
                        "name" to "ShamelaAndroid",
                        "version" to "1.0.0"
                    )
                )
            )

            val requestBody = moshi.adapter(McpJsonRpcRequest::class.java).toJson(request)
            val httpPost = Request.Builder()
                .url(url)
                .post(requestBody.toRequestBody(jsonMediaType))
                .build()

            val response = okHttpClient.newCall(httpPost).execute()
            if (!response.isSuccessful) {
                response.close()
                pendingRequests.remove(initId)
                return
            }
            response.close()

            // Wait for response via SSE
            val responseData = withTimeoutOrNull(10000) {
                deferred.await()
            }

            if (responseData != null) {
                // Send initialized notification
                val notification = McpJsonRpcRequest(
                    method = "notifications/initialized"
                )
                val notifyBody = moshi.adapter(McpJsonRpcRequest::class.java).toJson(notification)
                val notifyPost = Request.Builder()
                    .url(url)
                    .post(notifyBody.toRequestBody(jsonMediaType))
                    .build()
                okHttpClient.newCall(notifyPost).execute().close()

                mutex.withLock {
                    isConnected = true
                    lastError = null
                }
            } else {
                pendingRequests.remove(initId)
            }
        } catch (e: Exception) {
            mutex.withLock {
                isConnected = false
                lastError = "Handshake gagal: ${e.localizedMessage}"
            }
        }
    }

    suspend fun executeTool(toolName: String, arguments: Map<String, Any?>?): String {
        // Ensure connection is active
        if (postUrl == null) {
            startConnection()
            // Wait up to 5 seconds for connection
            for (i in 1..25) {
                if (isConnected) break
                delay(200)
            }
        }

        val url = postUrl ?: return "Error: Server MCP tidak siap."
        val reqId = idCounter.getAndIncrement().toString()
        val deferred = CompletableDeferred<String>()
        pendingRequests[reqId] = deferred

        val request = McpJsonRpcRequest(
            id = reqId,
            method = "tools/call",
            params = mapOf(
                "name" to toolName,
                "arguments" to (arguments ?: emptyMap())
            )
        )

        return try {
            val requestBody = moshi.adapter(McpJsonRpcRequest::class.java).toJson(request)
            val httpPost = Request.Builder()
                .url(url)
                .post(requestBody.toRequestBody(jsonMediaType))
                .build()

            val response = okHttpClient.newCall(httpPost).execute()
            if (!response.isSuccessful) {
                response.close()
                pendingRequests.remove(reqId)
                return "Error: HTTP ${response.code} saat memanggil tool $toolName."
            }
            response.close()

            // Wait for SSE response with timeout of 15s
            val responseData = withTimeoutOrNull(15000) {
                deferred.await()
            }

            if (responseData != null) {
                responseData
            } else {
                pendingRequests.remove(reqId)
                "Error: Timeout menunggu respon dari server MCP."
            }
        } catch (e: Exception) {
            pendingRequests.remove(reqId)
            "Error: ${e.localizedMessage}"
        }
    }

    fun parseToolResultText(json: String): String {
        try {
            val adapter = moshi.adapter(Map::class.java)
            val map = adapter.fromJson(json) as? Map<*, *>
            val result = map?.get("result") as? Map<*, *>
            val content = result?.get("content") as? List<*>
            if (content != null) {
                val textBuilder = StringBuilder()
                for (item in content) {
                    if (item is Map<*, *>) {
                        val text = item["text"] as? String
                        if (text != null) {
                            textBuilder.append(text).append("\n")
                        }
                    }
                }
                return textBuilder.toString().trim()
            }
        } catch (e: Exception) {
            // Fallback
        }
        return json // Return raw json if parsing failed
    }

    fun close() {
        sseCall?.cancel()
        sseCall = null
        isConnected = false
        postUrl = null
        pendingRequests.clear()
    }
}
