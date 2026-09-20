package com.afrchat.app.ui.story

import android.content.Context
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.afrchat.app.data.model.Story
import com.afrchat.app.data.repository.AuthRepository
import com.afrchat.app.data.repository.ChatRepository
import com.afrchat.app.data.repository.MediaRepository
import com.afrchat.app.data.repository.StoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StoryListViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val storyRepository: StoryRepository,
    private val chatRepository: ChatRepository
) : ViewModel() {

    private val _stories = MutableStateFlow<List<Story>>(emptyList())
    val stories: StateFlow<List<Story>> = _stories.asStateFlow()

    init {
        val uid = authRepository.currentUserId ?: return
        viewModelScope.launch {
            chatRepository.observeConversations(uid).collect { conversations ->
                val contactUids = conversations.flatMap { it.participantIds }.filter { it != uid }.distinct() + uid
                storyRepository.observeActiveStories(contactUids).collect { _stories.value = it }
            }
        }
    }
}

@HiltViewModel
class CreateStoryViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val storyRepository: StoryRepository,
    private val mediaRepository: MediaRepository
) : ViewModel() {
    val isLoading = MutableStateFlow(false)
    var posted = MutableStateFlow(false)

    fun postText(text: String, backgroundColor: String) {
        val uid = authRepository.currentUserId ?: return
        viewModelScope.launch {
            isLoading.value = true
            storyRepository.postStory(Story(ownerUid = uid, type = "text", content = text, backgroundColor = backgroundColor))
            isLoading.value = false
            posted.value = true
        }
    }

    fun postMedia(context: Context, uri: Uri, type: String) {
        val uid = authRepository.currentUserId ?: return
        viewModelScope.launch {
            isLoading.value = true
            mediaRepository.uploadFile(context, uri, "stories", uid, if (type == "video") "video/mp4" else "image/jpeg").collect { state ->
                if (state is MediaRepository.UploadState.Success) {
                    storyRepository.postStory(Story(ownerUid = uid, type = type, mediaUrl = state.url))
                    isLoading.value = false
                    posted.value = true
                }
            }
        }
    }
}
