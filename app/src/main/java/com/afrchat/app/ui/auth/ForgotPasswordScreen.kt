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
import com.afrchat.app.ui.components.AuthHeader
import androidx.compose.ui.text.input.KeyboardType

@Composable
fun ForgotPasswordScreen(
    onBack: () -> Unit,
    viewModel: ForgotPasswordViewModel = hiltViewModel()
) {
    val state = viewModel.uiState
    Column(modifier = Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center) {
        AuthHeader("Récupérer mon compte", "Recevez un lien de réinitialisation par e-mail.")
        Spacer(Modifier.height(28.dp))
        AfrChatTextField(state.email, viewModel::onEmailChange, "Adresse e-mail", keyboardType = KeyboardType.Email)

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
