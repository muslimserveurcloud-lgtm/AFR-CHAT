package com.afrchat.app.ui.main

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.afrchat.app.ui.components.AvatarImage

@Composable
fun ConversationListScreen(
    onOpenConversation: (String, String) -> Unit,
    onOpenSearch: () -> Unit,
    onOpenProfile: () -> Unit,
    onCreateGroup: () -> Unit,
    viewModel: ConversationListViewModel = hiltViewModel()
) {
    val items by viewModel.items.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AFR CHAT", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = onOpenSearch) { Icon(Icons.Filled.Search, contentDescription = "Rechercher") }
                    IconButton(onClick = onOpenProfile) {
                        AvatarImage(currentUser?.photoUrl, size = 32.dp)
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onCreateGroup) { Icon(Icons.Filled.GroupAdd, contentDescription = "Nouveau groupe") }
        }
    ) { padding ->
        Column(Modifier.padding(padding)) {
        if (items.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                Text("Aucune discussion pour l'instant.\nLancez une recherche pour démarrer !", textAlign = androidx.compose.ui.text.style.TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn {
                items(items, key = { it.conversation.id }) { item ->
                    ConversationRow(item, currentUser?.uid.orEmpty()) {
                        val cid = item.conversation.id
                        onOpenConversation(cid, item.displayName)
                    }
                    HorizontalDivider(modifier = Modifier.padding(start = 82.dp))
                }
            }
        }
        }
    }
}
