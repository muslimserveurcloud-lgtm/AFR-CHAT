package com.afrchat.app.ui.call.webrtc

import android.content.Context
import com.afrchat.app.utils.Constants
import org.webrtc.AudioSource
import org.webrtc.AudioTrack
import org.webrtc.Camera2Enumerator
import org.webrtc.CameraVideoCapturer
import org.webrtc.DefaultVideoDecoderFactory
import org.webrtc.DefaultVideoEncoderFactory
import org.webrtc.EglBase
import org.webrtc.IceCandidate
import org.webrtc.MediaConstraints
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.SdpObserver
import org.webrtc.SessionDescription
import org.webrtc.SurfaceTextureHelper
import org.webrtc.SurfaceViewRenderer
import org.webrtc.VideoCapturer
import org.webrtc.VideoSource
import org.webrtc.VideoTrack

/**
 * Client WebRTC pair-à-pair pour les appels audio/vidéo AFR CHAT.
 *
 * Le SIGNALING (échange de l'offre/réponse SDP et des candidats ICE) passe par Firestore
 * (voir CallRepository), pas par ce client — ce client ne s'occupe que de la connexion média.
 *
 * ⚠️ Pour une connectivité fiable derrière NAT/pare-feu, configure un serveur TURN dans
 * Constants.kt (TURN_URL, TURN_USERNAME, TURN_CREDENTIAL). Avec les seuls serveurs STUN publics,
 * l'appel ne s'établira que si au moins un des deux réseaux autorise une connexion directe.
 */
class WebRtcClient(
    private val context: Context,
    private val eglBase: EglBase,
    private val onIceCandidate: (IceCandidate) -> Unit,
    private val onRemoteStream: (VideoTrack?, AudioTrack?) -> Unit
) {
    private val factory: PeerConnectionFactory
    private var peerConnection: PeerConnection? = null
    private var localVideoCapturer: VideoCapturer? = null
    private var localVideoTrack: VideoTrack? = null
    private var localAudioTrack: AudioTrack? = null
    private var videoSource: VideoSource? = null
    private var audioSource: AudioSource? = null
    private var surfaceTextureHelper: SurfaceTextureHelper? = null

    init {
        PeerConnectionFactory.initialize(
            PeerConnectionFactory.InitializationOptions.builder(context)
                .setEnableInternalTracer(false)
                .createInitializationOptions()
        )
        factory = PeerConnectionFactory.builder()
            .setVideoEncoderFactory(DefaultVideoEncoderFactory(eglBase.eglBaseContext, true, true))
            .setVideoDecoderFactory(DefaultVideoDecoderFactory(eglBase.eglBaseContext))
            .createPeerConnectionFactory()
    }

    private fun iceServers(): List<PeerConnection.IceServer> = buildList {
        Constants.STUN_SERVERS.forEach { add(PeerConnection.IceServer.builder(it).createIceServer()) }
        if (Constants.TURN_URL.contains("A_CONFIGURER").not() && Constants.TURN_USERNAME != "A_CONFIGURER") {
            add(
                PeerConnection.IceServer.builder(Constants.TURN_URL)
                    .setUsername(Constants.TURN_USERNAME)
                    .setPassword(Constants.TURN_CREDENTIAL)
                    .createIceServer()
            )
        }
    }

    fun startLocalMedia(isVideo: Boolean, localRenderer: SurfaceViewRenderer?): MediaStream {
        val streamId = "afrchat_local_stream"
        val stream = factory.createLocalMediaStream(streamId)

        // Audio (toujours activé, même pour un appel vidéo)
        audioSource = factory.createAudioSource(MediaConstraints())
        localAudioTrack = factory.createAudioTrack("afrchat_audio", audioSource)
        stream.addTrack(localAudioTrack)

        if (isVideo) {
            localRenderer?.init(eglBase.eglBaseContext, null)
            val enumerator = Camera2Enumerator(context)
            val cameraName = enumerator.deviceNames.firstOrNull { enumerator.isFrontFacing(it) } ?: enumerator.deviceNames.firstOrNull()
            localVideoCapturer = cameraName?.let { enumerator.createCapturer(it, null) }

            surfaceTextureHelper = SurfaceTextureHelper.create("CaptureThread", eglBase.eglBaseContext)
            videoSource = factory.createVideoSource(false)
            localVideoCapturer?.initialize(surfaceTextureHelper, context, videoSource!!.capturerObserver)
            localVideoCapturer?.startCapture(1280, 720, 30)

            localVideoTrack = factory.createVideoTrack("afrchat_video", videoSource)
            localRenderer?.let { localVideoTrack?.addSink(it) }
            stream.addTrack(localVideoTrack)
        }
        return stream
    }

    fun createPeerConnection(localStream: MediaStream) {
        val rtcConfig = PeerConnection.RTCConfiguration(iceServers()).apply {
            sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
        }
        peerConnection = factory.createPeerConnection(rtcConfig, object : PeerConnection.Observer {
            override fun onIceCandidate(candidate: IceCandidate) { onIceCandidate.invoke(candidate) }
            override fun onAddStream(stream: MediaStream) {
                onRemoteStream.invoke(stream.videoTracks.firstOrNull(), stream.audioTracks.firstOrNull())
            }
            override fun onSignalingChange(p0: PeerConnection.SignalingState?) {}
            override fun onIceConnectionChange(p0: PeerConnection.IceConnectionState?) {}
            override fun onIceConnectionReceivingChange(p0: Boolean) {}
            override fun onIceGatheringChange(p0: PeerConnection.IceGatheringState?) {}
            override fun onIceCandidatesRemoved(p0: Array<out IceCandidate>?) {}
            override fun onRemoveStream(p0: MediaStream?) {}
            override fun onDataChannel(p0: org.webrtc.DataChannel?) {}
            override fun onRenegotiationNeeded() {}
            override fun onAddTrack(receiver: org.webrtc.RtpReceiver?, streams: Array<out MediaStream>?) {
                val track = receiver?.track()
                if (track is VideoTrack) onRemoteStream.invoke(track, null)
            }
        })
        localStream.audioTracks.forEach { peerConnection?.addTrack(it) }
        localStream.videoTracks.forEach { peerConnection?.addTrack(it) }
    }

    fun createOffer(onSuccess: (SessionDescription) -> Unit) {
        val constraints = MediaConstraints()
        peerConnection?.createOffer(object : SdpObserverAdapter() {
            override fun onCreateSuccess(p0: SessionDescription?) {
                val desc = p0 ?: return
                peerConnection?.setLocalDescription(SdpObserverAdapter(), desc)
                onSuccess(desc)
            }
        }, constraints)
    }

    fun createAnswer(onSuccess: (SessionDescription) -> Unit) {
        val constraints = MediaConstraints()
        peerConnection?.createAnswer(object : SdpObserverAdapter() {
            override fun onCreateSuccess(p0: SessionDescription?) {
                val desc = p0 ?: return
                peerConnection?.setLocalDescription(SdpObserverAdapter(), desc)
                onSuccess(desc)
            }
        }, constraints)
    }

    fun setRemoteDescription(sdp: SessionDescription) {
        peerConnection?.setRemoteDescription(SdpObserverAdapter(), sdp)
    }

    fun addIceCandidate(candidate: IceCandidate) {
        peerConnection?.addIceCandidate(candidate)
    }

    fun toggleMic(enabled: Boolean) { localAudioTrack?.setEnabled(enabled) }
    fun toggleCamera(enabled: Boolean) { localVideoTrack?.setEnabled(enabled) }

    fun switchCamera() {
        (localVideoCapturer as? CameraVideoCapturer)?.switchCamera(null)
    }

    fun close() {
        try { localVideoCapturer?.stopCapture() } catch (_: Exception) { }
        localVideoCapturer?.dispose()
        surfaceTextureHelper?.dispose()
        videoSource?.dispose()
        audioSource?.dispose()
        peerConnection?.close()
        peerConnection?.dispose()
    }

    private open class SdpObserverAdapter : SdpObserver {
        override fun onCreateSuccess(p0: SessionDescription?) {}
        override fun onSetSuccess() {}
        override fun onCreateFailure(p0: String?) {}
        override fun onSetFailure(p0: String?) {}
    }
}
