package com.afrchat.app.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.afrchat.app.data.model.Message
import com.afrchat.app.ui.theme.LocalAfrChatExtraColors
import com.afrchat.app.utils.TimeFormatter

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun MessageBubble(
    message: Message,
    isMine: Boolean,
    onLongPress: () -> Unit
) {
    val extra = LocalAfrChatExtraColors.current
    val bubbleColor = if (isMine) extra.bubbleOut else extra.bubbleIn
    val textColor = if (isMine) Color.White else MaterialTheme.colorScheme.onBackground

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 3.dp),
        horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .background(bubbleColor, RoundedCornerShape(16.dp))
                .combinedClickable(onClick = {}, onLongClick = onLongPress)
                .padding(10.dp)
        ) {
            if (message.isDeletedForEveryone) {
                Text("Message supprimé", color = textColor.copy(alpha = 0.6f), style = MaterialTheme.typography.bodyMedium)
            } else {
                when (message.type) {
                    "image" -> AsyncImage(
                        model = message.mediaUrl, contentDescription = null,
                        modifier = Modifier.widthIn(max = 260.dp).heightIn(max = 260.dp)
                    )
                    "video" -> com.afrchat.app.ui.components.VideoPlayerView(
                        url = message.mediaUrl,
                        modifier = Modifier.widthIn(max = 260.dp).heightIn(max = 260.dp),
                        showControls = true
                    )
                    "audio" -> Text("🎤 Message vocal (${message.mediaDurationMs / 1000}s)", color = textColor)
                    "file" -> Text("📎 ${message.fileName}", color = textColor)
                    "contact" -> Text("👤 Contact partagé : ${message.text}", color = textColor)
                    else -> Text(message.text, color = textColor, style = MaterialTheme.typography.bodyLarge)
                }

                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        TimeFormatter.formatMessageTime(message.sentAt),
                        color = textColor.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.bodySmall
                    )
                    if (isMine) {
                        Spacer(Modifier.width(4.dp))
                        Icon(
                            imageVector = if (message.status == "read") Icons.Filled.DoneAll else if (message.status == "delivered") Icons.Filled.DoneAll else Icons.Filled.Done,
                            contentDescription = message.status,
                            tint = if (message.status == "read") Color(0xFF34D1FF) else textColor.copy(alpha = 0.7f),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
                if (message.reactions.isNotEmpty()) {
                    Text(message.reactions.values.toSet().joinToString(" "), style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
