package com.afrchat.app.ui.call

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.afrchat.app.data.model.IceCandidateData
import com.afrchat.app.data.repository.AuthRepository
import com.afrchat.app.data.repository.CallRepository
import com.afrchat.app.ui.call.webrtc.WebRtcClient
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.webrtc.AudioTrack
import org.webrtc.EglBase
import org.webrtc.IceCandidate
import org.webrtc.SessionDescription
import org.webrtc.SurfaceViewRenderer
import org.webrtc.VideoTrack
import javax.inject.Inject

data class CallUiState(
    val status: String = "connecting", // connecting | ringing | connected | ended
    val isVideo: Boolean = false,
    val isCaller: Boolean = false,
    val micEnabled: Boolean = true,
    val cameraEnabled: Boolean = true,
    val remoteVideoTrack: VideoTrack? = null,
    val elapsedSeconds: Int = 0
)

@HiltViewModel
class CallViewModel @Inject constructor(
    application: Application,
    private val authRepository: AuthRepository,
    private val callRepository: CallRepository,
    savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {

    val callId: String = savedStateHandle.get<String>("callId").orEmpty()
    val isVideo: Boolean = savedStateHandle.get<String>("isVideo")?.toBoolean() ?: false
    val isCaller: Boolean = savedStateHandle.get<String>("isCaller")?.toBoolean() ?: false
    val peerUid: String = savedStateHandle.get<String>("peerUid").orEmpty()

    val eglBase: EglBase = EglBase.create()

    private val _uiState = MutableStateFlow(CallUiState(isVideo = isVideo, isCaller = isCaller))
    val uiState: StateFlow<CallUiState> = _uiState.asStateFlow()

    private var webRtcClient: WebRtcClient? = null
    private var timerJob: kotlinx.coroutines.Job? = null

    fun start(localRenderer: SurfaceViewRenderer?) {
        val uid = authRepository.currentUserId ?: return
        webRtcClient = WebRtcClient(
            context = getApplication(),
            eglBase = eglBase,
            onIceCandidate = { candidate ->
                viewModelScope.launch {
                    callRepository.sendIceCandidate(
                        callId, from = if (isCaller) "caller" else "callee",
                        candidate = IceCandidateData(candidate.sdpMid ?: "", candidate.sdpMLineIndex, candidate.sdp)
                    )
                }
            },
            onRemoteStream = { videoTrack, _ ->
                _uiState.value = _uiState.value.copy(remoteVideoTrack = videoTrack, status = "connected")
                startTimerIfNeeded()
            }
        )

        val localStream = webRtcClient!!.startLocalMedia(isVideo, localRenderer)
        webRtcClient!!.createPeerConnection(localStream)

        if (isCaller) {
            webRtcClient!!.createOffer { offer ->
                viewModelScope.launch { callRepository.createCall(uid, peerUid, isVideo, offer.description) }
            }
        }

        observeCall()
        observeRemoteCandidates()
    }

    private fun observeCall() {
        viewModelScope.launch {
            callRepository.observeCall(callId).collect { session ->
                session ?: return@collect
                when (session.status) {
                    "ringing" -> _uiState.value = _uiState.value.copy(status = "ringing")
                    "accepted" -> {
                        if (isCaller && session.answerSdp != null) {
                            webRtcClient?.setRemoteDescription(SessionDescription(SessionDescription.Type.ANSWER, session.answerSdp))
                        } else if (!isCaller && session.offerSdp != null && _uiState.value.status != "connected") {
                            // callee : la réponse est créée lors de l'appel à acceptIncomingCall()
                        }
                    }
                    "declined", "ended" -> _uiState.value = _uiState.value.copy(status = "ended")
                }
            }
        }
    }

    /** À appeler côté callee lorsqu'il décroche. */
    fun acceptIncomingCall(offerSdp: String) {
        webRtcClient?.setRemoteDescription(SessionDescription(SessionDescription.Type.OFFER, offerSdp))
        webRtcClient?.createAnswer { answer ->
            viewModelScope.launch { callRepository.acceptCall(callId, answer.description) }
        }
    }

    private fun observeRemoteCandidates() {
        val remoteFrom = if (isCaller) "callee" else "caller"
        viewModelScope.launch {
            callRepository.observeRemoteIceCandidates(callId, remoteFrom).collect { data ->
                webRtcClient?.addIceCandidate(IceCandidate(data.sdpMid, data.sdpMLineIndex, data.candidate))
            }
        }
    }

    private fun startTimerIfNeeded() {
        if (timerJob != null) return
        timerJob = viewModelScope.launch {
            while (true) {
                kotlinx.coroutines.delay(1000)
                _uiState.value = _uiState.value.copy(elapsedSeconds = _uiState.value.elapsedSeconds + 1)
            }
        }
    }

    fun toggleMic() {
        val enabled = !_uiState.value.micEnabled
        webRtcClient?.toggleMic(enabled)
        _uiState.value = _uiState.value.copy(micEnabled = enabled)
    }

    fun toggleCamera() {
        val enabled = !_uiState.value.cameraEnabled
        webRtcClient?.toggleCamera(enabled)
        _uiState.value = _uiState.value.copy(cameraEnabled = enabled)
    }

    fun switchCamera() = webRtcClient?.switchCamera()

    fun hangUp() {
        viewModelScope.launch { callRepository.endCall(callId) }
        cleanup()
    }

    fun decline() {
        viewModelScope.launch { callRepository.declineCall(callId) }
        cleanup()
    }

    private fun cleanup() {
        timerJob?.cancel()
        webRtcClient?.close()
        eglBase.release()
    }

    override fun onCleared() {
        super.onCleared()
        cleanup()
    }
}
