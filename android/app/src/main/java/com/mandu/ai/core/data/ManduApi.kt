package com.mandu.ai.core.data

import com.mandu.ai.core.data.dto.*
import retrofit2.http.*

interface ManduApi {

    @POST("api/auth/register")
    suspend fun register(@Body body: AuthRequest): AuthResponse

    @POST("api/auth/login")
    suspend fun login(@Body body: AuthRequest): AuthResponse

    @GET("api/auth/me")
    suspend fun me(@Header("Authorization") bearer: String): UserDto

    @GET("api/conversations")
    suspend fun conversations(@Header("Authorization") bearer: String): List<ConversationDto>

    @POST("api/conversations")
    suspend fun createConversation(
        @Header("Authorization") bearer: String,
        @Body body: Map<String, String>
    ): ConversationDto

    @PATCH("api/conversations/{id}")
    suspend fun renameConversation(
        @Header("Authorization") bearer: String,
        @Path("id") id: String,
        @Body body: Map<String, String>
    ): ConversationDto

    @DELETE("api/conversations/{id}")
    suspend fun deleteConversation(
        @Header("Authorization") bearer: String,
        @Path("id") id: String
    ): Map<String, Boolean>

    @GET("api/conversations/{id}/messages")
    suspend fun messages(
        @Header("Authorization") bearer: String,
        @Path("id") id: String
    ): List<MessageDto>

    @POST("api/images/generate")
    suspend fun generateImage(
        @Header("Authorization") bearer: String,
        @Body body: ImageRequest
    ): ImageResponse

    @POST("api/search")
    suspend fun search(
        @Header("Authorization") bearer: String,
        @Body body: SearchRequest
    ): SearchResponse

    @GET("api/connectors/catalog")
    suspend fun connectorCatalog(@Header("Authorization") bearer: String): List<ConnectorCatalogItem>

    @GET("api/connectors")
    suspend fun connectors(@Header("Authorization") bearer: String): List<ConnectorDto>

    @POST("api/connectors/{provider}/authorize")
    suspend fun authorizeConnector(
        @Header("Authorization") bearer: String,
        @Path("provider") provider: String
    ): AuthorizeResponse

    @DELETE("api/connectors/{provider}")
    suspend fun disconnectConnector(
        @Header("Authorization") bearer: String,
        @Path("provider") provider: String
    ): Map<String, Boolean>

    @POST("api/agents/run")
    suspend fun runAgent(
        @Header("Authorization") bearer: String,
        @Body body: AgentRequest
    ): AgentResponse
}
