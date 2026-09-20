package com.afrchat.app.ui.chat

import android.content.Context
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.afrchat.app.data.model.AfrResult
import com.afrchat.app.data.model.Message
import com.afrchat.app.data.repository.AuthRepository
import com.afrchat.app.data.repository.ChatRepository
import com.afrchat.app.data.repository.MediaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChatUiState(
    val conversationId: String = "",
    val messages: List<Message> = emptyList(),
    val messageInput: String = "",
    val replyTo: Message? = null,
    val isPeerTyping: Boolean = false,
    val peerUid: String = "",
    val isGroup: Boolean = false,
    val groupId: String? = null,
    val uploadProgress: Int? = null,
    val errorMessage: String? = null
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val chatRepository: ChatRepository,
    private val mediaRepository: MediaRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val currentUid: String get() = authRepository.currentUserId.orEmpty()
    private val conversationId: String = savedStateHandle.get<String>("conversationId").orEmpty()

    private val _uiState = MutableStateFlow(ChatUiState(conversationId = conversationId))
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var typingJob: kotlinx.coroutines.Job? = null

    init {
        observeMessages()
        observeConversationMeta()
        markRead()
    }

    private fun observeMessages() {
        viewModelScope.launch {
            chatRepository.observeNewMessages(conversationId).collect { msgs ->
                _uiState.value = _uiState.value.copy(messages = msgs.filter { currentUid !in it.deletedFor })
            }
        }
    }

    private fun observeConversationMeta() {
        viewModelScope.launch {
            chatRepository.observeConversation(conversationId).collect { convo ->
                val typing = convo?.typingUserIds?.any { it != currentUid } ?: false
                val peerUid = convo?.participantIds?.firstOrNull { it != currentUid } ?: ""
                _uiState.value = _uiState.value.copy(
                    isPeerTyping = typing,
                    peerUid = peerUid,
                    isGroup = convo?.type == "group",
                    groupId = convo?.groupId
                )
            }
        }
    }

    private fun markRead() {
        viewModelScope.launch { chatRepository.markMessagesAsRead(conversationId, currentUid) }
    }

    fun onInputChange(text: String) {
        _uiState.value = _uiState.value.copy(messageInput = text)
        typingJob?.cancel()
        viewModelScope.launch { chatRepository.setTyping(conversationId, currentUid, text.isNotBlank()) }
        typingJob = viewModelScope.launch {
            kotlinx.coroutines.delay(3000)
            chatRepository.setTyping(conversationId, currentUid, false)
        }
    }

    fun setReplyTo(message: Message?) { _uiState.value = _uiState.value.copy(replyTo = message) }

    fun sendText() {
        val text = _uiState.value.messageInput.trim()
        if (text.isEmpty()) return
        val replyId = _uiState.value.replyTo?.id
        _uiState.value = _uiState.value.copy(messageInput = "", replyTo = null)
        viewModelScope.launch {
            chatRepository.setTyping(conversationId, currentUid, false)
            when (val result = chatRepository.sendTextMessage(conversationId, currentUid, text, replyId)) {
                is AfrResult.Error -> _uiState.value = _uiState.value.copy(errorMessage = result.message)
                else -> {}
            }
        }
    }

    fun sendMedia(context: Context, uri: Uri, type: String, contentType: String, fileName: String = "") {
        val folder = when (type) {
            "image" -> "chat_images"
            "video" -> "chat_videos"
            "audio" -> "voice_messages"
            else -> "chat_files"
        }
        viewModelScope.launch {
            mediaRepository.uploadFile(context, uri, folder, currentUid, contentType).collect { state ->
                when (state) {
                    is MediaRepository.UploadState.Progress -> _uiState.value = _uiState.value.copy(uploadProgress = state.percent)
                    is MediaRepository.UploadState.Success -> {
                        _uiState.value = _uiState.value.copy(uploadProgress = null)
                        chatRepository.sendMediaMessage(conversationId, currentUid, type, state.url, fileName)
                    }
                    is MediaRepository.UploadState.Error -> _uiState.value = _uiState.value.copy(uploadProgress = null, errorMessage = state.message)
                }
            }
        }
    }

    fun deleteForMe(messageId: String) = viewModelScope.launch { chatRepository.deleteMessageForMe(conversationId, messageId, currentUid) }
    fun deleteForEveryone(messageId: String) = viewModelScope.launch { chatRepository.deleteMessageForEveryone(conversationId, messageId) }
    fun react(messageId: String, emoji: String) = viewModelScope.launch { chatRepository.addReaction(conversationId, messageId, currentUid, emoji) }

    fun loadOlder(onDone: (Boolean) -> Unit) {
        // Pagination gérée via ChatRepository.loadOlderMessages ; branché à un DocumentSnapshot
        // curseur conservé côté UI (LazyColumn) lors du scroll vers le haut.
        onDone(true)
    }
}
