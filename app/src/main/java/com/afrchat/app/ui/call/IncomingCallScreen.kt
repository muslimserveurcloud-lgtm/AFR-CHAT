package com.afrchat.app.ui.call

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.afrchat.app.data.model.CallSession
import com.afrchat.app.ui.components.AvatarImage

/**
 * Écran plein écran affiché lorsqu'un appel entrant est détecté (voir observation globale
 * dans navigation/NavGraph.kt, branchée sur CallRepository.observeIncomingCalls).
 */
@Composable
fun IncomingCallScreen(
    call: CallSession,
    peerName: String,
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    Box(Modifier.fillMaxSize().background(Color(0xFF0F0E17))) {
        Column(
            Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AvatarImage(null, size = 100.dp)
            Spacer(Modifier.height(16.dp))
            Text(peerName, color = Color.White, style = MaterialTheme.typography.headlineMedium)
            Text(
                if (call.isVideo) "Appel vidéo entrant…" else "Appel audio entrant…",
                color = Color.White.copy(alpha = 0.7f)
            )
        }

        Row(
            Modifier.align(Alignment.BottomCenter).padding(48.dp),
            horizontalArrangement = Arrangement.spacedBy(48.dp)
        ) {
            FloatingActionButton(onClick = onDecline, containerColor = MaterialTheme.colorScheme.error) {
                Icon(Icons.Filled.CallEnd, contentDescription = "Refuser", tint = Color.White)
            }
            FloatingActionButton(onClick = onAccept, containerColor = Color(0xFF2ECC71)) {
                Icon(Icons.Filled.Call, contentDescription = "Accepter", tint = Color.White)
            }
        }
    }
}
