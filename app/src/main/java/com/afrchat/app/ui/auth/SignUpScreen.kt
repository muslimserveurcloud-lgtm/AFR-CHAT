package com.afrchat.app.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.afrchat.app.ui.components.AfrChatButton
import com.afrchat.app.ui.components.AfrChatTextField

@Composable
fun SignUpScreen(
    onSignedUp: () -> Unit,
    onNavigateToLogin: () -> Unit,
    viewModel: SignUpViewModel = hiltViewModel()
) {
    val state = viewModel.uiState

    LaunchedEffect(state.success) {
        if (state.success) onSignedUp()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Spacer(Modifier.height(24.dp))
        Text("Créer un compte", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        Text("Rejoins AFR CHAT en quelques secondes", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(32.dp))

        AfrChatTextField(state.firstName, viewModel::onFirstNameChange, "Prénom", isError = state.fieldErrors.containsKey("firstName"), errorText = state.fieldErrors["firstName"])
        Spacer(Modifier.height(12.dp))
        AfrChatTextField(state.lastName, viewModel::onLastNameChange, "Nom", isError = state.fieldErrors.containsKey("lastName"), errorText = state.fieldErrors["lastName"])
        Spacer(Modifier.height(12.dp))
        AfrChatTextField(state.email, viewModel::onEmailChange, "Adresse e-mail", isError = state.fieldErrors.containsKey("email"), errorText = state.fieldErrors["email"])
        Spacer(Modifier.height(12.dp))
        AfrChatTextField(state.phone, viewModel::onPhoneChange, "Numéro de téléphone (facultatif)", isError = state.fieldErrors.containsKey("phone"), errorText = state.fieldErrors["phone"])
        Spacer(Modifier.height(12.dp))
        AfrChatTextField(state.password, viewModel::onPasswordChange, "Mot de passe", isPassword = true, isError = state.fieldErrors.containsKey("password"), errorText = state.fieldErrors["password"])
        Spacer(Modifier.height(12.dp))
        AfrChatTextField(state.confirmPassword, viewModel::onConfirmPasswordChange, "Confirmer le mot de passe", isPassword = true, isError = state.fieldErrors.containsKey("confirmPassword"), errorText = state.fieldErrors["confirmPassword"])
        Spacer(Modifier.height(16.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = state.termsAccepted, onCheckedChange = viewModel::onTermsChange)
            Text("J'accepte les conditions d'utilisation d'AFR CHAT", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
        }
        state.fieldErrors["terms"]?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }

        state.errorMessage?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
        }

        Spacer(Modifier.height(24.dp))
        AfrChatButton("S'inscrire", onClick = viewModel::submit, isLoading = state.isLoading)

        Spacer(Modifier.height(16.dp))
        TextButton(onClick = onNavigateToLogin, modifier = Modifier.fillMaxWidth()) {
            Text("Déjà un compte ? Connectez-vous")
        }
        Spacer(Modifier.height(24.dp))
    }
}
