package com.afrchat.app.ui.call

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import org.webrtc.RendererCommon
import org.webrtc.SurfaceViewRenderer

@Composable
fun CallScreen(
    peerName: String,
    onEnded: () -> Unit,
    viewModel: CallViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var localRenderer by remember { androidx.compose.runtime.mutableStateOf<SurfaceViewRenderer?>(null) }
    var remoteRenderer by remember { androidx.compose.runtime.mutableStateOf<SurfaceViewRenderer?>(null) }

    LaunchedEffect(Unit) { viewModel.start(localRenderer) }
    LaunchedEffect(state.status) { if (state.status == "ended") onEnded() }

    DisposableEffect(Unit) {
        onDispose { }
    }

    Box(Modifier.fillMaxSize().background(Color(0xFF0F0E17))) {
        if (state.isVideo) {
            AndroidView(
                factory = { ctx ->
                    SurfaceViewRenderer(ctx).also {
                        it.init(viewModel.eglBase.eglBaseContext, null)
                        it.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL)
                        remoteRenderer = it
                        state.remoteVideoTrack?.addSink(it)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
            AndroidView(
                factory = { ctx ->
                    SurfaceViewRenderer(ctx).also {
                        it.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL)
                        it.setZOrderMediaOverlay(true)
                        localRenderer = it
                    }
                },
                modifier = Modifier.align(Alignment.TopEnd).padding(16.dp).size(120.dp, 160.dp)
            )
        } else {
            Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(peerName, color = Color.White, style = MaterialTheme.typography.headlineMedium)
                Text(
                    if (state.status == "connected") "%02d:%02d".format(state.elapsedSeconds / 60, state.elapsedSeconds % 60) else "Appel en cours…",
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
        }

        Row(
            Modifier.align(Alignment.BottomCenter).padding(32.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            FloatingActionButton(onClick = viewModel::toggleMic, containerColor = Color.White.copy(alpha = 0.2f)) {
                Icon(if (state.micEnabled) Icons.Filled.Mic else Icons.Filled.MicOff, contentDescription = null, tint = Color.White)
            }
            if (state.isVideo) {
                FloatingActionButton(onClick = viewModel::toggleCamera, containerColor = Color.White.copy(alpha = 0.2f)) {
                    Icon(if (state.cameraEnabled) Icons.Filled.Videocam else Icons.Filled.VideocamOff, contentDescription = null, tint = Color.White)
                }
                FloatingActionButton(onClick = viewModel::switchCamera, containerColor = Color.White.copy(alpha = 0.2f)) {
                    Icon(Icons.Filled.Cameraswitch, contentDescription = null, tint = Color.White)
                }
            }
            FloatingActionButton(onClick = viewModel::hangUp, containerColor = MaterialTheme.colorScheme.error) {
                Icon(Icons.Filled.CallEnd, contentDescription = "Raccrocher", tint = Color.White)
            }
        }
    }
}
