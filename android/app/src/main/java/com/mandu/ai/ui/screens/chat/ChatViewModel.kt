package com.mandu.ai.ui.screens.chat

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mandu.ai.core.data.dto.ChatRequest
import com.mandu.ai.core.data.dto.ConversationDto
import com.mandu.ai.core.data.repo.ChatRepository
import com.mandu.ai.core.util.ChatEvent
import com.mandu.ai.core.util.StreamClient
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class UiMessage(val role: String, val content: String)

data class ChatUiState(
    val loading: Boolean = false,
    val streaming: Boolean = false,
    val conversations: List<ConversationDto> = emptyList(),
    val activeId: String? = null,
    val messages: List<UiMessage> = emptyList(),
    val error: String? = null,
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val repo: ChatRepository,
    private val stream: StreamClient,
    private val savedState: SavedStateHandle,
) : ViewModel() {

    private val _state = MutableStateFlow(ChatUiState())
    val state: StateFlow<ChatUiState> = _state.asStateFlow()
    private var streamJob: Job? = null

    init {
        val preset = savedState.get<String>("cid")?.takeIf { it.isNotBlank() && it != "null" }
        _state.value = _state.value.copy(activeId = preset)
        refreshConversations()
        if (preset != null) loadMessages(preset)
    }

    private fun refreshConversations() {
        viewModelScope.launch {
            try {
                val list = repo.conversations()
                _state.value = _state.value.copy(conversations = list)
            } catch (e: Exception) {
                _state.value = _state.value.copy(error = e.message)
            }
        }
    }

    fun newChat() {
        viewModelScope.launch {
            try {
                val c = repo.createConversation()
                _state.value = _state.value.copy(activeId = c.id, messages = emptyList(), error = null)
                refreshConversations()
            } catch (e: Exception) {
                _state.value = _state.value.copy(error = e.message)
            }
        }
    }

    fun select(id: String) {
        _state.value = _state.value.copy(activeId = id, messages = emptyList())
        loadMessages(id)
    }

    private fun loadMessages(cid: String) {
        viewModelScope.launch {
            try {
                val list = repo.messages(cid).map { UiMessage(it.role, it.content) }
                _state.value = _state.value.copy(messages = list)
            } catch (e: Exception) {
                _state.value = _state.value.copy(error = e.message)
            }
        }
    }

    fun delete(id: String) {
        viewModelScope.launch {
            try {
                repo.delete(id)
                if (_state.value.activeId == id) {
                    _state.value = _state.value.copy(activeId = null, messages = emptyList())
                }
                refreshConversations()
            } catch (e: Exception) { _state.value = _state.value.copy(error = e.message) }
        }
    }

    fun rename(id: String, newTitle: String) {
        viewModelScope.launch {
            try { repo.rename(id, newTitle); refreshConversations() }
            catch (e: Exception) { _state.value = _state.value.copy(error = e.message) }
        }
    }

    fun send(text: String) {
        if (text.isBlank() || _state.value.streaming) return
        viewModelScope.launch {
            var cid = _state.value.activeId
            if (cid == null) {
                try {
                    cid = repo.createConversation().id
                    _state.value = _state.value.copy(activeId = cid)
                } catch (e: Exception) {
                    _state.value = _state.value.copy(error = e.message); return@launch
                }
            }
            val system = repo.systemPrompt()
            val currentCid = cid!!

            _state.value = _state.value.copy(
                messages = _state.value.messages + UiMessage("user", text) + UiMessage("assistant", ""),
                streaming = true, error = null,
            )

            val bearer = repo.bearer().removePrefix("Bearer ")
            val request = ChatRequest(conversationId = currentCid, system = system, message = text)

            streamJob = viewModelScope.launch {
                try {
                    stream.stream(repo.backendBaseUrl(), bearer, request).collect { ev ->
                        when (ev) {
                            is ChatEvent.Delta -> {
                                val msgs = _state.value.messages.toMutableList()
                                val last = msgs.removeLast()
                                msgs.add(last.copy(content = last.content + ev.text))
                                _state.value = _state.value.copy(messages = msgs)
                            }
                            is ChatEvent.Done -> {
                                _state.value = _state.value.copy(streaming = false)
                                refreshConversations()
                            }
                            is ChatEvent.Error -> {
                                _state.value = _state.value.copy(streaming = false, error = ev.message)
                            }
                            is ChatEvent.Meta -> {
                                if (_state.value.activeId != ev.conversationId) {
                                    _state.value = _state.value.copy(activeId = ev.conversationId)
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    _state.value = _state.value.copy(streaming = false, error = e.message)
                }
            }
        }
    }

    fun stopGeneration() {
        streamJob?.cancel()
        _state.value = _state.value.copy(streaming = false)
    }

    fun regenerate() {
        val msgs = _state.value.messages
        val lastUser = msgs.lastOrNull { it.role == "user" } ?: return
        // Remove last assistant
        val trimmed = msgs.dropLastWhile { it.role == "assistant" }
        _state.value = _state.value.copy(messages = trimmed)
        send(lastUser.content)
    }
}
