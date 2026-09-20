package com.afrchat.app.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.afrchat.app.data.model.Conversation
import com.afrchat.app.data.model.User
import com.afrchat.app.data.repository.AuthRepository
import com.afrchat.app.data.repository.ChatRepository
import com.afrchat.app.data.repository.GroupRepository
import com.afrchat.app.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ConversationListItem(
    val conversation: Conversation,
    val peer: User? = null,       // pour les discussions privées
    val displayName: String = "",
    val displayPhoto: String = ""
)

@HiltViewModel
class ConversationListViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val chatRepository: ChatRepository,
    private val userRepository: UserRepository,
    private val groupRepository: GroupRepository
) : ViewModel() {

    private val _items = MutableStateFlow<List<ConversationListItem>>(emptyList())
    val items: StateFlow<List<ConversationListItem>> = _items.asStateFlow()

    val currentUser: StateFlow<User?> = authRepository.currentUserId?.let { uid ->
        userRepository.observeUser(uid)
    }?.let { flow ->
        val state = MutableStateFlow<User?>(null)
        viewModelScope.launch { flow.collect { state.value = it } }
        state.asStateFlow()
    } ?: MutableStateFlow(null)

    private val userCache = mutableMapOf<String, User>()
    private val groupCache = mutableMapOf<String, com.afrchat.app.data.model.Group>()

    init { loadConversations() }

    private fun loadConversations() {
        val uid = authRepository.currentUserId ?: return
        viewModelScope.launch {
            chatRepository.observeConversations(uid).collect { conversations ->
                val enriched = conversations.map { convo ->
                    if (convo.type == "private") {
                        val peerUid = convo.participantIds.firstOrNull { it != uid }
                        val peer = peerUid?.let { pid ->
                            userCache[pid] ?: (userRepository.getUser(pid) as? com.afrchat.app.data.model.AfrResult.Success)?.data?.also { userCache[pid] = it }
                        }
                        ConversationListItem(convo, peer, peer?.fullName ?: "Utilisateur", peer?.photoUrl ?: "")
                    } else {
                        val group = convo.groupId?.let { gid ->
                            groupCache[gid] ?: groupRepository.observeGroup(gid).first()?.also { groupCache[gid] = it }
                        }
                        ConversationListItem(convo, null, group?.name ?: "Groupe", group?.photoUrl ?: "")
                    }
                }
                _items.value = enriched
            }
        }
    }
}
