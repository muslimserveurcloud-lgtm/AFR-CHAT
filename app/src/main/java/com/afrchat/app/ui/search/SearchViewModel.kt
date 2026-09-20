package com.afrchat.app.ui.search

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.afrchat.app.data.model.AfrResult
import com.afrchat.app.data.model.User
import com.afrchat.app.data.repository.AuthRepository
import com.afrchat.app.data.repository.ChatRepository
import com.afrchat.app.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SearchUiState(
    val query: String = "",
    val results: List<User> = emptyList(),
    val isLoading: Boolean = false,
    val openConversationId: String? = null
)

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val chatRepository: ChatRepository
) : ViewModel() {

    var uiState by mutableStateOf(SearchUiState())
        private set

    fun onQueryChange(query: String) {
        uiState = uiState.copy(query = query, isLoading = true)
        viewModelScope.launch {
            val uid = authRepository.currentUserId ?: return@launch
            when (val result = userRepository.searchUsers(query, uid)) {
                is AfrResult.Success -> uiState = uiState.copy(results = result.data, isLoading = false)
                else -> uiState = uiState.copy(isLoading = false)
            }
        }
    }

    fun startConversation(peerUid: String) {
        val uid = authRepository.currentUserId ?: return
        viewModelScope.launch {
            when (val result = chatRepository.getOrCreatePrivateConversation(uid, peerUid)) {
                is AfrResult.Success -> uiState = uiState.copy(openConversationId = result.data)
                else -> {}
            }
        }
    }
}
