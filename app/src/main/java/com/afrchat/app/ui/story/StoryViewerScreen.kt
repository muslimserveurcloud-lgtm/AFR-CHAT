package com.afrchat.app.ui.story

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import coil.compose.AsyncImage
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun StoryViewerScreen(
    ownerUid: String,
    onClose: () -> Unit,
    viewModel: StoryListViewModel = hiltViewModel()
) {
    val stories by viewModel.stories.collectAsState()
    val ownerStories = stories.filter { it.ownerUid == ownerUid }
    var index by remember { mutableIntStateOf(0) }

    LaunchedEffect(index, ownerStories.size) {
        if (ownerStories.isEmpty()) { onClose(); return@LaunchedEffect }
        kotlinx.coroutines.delay(5000)
        if (index < ownerStories.size - 1) index++ else onClose()
    }

    val current = ownerStories.getOrNull(index) ?: return

    Box(
        Modifier.fillMaxSize().background(
            try { Color(android.graphics.Color.parseColor(current.backgroundColor)) } catch (e: Exception) { Color.Black }
        )
    ) {
        when (current.type) {
            "image" -> AsyncImage(model = current.mediaUrl, contentDescription = null, modifier = Modifier.fillMaxSize())
            "video" -> com.afrchat.app.ui.components.VideoPlayerView(
                url = current.mediaUrl,
                modifier = Modifier.fillMaxSize(),
                autoPlay = true,
                showControls = false
            )
            else -> Text(
                current.content, color = Color.White, style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.align(androidx.compose.ui.Alignment.Center).padding(24.dp)
            )
        }
    }
}
