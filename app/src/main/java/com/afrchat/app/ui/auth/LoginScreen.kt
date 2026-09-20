package com.afrchat.app.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.afrchat.app.ui.components.AfrChatButton
import com.afrchat.app.ui.components.AfrChatTextField

@Composable
fun LoginScreen(
    onLoggedIn: () -> Unit,
    onNavigateToSignUp: () -> Unit,
    onNavigateToForgotPassword: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel()
) {
    val state = viewModel.uiState

    LaunchedEffect(state.success) {
        if (state.success) onLoggedIn()
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("AFR CHAT", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Text("Parlez. Partagez. Rapprochez-vous.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(40.dp))

        AfrChatTextField(state.emailOrPhone, viewModel::onEmailChange, "E-mail ou téléphone")
        Spacer(Modifier.height(12.dp))
        AfrChatTextField(state.password, viewModel::onPasswordChange, "Mot de passe", isPassword = true)

        state.errorMessage?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
        }

        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onNavigateToForgotPassword, modifier = Modifier.align(Alignment.End)) {
            Text("Mot de passe oublié ?")
        }

        Spacer(Modifier.height(16.dp))
        AfrChatButton("Se connecter", onClick = viewModel::submit, isLoading = state.isLoading)

        Spacer(Modifier.height(16.dp))
        TextButton(onClick = onNavigateToSignUp, modifier = Modifier.fillMaxWidth()) {
            Text("Pas de compte ? Inscrivez-vous")
        }
    }
}
