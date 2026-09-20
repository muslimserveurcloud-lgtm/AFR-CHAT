package com.afrchat.app.ui.story

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.afrchat.app.ui.components.AvatarImage

@Composable
fun StoryListScreen(
    onCreateStory: () -> Unit,
    onViewStories: (String) -> Unit,
    viewModel: StoryListViewModel = hiltViewModel()
) {
    val stories by viewModel.stories.collectAsState()
    val grouped = stories.groupBy { it.ownerUid }

    Column(Modifier.fillMaxSize()) {
        TopAppBar(title = { Text("Statuts") })

        Row(
            Modifier.fillMaxWidth().clickable(onClick = onCreateStory).padding(16.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Box {
                AvatarImage(null, size = 54.dp)
                Box(
                    Modifier.align(androidx.compose.ui.Alignment.BottomEnd).size(20.dp).clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = androidx.compose.ui.Alignment.Center
                ) { Icon(Icons.Filled.Add, contentDescription = null, tint = androidx.compose.ui.graphics.Color.White, modifier = Modifier.size(14.dp)) }
            }
            Spacer(Modifier.width(12.dp))
            Text("Ajouter un statut", style = MaterialTheme.typography.titleMedium)
        }
        HorizontalDivider()

        LazyColumn {
            items(grouped.keys.toList()) { ownerUid ->
                Row(
                    Modifier.fillMaxWidth().clickable { onViewStories(ownerUid) }.padding(16.dp),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    AvatarImage(null, size = 54.dp)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(ownerUid.take(10) + "…", style = MaterialTheme.typography.titleMedium)
                        Text("${grouped[ownerUid]?.size ?: 0} statut(s)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
