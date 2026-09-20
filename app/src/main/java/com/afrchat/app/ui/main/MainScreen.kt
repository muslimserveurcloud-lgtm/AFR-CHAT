package com.afrchat.app.ui.main

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.afrchat.app.ui.call.CallLogScreen
import com.afrchat.app.ui.profile.SettingsScreen
import com.afrchat.app.ui.story.StoryListScreen

@Composable
fun MainScreen(
    onOpenConversation: (String, String) -> Unit,
    onOpenSearch: () -> Unit,
    onOpenProfile: () -> Unit,
    onCreateStory: () -> Unit,
    onViewStories: (String) -> Unit,
    onCreateGroup: () -> Unit
) {
    var tab by remember { mutableIntStateOf(0) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = tab == 0, onClick = { tab = 0 },
                    icon = { Icon(Icons.Filled.Chat, contentDescription = null) }, label = { Text("Discussions") }
                )
                NavigationBarItem(
                    selected = tab == 1, onClick = { tab = 1 },
                    icon = { Icon(Icons.Outlined.AutoAwesome, contentDescription = null) }, label = { Text("Statuts") }
                )
                NavigationBarItem(
                    selected = tab == 2, onClick = { tab = 2 },
                    icon = { Icon(Icons.Filled.Call, contentDescription = null) }, label = { Text("Appels") }
                )
                NavigationBarItem(
                    selected = tab == 3, onClick = { tab = 3 },
                    icon = { Icon(Icons.Filled.Settings, contentDescription = null) }, label = { Text("Réglages") }
                )
            }
        }
    ) { padding ->
        Modifier.padding(padding).let {
            when (tab) {
                0 -> ConversationListScreen(onOpenConversation, onOpenSearch, onOpenProfile, onCreateGroup)
                1 -> StoryListScreen(onCreateStory, onViewStories)
                2 -> CallLogScreen()
                else -> SettingsScreen()
            }
        }
    }
}
