package com.afrchat.app.ui.group

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.afrchat.app.ui.components.AfrChatTextField
import com.afrchat.app.ui.components.AvatarImage

@Composable
fun CreateGroupScreen(
    onBack: () -> Unit,
    onGroupCreated: (String) -> Unit,
    viewModel: CreateGroupViewModel = hiltViewModel()
) {
    val state = viewModel.uiState

    LaunchedEffect(state.createdConversationId) {
        state.createdConversationId?.let { onGroupCreated(it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nouveau groupe") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, null) } },
                actions = {
                    IconButton(onClick = viewModel::createGroup) { Icon(Icons.Filled.Check, contentDescription = "Créer") }
                }
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding).padding(16.dp)) {
            AfrChatTextField(state.name, viewModel::onNameChange, "Nom du groupe")
            Spacer(Modifier.height(12.dp))

            if (state.selectedMembers.isNotEmpty()) {
                LazyRow {
                    items(state.selectedMembers) { user ->
                        Column(
                            modifier = Modifier.padding(end = 8.dp),
                            horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
                        ) {
                            AvatarImage(user.photoUrl, size = 48.dp)
                            Text(user.firstName, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            AfrChatTextField(state.searchQuery, viewModel::onSearchChange, "Ajouter des membres")
            state.errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }

            Spacer(Modifier.height(8.dp))
            LazyColumn {
                items(state.searchResults, key = { it.uid }) { user ->
                    val selected = state.selectedMembers.any { it.uid == user.uid }
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 8.dp)
                            .clickable { viewModel.toggleMember(user) },
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        AvatarImage(user.photoUrl, size = 44.dp)
                        Spacer(Modifier.width(12.dp))
                        Text(user.fullName, modifier = Modifier.weight(1f))
                        Checkbox(checked = selected, onCheckedChange = { viewModel.toggleMember(user) })
                    }
                }
            }

            if (state.isLoading) {
                Spacer(Modifier.height(12.dp))
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
        }
    }
}
