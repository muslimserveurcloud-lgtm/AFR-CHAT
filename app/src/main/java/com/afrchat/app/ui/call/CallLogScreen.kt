package com.afrchat.app.ui.call

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallMissed
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.afrchat.app.ui.components.AvatarImage
import com.afrchat.app.utils.TimeFormatter

@Composable
fun CallLogScreen(viewModel: CallLogViewModel = hiltViewModel()) {
    val calls by viewModel.calls.collectAsState()
    LaunchedEffect(Unit) { viewModel.load() }

    Column(Modifier.fillMaxSize()) {
        TopAppBar(title = { Text("Appels") })
        if (calls.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                Text("Aucun appel récent.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn {
                items(calls, key = { it.id }) { call ->
                    Row(
                        Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        AvatarImage(null, size = 44.dp)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(if (call.callerUid == viewModel.currentUid) "Appel sortant" else "Appel entrant")
                            Text(TimeFormatter.formatConversationTime(call.createdAt), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Icon(
                            if (call.status == "missed" || call.status == "declined") Icons.Filled.CallMissed
                            else if (call.isVideo) Icons.Filled.Videocam else Icons.Filled.Call,
                            contentDescription = null,
                            tint = if (call.status == "missed" || call.status == "declined") MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                        )
                    }
                    HorizontalDivider()
                }
            }
        }
    }
}
