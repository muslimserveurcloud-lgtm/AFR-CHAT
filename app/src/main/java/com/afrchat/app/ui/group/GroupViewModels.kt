package com.afrchat.app.ui.group

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.afrchat.app.data.model.AfrResult
import com.afrchat.app.data.model.Group
import com.afrchat.app.data.model.User
import com.afrchat.app.data.repository.AuthRepository
import com.afrchat.app.data.repository.GroupRepository
import com.afrchat.app.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CreateGroupUiState(
    val name: String = "",
    val searchQuery: String = "",
    val searchResults: List<User> = emptyList(),
    val selectedMembers: List<User> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val createdConversationId: String? = null
)

@HiltViewModel
class CreateGroupViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val groupRepository: GroupRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    var uiState by mutableStateOf(CreateGroupUiState())
        private set

    fun onNameChange(v: String) { uiState = uiState.copy(name = v) }

    fun onSearchChange(v: String) {
        uiState = uiState.copy(searchQuery = v)
        viewModelScope.launch {
            val uid = authRepository.currentUserId ?: return@launch
            when (val result = userRepository.searchUsers(v, uid)) {
                is AfrResult.Success -> uiState = uiState.copy(searchResults = result.data)
                else -> {}
            }
        }
    }

    fun toggleMember(user: User) {
        val current = uiState.selectedMembers
        uiState = uiState.copy(
            selectedMembers = if (current.any { it.uid == user.uid }) current.filterNot { it.uid == user.uid } else current + user
        )
    }

    fun createGroup() {
        val uid = authRepository.currentUserId ?: return
        if (uiState.name.isBlank()) { uiState = uiState.copy(errorMessage = "Donnez un nom au groupe."); return }
        if (uiState.selectedMembers.isEmpty()) { uiState = uiState.copy(errorMessage = "Ajoutez au moins un membre."); return }

        uiState = uiState.copy(isLoading = true, errorMessage = null)
        viewModelScope.launch {
            when (val result = groupRepository.createGroup(uiState.name, uid, uiState.selectedMembers.map { it.uid })) {
                is AfrResult.Success -> uiState = uiState.copy(isLoading = false, createdConversationId = result.data)
                is AfrResult.Error -> uiState = uiState.copy(isLoading = false, errorMessage = result.message)
                AfrResult.Loading -> {}
            }
        }
    }
}

@HiltViewModel
class GroupInfoViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val groupRepository: GroupRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val groupId: String = savedStateHandle.get<String>("groupId").orEmpty()
    val conversationId: String = savedStateHandle.get<String>("conversationId").orEmpty()
    val currentUid: String get() = authRepository.currentUserId.orEmpty()

    private val _group = MutableStateFlow<Group?>(null)
    val group: StateFlow<Group?> = _group.asStateFlow()

    init {
        viewModelScope.launch { groupRepository.observeGroup(groupId).collect { _group.value = it } }
    }

    fun leaveGroup(onDone: () -> Unit) = viewModelScope.launch {
        groupRepository.leaveGroup(groupId, conversationId, currentUid)
        onDone()
    }

    fun removeMember(uid: String) = viewModelScope.launch {
        groupRepository.removeMember(groupId, conversationId, uid, currentUid)
    }

    fun promoteAdmin(uid: String, isAdmin: Boolean) = viewModelScope.launch {
        groupRepository.setAdmin(groupId, uid, currentUid, isAdmin)
    }

    fun toggleOnlyAdminsCanPost(value: Boolean) = viewModelScope.launch {
        groupRepository.setOnlyAdminsCanPost(groupId, value)
    }
}
