package com.afrchat.app.ui.group

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PersonRemove
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
fun GroupInfoScreen(
    onBack: () -> Unit,
    onLeft: () -> Unit,
    viewModel: GroupInfoViewModel = hiltViewModel()
) {
    val group by viewModel.group.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(group?.name ?: "Groupe") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, null) } }
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding).padding(16.dp)) {
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                AvatarImage(group?.photoUrl, size = 64.dp)
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(group?.name.orEmpty(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("${group?.memberUids?.size ?: 0} membres", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.height(8.dp))
            if (group?.description?.isNotBlank() == true) {
                Text(group!!.description, style = MaterialTheme.typography.bodyMedium)
            }

            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Text("Seuls les admins peuvent publier")
                Switch(
                    checked = group?.onlyAdminsCanPost == true,
                    onCheckedChange = { viewModel.toggleOnlyAdminsCanPost(it) }
                )
            }

            Spacer(Modifier.height(16.dp))
            Text("Membres", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(group?.memberUids.orEmpty()) { uid ->
                    val isAdmin = group?.adminUids?.contains(uid) == true
                    val isOwner = group?.ownerUid == uid
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 10.dp),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        AvatarImage(null, size = 40.dp)
                        Spacer(Modifier.width(12.dp))
                        Text(uid.take(8) + "…", modifier = Modifier.weight(1f))
                        if (isOwner) Text("Propriétaire", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                        else if (isAdmin) Text("Admin", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)

                        if (uid != group?.ownerUid && uid != viewModel.currentUid) {
                            IconButton(onClick = { viewModel.removeMember(uid) }) {
                                Icon(Icons.Filled.PersonRemove, contentDescription = "Retirer du groupe")
                            }
                        }
                    }
                }
            }

            OutlinedButton(
                onClick = { viewModel.leaveGroup(onLeft) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) { Text("Quitter le groupe") }
        }
    }
}
