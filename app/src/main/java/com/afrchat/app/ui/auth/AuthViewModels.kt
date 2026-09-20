package com.afrchat.app.ui.auth

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.afrchat.app.data.model.AfrResult
import com.afrchat.app.data.repository.AuthRepository
import com.afrchat.app.utils.Validators
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SignUpUiState(
    val firstName: String = "",
    val lastName: String = "",
    val email: String = "",
    val phone: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val termsAccepted: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val fieldErrors: Map<String, String> = emptyMap(),
    val success: Boolean = false
)

@HiltViewModel
class SignUpViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    var uiState by mutableStateOf(SignUpUiState())
        private set

    fun onFirstNameChange(v: String) { uiState = uiState.copy(firstName = v) }
    fun onLastNameChange(v: String) { uiState = uiState.copy(lastName = v) }
    fun onEmailChange(v: String) { uiState = uiState.copy(email = v) }
    fun onPhoneChange(v: String) { uiState = uiState.copy(phone = v) }
    fun onPasswordChange(v: String) { uiState = uiState.copy(password = v) }
    fun onConfirmPasswordChange(v: String) { uiState = uiState.copy(confirmPassword = v) }
    fun onTermsChange(v: Boolean) { uiState = uiState.copy(termsAccepted = v) }

    fun submit() {
        val errors = mutableMapOf<String, String>()
        Validators.nameError(uiState.firstName)?.let { errors["firstName"] = it }
        Validators.nameError(uiState.lastName)?.let { errors["lastName"] = it }
        if (!Validators.isValidEmail(uiState.email)) errors["email"] = "Adresse e-mail invalide."
        if (uiState.phone.isNotBlank() && !Validators.isValidPhone(uiState.phone)) errors["phone"] = "Numéro de téléphone invalide."
        Validators.passwordError(uiState.password)?.let { errors["password"] = it }
        if (uiState.password != uiState.confirmPassword) errors["confirmPassword"] = "Les mots de passe ne correspondent pas."
        if (!uiState.termsAccepted) errors["terms"] = "Vous devez accepter les conditions d'utilisation."

        if (errors.isNotEmpty()) {
            uiState = uiState.copy(fieldErrors = errors)
            return
        }

        uiState = uiState.copy(isLoading = true, errorMessage = null, fieldErrors = emptyMap())
        viewModelScope.launch {
            when (val result = authRepository.signUp(uiState.firstName, uiState.lastName, uiState.email, uiState.password, uiState.phone)) {
                is AfrResult.Success -> uiState = uiState.copy(isLoading = false, success = true)
                is AfrResult.Error -> uiState = uiState.copy(isLoading = false, errorMessage = result.message)
                AfrResult.Loading -> {}
            }
        }
    }
}

data class LoginUiState(
    val emailOrPhone: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val success: Boolean = false
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    var uiState by mutableStateOf(LoginUiState())
        private set

    fun onEmailChange(v: String) { uiState = uiState.copy(emailOrPhone = v) }
    fun onPasswordChange(v: String) { uiState = uiState.copy(password = v) }

    fun submit() {
        if (uiState.emailOrPhone.isBlank() || uiState.password.isBlank()) {
            uiState = uiState.copy(errorMessage = "Merci de remplir tous les champs.")
            return
        }
        uiState = uiState.copy(isLoading = true, errorMessage = null)
        viewModelScope.launch {
            when (val result = authRepository.login(uiState.emailOrPhone.trim(), uiState.password)) {
                is AfrResult.Success -> uiState = uiState.copy(isLoading = false, success = true)
                is AfrResult.Error -> uiState = uiState.copy(isLoading = false, errorMessage = result.message)
                AfrResult.Loading -> {}
            }
        }
    }
}

data class ForgotPasswordUiState(
    val email: String = "",
    val isLoading: Boolean = false,
    val message: String? = null,
    val isError: Boolean = false
)

@HiltViewModel
class ForgotPasswordViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {
    var uiState by mutableStateOf(ForgotPasswordUiState())
        private set

    fun onEmailChange(v: String) { uiState = uiState.copy(email = v) }

    fun submit() {
        if (!Validators.isValidEmail(uiState.email)) {
            uiState = uiState.copy(message = "Adresse e-mail invalide.", isError = true)
            return
        }
        uiState = uiState.copy(isLoading = true, message = null)
        viewModelScope.launch {
            when (val result = authRepository.sendPasswordReset(uiState.email.trim())) {
                is AfrResult.Success -> uiState = uiState.copy(isLoading = false, message = "E-mail de réinitialisation envoyé.", isError = false)
                is AfrResult.Error -> uiState = uiState.copy(isLoading = false, message = result.message, isError = true)
                AfrResult.Loading -> {}
            }
        }
    }
}
