package com.afrchat.app.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.afrchat.app.ui.components.AfrChatButton
import com.afrchat.app.ui.components.AfrChatTextField

@Composable
fun ForgotPasswordScreen(
    onBack: () -> Unit,
    viewModel: ForgotPasswordViewModel = hiltViewModel()
) {
    val state = viewModel.uiState
    Column(modifier = Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center) {
        Text("Récupérer mon compte", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text("Recevez un lien de réinitialisation par e-mail.", style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(24.dp))
        AfrChatTextField(state.email, viewModel::onEmailChange, "Adresse e-mail")

        state.message?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = if (state.isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
        }

        Spacer(Modifier.height(24.dp))
        AfrChatButton("Envoyer le lien", onClick = viewModel::submit, isLoading = state.isLoading)
        Spacer(Modifier.height(12.dp))
        TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Retour à la connexion") }
    }
}
