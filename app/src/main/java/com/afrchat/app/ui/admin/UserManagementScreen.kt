package com.afrchat.app.ui.admin

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.afrchat.app.ui.components.AfrChatButton
import com.afrchat.app.ui.components.AfrChatTextField

/** Action ponctuelle de bannissement/débannissement par UID (recherche d'utilisateur à brancher sur UserRepository.searchUsers). */
@Composable
fun UserManagementScreen(viewModel: AdminViewModel = hiltViewModel()) {
    var uid by remember { mutableStateOf("") }
    var reason by remember { mutableStateOf("") }

    Column(Modifier.fillMaxSize().padding(24.dp)) {
        Text("Gestion des utilisateurs", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(16.dp))
        AfrChatTextField(uid, { uid = it }, "UID de l'utilisateur")
        Spacer(Modifier.height(12.dp))
        AfrChatTextField(reason, { reason = it }, "Motif du bannissement")
        Spacer(Modifier.height(16.dp))
        Row {
            AfrChatButton("Bannir", onClick = { if (uid.isNotBlank()) viewModel.banUser(uid, reason) }, modifier = Modifier.weight(1f))
            Spacer(Modifier.width(12.dp))
            OutlinedButton(onClick = { if (uid.isNotBlank()) viewModel.unbanUser(uid) }, modifier = Modifier.weight(1f)) {
                Text("Débannir")
            }
        }
    }
}
