package com.afrchat.app.ui.chat

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.background
import androidx.compose.foundation.background
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.afrchat.app.ui.components.AvatarImage

@Composable
fun ChatScreen(
    peerName: String,
    onBack: () -> Unit,
    onStartCall: (isVideo: Boolean, peerUid: String) -> Unit,
    onOpenGroupInfo: (groupId: String) -> Unit,
    viewModel: ChatViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val listState = rememberLazyListState()

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { viewModel.sendMedia(context, it, "image", context.contentResolver.getType(it) ?: "image/jpeg") }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, null) } },
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.let { m ->
                            if (state.isGroup && state.groupId != null) {
                                m.clickable { onOpenGroupInfo(state.groupId!!) }
                            } else m
                        }
                    ) {
                        AvatarImage(null, size = 36.dp)
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(peerName, style = MaterialTheme.typography.titleMedium)
                            if (state.isPeerTyping) Text("écrit…", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                },
                actions = {
                    if (state.isGroup && state.groupId != null) {
                        IconButton(onClick = { onOpenGroupInfo(state.groupId!!) }) {
                            Icon(Icons.Filled.Info, contentDescription = "Infos du groupe")
                        }
                    }
                    IconButton(onClick = { onStartCall(false, state.peerUid) }) { Icon(Icons.Filled.Call, contentDescription = "Appel audio") }
                    IconButton(onClick = { onStartCall(true, state.peerUid) }) { Icon(Icons.Filled.Videocam, contentDescription = "Appel vidéo") }
                }
            )
        },
        bottomBar = {
            Column {
                state.replyTo?.let { reply ->
                    Row(
                        Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant).padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Réponse à : ${reply.text.take(40)}", style = MaterialTheme.typography.bodySmall)
                        TextButton(onClick = { viewModel.setReplyTo(null) }) { Text("Annuler") }
                    }
                }
                state.uploadProgress?.let { progress ->
                    LinearProgressIndicator(progress = { progress / 100f }, modifier = Modifier.fillMaxWidth())
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { imagePicker.launch("image/*") }) {
                        Icon(Icons.Filled.AttachFile, contentDescription = "Joindre un média")
                    }
                    OutlinedTextField(
                        value = state.messageInput,
                        onValueChange = viewModel::onInputChange,
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Message…") },
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
                        maxLines = 4
                    )
                    Spacer(Modifier.width(4.dp))
                    if (state.messageInput.isBlank()) {
                        IconButton(onClick = { /* Enregistrement vocal : voir ui/chat/VoiceMessageRecorder.kt */ }) {
                            Icon(Icons.Filled.Mic, contentDescription = "Message vocal")
                        }
                    } else {
                        IconButton(onClick = viewModel::sendText) {
                            Icon(Icons.Filled.Send, contentDescription = "Envoyer", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            state = listState,
            reverseLayout = true,
            modifier = Modifier.fillMaxSize().padding(padding)
        ) {
            items(state.messages, key = { it.id.ifEmpty { it.clientTempId } }) { message ->
                MessageBubble(
                    message = message,
                    isMine = message.senderId == viewModel.currentUid,
                    onLongPress = { viewModel.setReplyTo(message) }
                )
            }
        }
    }
}
