package com.mandu.ai.core.data.repo

import com.mandu.ai.core.data.ManduApi
import com.mandu.ai.core.data.dto.*
import com.mandu.ai.core.data.local.ChatDao
import com.mandu.ai.core.data.local.ChatMessageEntity
import com.mandu.ai.core.data.local.SessionStore
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val api: ManduApi,
    private val session: SessionStore,
) {
    val tokenFlow = session.token
    val emailFlow = session.email
    val nameFlow = session.name

    suspend fun login(email: String, password: String): UserDto {
        val res = api.login(AuthRequest(email, password))
        session.saveSession(res.token, res.user.email, res.user.name)
        return res.user
    }

    suspend fun register(name: String?, email: String, password: String): UserDto {
        val res = api.register(AuthRequest(email, password, name))
        session.saveSession(res.token, res.user.email, res.user.name)
        return res.user
    }

    suspend fun logout() = session.clear()

    suspend fun bearer(): String = "Bearer " + (session.token.first() ?: "")
}

@Singleton
class ChatRepository @Inject constructor(
    private val api: ManduApi,
    private val auth: AuthRepository,
    private val dao: ChatDao,
    private val session: SessionStore,
) {
    suspend fun conversations() = api.conversations(auth.bearer())

    suspend fun createConversation(): ConversationDto =
        api.createConversation(auth.bearer(), mapOf("title" to "New chat"))

    suspend fun rename(id: String, title: String) =
        api.renameConversation(auth.bearer(), id, mapOf("title" to title))

    suspend fun delete(id: String) {
        api.deleteConversation(auth.bearer(), id)
        dao.clear(id)
    }

    suspend fun messages(id: String): List<MessageDto> = api.messages(auth.bearer(), id)

    fun observeLocal(cid: String) = dao.observe(cid)

    suspend fun saveLocal(cid: String, role: String, content: String) {
        dao.insert(ChatMessageEntity(conversationId = cid, role = role, content = content))
    }

    suspend fun systemPrompt(): String = session.systemPrompt.first()
    suspend fun bearer() = auth.bearer()
    fun backendBaseUrl(): String = com.mandu.ai.BuildConfig.BACKEND_URL
}

@Singleton
class ImageRepository @Inject constructor(
    private val api: ManduApi,
    private val auth: AuthRepository,
) {
    suspend fun generate(prompt: String) = api.generateImage(auth.bearer(), ImageRequest(prompt))
}

@Singleton
class SearchRepository @Inject constructor(
    private val api: ManduApi,
    private val auth: AuthRepository,
) {
    suspend fun search(query: String) = api.search(auth.bearer(), SearchRequest(query))
}

@Singleton
class ConnectorRepository @Inject constructor(
    private val api: ManduApi,
    private val auth: AuthRepository,
) {
    suspend fun catalog() = api.connectorCatalog(auth.bearer())
    suspend fun connections() = api.connectors(auth.bearer())
    suspend fun startOAuth(provider: String) = api.authorizeConnector(auth.bearer(), provider)
    suspend fun disconnect(provider: String) = api.disconnectConnector(auth.bearer(), provider)
}

@Singleton
class AgentRepository @Inject constructor(
    private val api: ManduApi,
    private val auth: AuthRepository,
) {
    suspend fun run(instruction: String) = api.runAgent(auth.bearer(), AgentRequest(instruction))
}
