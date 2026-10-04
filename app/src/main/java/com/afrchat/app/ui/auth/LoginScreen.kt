package com.afrchat.app.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.afrchat.app.ui.components.AfrChatButton
import com.afrchat.app.ui.components.AfrChatTextField
import com.afrchat.app.ui.components.AuthHeader
import com.afrchat.app.ui.components.ErrorBanner

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
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 24.dp)
    ) {
        Spacer(Modifier.height(56.dp))
        AuthHeader("AFR CHAT", "Parlez. Partagez. Rapprochez-vous.")
        Spacer(Modifier.height(40.dp))

        AfrChatTextField(
            value = state.emailOrPhone,
            onValueChange = viewModel::onEmailChange,
            label = "Adresse e-mail",
            keyboardType = KeyboardType.Email,
            leadingIcon = Icons.Filled.Email
        )
        Spacer(Modifier.height(12.dp))
        AfrChatTextField(
            value = state.password,
            onValueChange = viewModel::onPasswordChange,
            label = "Mot de passe",
            isPassword = true,
            leadingIcon = Icons.Filled.Lock
        )

        state.errorMessage?.let {
            Spacer(Modifier.height(12.dp))
            ErrorBanner(it)
        }

        Spacer(Modifier.height(4.dp))
        TextButton(onClick = onNavigateToForgotPassword, modifier = Modifier.align(Alignment.End)) {
            Text("Mot de passe oublié ?")
        }

        Spacer(Modifier.height(8.dp))
        AfrChatButton("Se connecter", onClick = viewModel::submit, isLoading = state.isLoading)

        Spacer(Modifier.height(16.dp))
        TextButton(onClick = onNavigateToSignUp, modifier = Modifier.fillMaxWidth()) {
            Text("Pas de compte ? Inscrivez-vous")
        }
    }
}
