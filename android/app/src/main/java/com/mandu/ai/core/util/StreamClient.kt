package com.mandu.ai.core.util

import com.mandu.ai.core.data.dto.ChatRequest
import com.mandu.ai.core.data.local.SessionStore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources
import javax.inject.Inject
import javax.inject.Singleton

sealed interface ChatEvent {
    data class Delta(val text: String) : ChatEvent
    data class Meta(val conversationId: String) : ChatEvent
    object Done : ChatEvent
    data class Error(val message: String) : ChatEvent
}

@Singleton
class StreamClient @Inject constructor(
    private val client: OkHttpClient,
    private val json: Json,
    private val session: SessionStore,
) {
    fun stream(baseUrl: String, token: String, request: ChatRequest): Flow<ChatEvent> = callbackFlow {
        val body = json.encodeToString(ChatRequest.serializer(), request)
        val http = Request.Builder()
            .url(baseUrl.trimEnd('/') + "/api/chat")
            .addHeader("Authorization", "Bearer $token")
            .addHeader("Accept", "text/event-stream")
            .post(body.toRequestBody("application/json".toMediaType()))
            .build()

        val factory = EventSources.createFactory(client)
        val listener = object : EventSourceListener() {
            override fun onEvent(eventSource: EventSource, id: String?, type: String?, data: String) {
                try {
                    when (type) {
                        "meta" -> {
                            val cid = json.parseToJsonElement(data).jsonObject["conversationId"]?.jsonPrimitive?.content
                            if (cid != null) trySend(ChatEvent.Meta(cid))
                        }
                        "done" -> { trySend(ChatEvent.Done); close() }
                        "error" -> {
                            val msg = json.parseToJsonElement(data).jsonObject["error"]?.jsonPrimitive?.content ?: "Stream error"
                            trySend(ChatEvent.Error(msg)); close()
                        }
                        else -> {
                            val delta = json.parseToJsonElement(data).jsonObject["delta"]?.jsonPrimitive?.content
                            if (delta != null) trySend(ChatEvent.Delta(delta))
                        }
                    }
                } catch (_: Exception) { /* ignore malformed */ }
            }

            override fun onFailure(eventSource: EventSource, t: Throwable?, response: Response?) {
                trySend(ChatEvent.Error(t?.message ?: "Connection failed ${response?.code ?: ""}"))
                close()
            }

            override fun onClosed(eventSource: EventSource) { close() }
        }

        val es = factory.newEventSource(http, listener)

        awaitClose { es.cancel() }
    }
}
