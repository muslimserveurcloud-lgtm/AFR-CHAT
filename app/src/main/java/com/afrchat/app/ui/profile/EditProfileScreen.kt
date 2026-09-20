package com.afrchat.app.ui.profile

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.afrchat.app.ui.components.AfrChatButton
import com.afrchat.app.ui.components.AfrChatTextField

@Composable
fun EditProfileScreen(onBack: () -> Unit, viewModel: ProfileViewModel = hiltViewModel()) {
    val user by viewModel.user.collectAsState()
    var firstName by remember(user) { mutableStateOf(user?.firstName.orEmpty()) }
    var lastName by remember(user) { mutableStateOf(user?.lastName.orEmpty()) }
    var status by remember(user) { mutableStateOf(user?.statusMessage.orEmpty()) }

    Column(Modifier.fillMaxSize().padding(24.dp)) {
        Text("Modifier le profil", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(24.dp))
        AfrChatTextField(firstName, { firstName = it }, "Prénom")
        Spacer(Modifier.height(12.dp))
        AfrChatTextField(lastName, { lastName = it }, "Nom")
        Spacer(Modifier.height(12.dp))
        AfrChatTextField(status, { status = it }, "Statut / description")
        Spacer(Modifier.height(24.dp))
        AfrChatButton("Enregistrer", onClick = {
            viewModel.updateName(firstName, lastName)
            viewModel.updateStatus(status)
            onBack()
        })
    }
}
