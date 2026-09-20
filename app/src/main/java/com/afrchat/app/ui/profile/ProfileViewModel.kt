package com.afrchat.app.ui.profile

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.afrchat.app.data.model.PrivacySettings
import com.afrchat.app.data.model.User
import com.afrchat.app.data.repository.AuthRepository
import com.afrchat.app.data.repository.MediaRepository
import com.afrchat.app.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val mediaRepository: MediaRepository
) : ViewModel() {

    private val _user = MutableStateFlow<User?>(null)
    val user: StateFlow<User?> = _user.asStateFlow()

    var uploadProgress by mutableStateOf<Int?>(null)
        private set

    init {
        authRepository.currentUserId?.let { uid ->
            viewModelScope.launch { userRepository.observeUser(uid).collect { _user.value = it } }
        }
    }

    fun updateName(firstName: String, lastName: String) {
        val uid = authRepository.currentUserId ?: return
        viewModelScope.launch { userRepository.updateProfile(uid, mapOf("firstName" to firstName, "lastName" to lastName)) }
    }

    fun updateStatus(status: String) {
        val uid = authRepository.currentUserId ?: return
        viewModelScope.launch { userRepository.updateProfile(uid, mapOf("statusMessage" to status)) }
    }

    fun updatePrivacy(privacy: PrivacySettings) {
        val uid = authRepository.currentUserId ?: return
        viewModelScope.launch {
            userRepository.updateProfile(uid, mapOf(
                "privacy.showLastSeen" to privacy.showLastSeen,
                "privacy.showOnlineStatus" to privacy.showOnlineStatus,
                "privacy.showReadReceipts" to privacy.showReadReceipts,
                "privacy.whoCanAddToGroups" to privacy.whoCanAddToGroups
            ))
        }
    }

    fun updatePhoto(context: Context, uri: Uri) {
        val uid = authRepository.currentUserId ?: return
        viewModelScope.launch {
            mediaRepository.uploadFile(context, uri, "profile_photos", uid, context.contentResolver.getType(uri) ?: "image/jpeg").collect { state ->
                when (state) {
                    is MediaRepository.UploadState.Progress -> uploadProgress = state.percent
                    is MediaRepository.UploadState.Success -> {
                        uploadProgress = null
                        userRepository.updateProfile(uid, mapOf("photoUrl" to state.url))
                    }
                    is MediaRepository.UploadState.Error -> uploadProgress = null
                }
            }
        }
    }

    fun logout(onDone: () -> Unit) {
        val uid = authRepository.currentUserId
        viewModelScope.launch {
            if (uid != null) userRepository.setOnlineStatus(uid, false)
            authRepository.logout()
            onDone()
        }
    }
}
