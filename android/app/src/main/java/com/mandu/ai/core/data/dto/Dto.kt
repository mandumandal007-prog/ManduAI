package com.mandu.ai.core.data.dto

import kotlinx.serialization.Serializable

@Serializable data class UserDto(val id: String, val email: String, val name: String? = null)

@Serializable data class AuthRequest(val email: String, val password: String, val name: String? = null)

@Serializable data class AuthResponse(val token: String, val user: UserDto)

@Serializable data class ConversationDto(val id: String, val title: String, val model: String? = null, val updatedAt: String? = null)

@Serializable data class MessageDto(
    val id: String? = null,
    val role: String,
    val content: String,
    val createdAt: String? = null,
)

@Serializable data class ChatRequest(
    val conversationId: String? = null,
    val provider: String? = null,
    val model: String? = null,
    val system: String? = null,
    val message: String,
)

@Serializable data class ImageRequest(val prompt: String, val size: String = "1024x1024", val n: Int = 1)

@Serializable data class ImageItem(val url: String? = null, val b64: String? = null)

@Serializable data class ImageResponse(val images: List<ImageItem> = emptyList())

@Serializable data class SearchRequest(val query: String, val count: Int = 6)

@Serializable data class SearchResult(val title: String, val url: String, val snippet: String? = null)

@Serializable data class SearchResponse(val answer: String? = null, val results: List<SearchResult> = emptyList())

@Serializable data class ConnectorDto(val provider: String, val status: String, val metadata: Map<String, String>? = null)

@Serializable data class ConnectorCatalogItem(val id: String, val label: String)

@Serializable data class AuthorizeResponse(val url: String)

@Serializable data class AgentRequest(val instruction: String)

@Serializable data class AgentResponse(val plan: String, val connectors: List<String> = emptyList())
