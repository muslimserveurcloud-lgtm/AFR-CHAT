package com.afrchat.app.ui.call

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.afrchat.app.data.model.CallSession
import com.afrchat.app.data.repository.AuthRepository
import com.afrchat.app.data.repository.CallRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CallLogViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val callRepository: CallRepository
) : ViewModel() {

    val currentUid: String get() = authRepository.currentUserId.orEmpty()

    private val _calls = MutableStateFlow<List<CallSession>>(emptyList())
    val calls: StateFlow<List<CallSession>> = _calls.asStateFlow()

    fun load() {
        if (currentUid.isEmpty()) return
        viewModelScope.launch {
            _calls.value = try { callRepository.loadCallLog() } catch (_: Exception) { _calls.value }
        }
    }
}
