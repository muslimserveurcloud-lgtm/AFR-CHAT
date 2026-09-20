package com.afrchat.app.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.afrchat.app.data.model.AfrResult
import com.afrchat.app.data.model.Report
import com.afrchat.app.data.repository.AdminRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AdminUiState(
    val reports: List<Report> = emptyList(),
    val stats: Map<String, Any> = emptyMap(),
    val isLoading: Boolean = false,
    val message: String? = null
)

/**
 * Toutes les actions mutent l'état via des Cloud Functions "callable" qui vérifient le
 * custom claim "admin" côté serveur (voir firebase/functions/index.js). Cet écran n'est
 * accessible dans l'app que si user.isAdmin == true (ProfileScreen), mais la sécurité réelle
 * est imposée par le serveur, pas par ce simple contrôle d'affichage côté client.
 */
@HiltViewModel
class AdminViewModel @Inject constructor(
    private val adminRepository: AdminRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminUiState())
    val uiState: StateFlow<AdminUiState> = _uiState.asStateFlow()

    init { refresh() }

    fun refresh() {
        _uiState.value = _uiState.value.copy(isLoading = true)
        viewModelScope.launch {
            val reportsResult = adminRepository.listOpenReports()
            val statsResult = adminRepository.getStats()
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                reports = (reportsResult as? AfrResult.Success)?.data ?: emptyList(),
                stats = (statsResult as? AfrResult.Success)?.data ?: emptyMap()
            )
        }
    }

    fun banUser(uid: String, reason: String) = viewModelScope.launch {
        val result = adminRepository.banUser(uid, reason)
        _uiState.value = _uiState.value.copy(message = (result as? AfrResult.Error)?.message ?: "Utilisateur banni.")
        refresh()
    }

    fun unbanUser(uid: String) = viewModelScope.launch {
        adminRepository.unbanUser(uid)
        refresh()
    }

    fun resolveReport(reportId: String, status: String) = viewModelScope.launch {
        adminRepository.resolveReport(reportId, status)
        refresh()
    }
}
