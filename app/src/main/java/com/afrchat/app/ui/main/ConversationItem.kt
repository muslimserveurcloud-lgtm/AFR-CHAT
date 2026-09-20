package com.afrchat.app.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.afrchat.app.ui.components.AvatarImage
import com.afrchat.app.ui.components.OnlineStatusDot
import com.afrchat.app.ui.components.UnreadBadge
import com.afrchat.app.utils.TimeFormatter

@Composable
fun ConversationRow(item: ConversationListItem, currentUid: String, onClick: () -> Unit) {
    val unread = item.conversation.unreadCount[currentUid] ?: 0
    val isTyping = item.conversation.typingUserIds.any { it != currentUid }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box {
            AvatarImage(item.displayPhoto, size = 54.dp)
            if (item.peer?.isOnline == true) {
                OnlineStatusDot(true, modifier = Modifier.align(Alignment.BottomEnd).background(Color.White, shape = androidx.compose.foundation.shape.CircleShape))
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(item.displayName, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(2.dp))
            Text(
                text = if (isTyping) "écrit un message…" else item.conversation.lastMessage,
                color = if (unread > 0) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = if (unread > 0) FontWeight.Medium else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        Spacer(Modifier.width(8.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text(
                TimeFormatter.formatConversationTime(item.conversation.lastMessageAt),
                style = MaterialTheme.typography.bodySmall,
                color = if (unread > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(6.dp))
            UnreadBadge(unread)
        }
    }
}
