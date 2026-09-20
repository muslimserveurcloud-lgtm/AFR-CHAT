package com.afrchat.app.ui.call

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.afrchat.app.data.model.CallSession
import com.afrchat.app.data.repository.AuthRepository
import com.afrchat.app.data.repository.CallRepository
import com.afrchat.app.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Écoute globale des appels entrants, active tant qu'un utilisateur est connecté (voir NavGraph). */
@HiltViewModel
class IncomingCallObserverViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val callRepository: CallRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _incomingCall = MutableStateFlow<CallSession?>(null)
    val incomingCall: StateFlow<CallSession?> = _incomingCall.asStateFlow()

    private val _peerName = MutableStateFlow("")
    val peerName: StateFlow<String> = _peerName.asStateFlow()

    fun start() {
        val uid = authRepository.currentUserId ?: return
        viewModelScope.launch {
            callRepository.observeIncomingCalls(uid).collect { call ->
                _incomingCall.value = call
                if (call != null) {
                    val peer = userRepository.getUser(call.callerUid)
                    _peerName.value = (peer as? com.afrchat.app.data.model.AfrResult.Success)?.data?.fullName ?: "AFR CHAT"
                }
            }
        }
    }

    fun clear() { _incomingCall.value = null }
}
