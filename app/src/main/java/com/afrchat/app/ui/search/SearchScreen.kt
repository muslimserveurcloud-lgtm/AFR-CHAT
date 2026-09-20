package com.afrchat.app.ui.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.afrchat.app.ui.components.AvatarImage

@Composable
fun SearchScreen(
    onBack: () -> Unit,
    onOpenConversation: (String, String) -> Unit,
    viewModel: SearchViewModel = hiltViewModel()
) {
    val state = viewModel.uiState

    LaunchedEffect(state.openConversationId) {
        state.openConversationId?.let { cid ->
            val peer = state.results.firstOrNull()
            onOpenConversation(cid, peer?.fullName ?: "Discussion")
        }
    }

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                OutlinedTextField(
                    value = state.query,
                    onValueChange = viewModel::onQueryChange,
                    placeholder = { Text("Rechercher une personne…") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, null) } }
        )

        if (state.isLoading) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())

        LazyColumn {
            items(state.results, key = { it.uid }) { user ->
                Row(
                    Modifier.fillMaxWidth().clickable { viewModel.startConversation(user.uid) }.padding(16.dp),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    AvatarImage(user.photoUrl, size = 48.dp)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(user.fullName, style = MaterialTheme.typography.titleMedium)
                        Text(user.statusMessage, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
