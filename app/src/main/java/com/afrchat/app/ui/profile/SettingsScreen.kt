package com.afrchat.app.ui.profile

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun SettingsScreen(viewModel: ProfileViewModel = hiltViewModel()) {
    val user by viewModel.user.collectAsState()
    val privacy = user?.privacy ?: com.afrchat.app.data.model.PrivacySettings()

    Column(Modifier.fillMaxSize().padding(24.dp)) {
        Text("Confidentialité", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(16.dp))

        SettingSwitchRow("Afficher ma dernière connexion", privacy.showLastSeen) {
            viewModel.updatePrivacy(privacy.copy(showLastSeen = it))
        }
        SettingSwitchRow("Afficher mon statut en ligne", privacy.showOnlineStatus) {
            viewModel.updatePrivacy(privacy.copy(showOnlineStatus = it))
        }
        SettingSwitchRow("Accusés de lecture", privacy.showReadReceipts) {
            viewModel.updatePrivacy(privacy.copy(showReadReceipts = it))
        }
    }
}

@Composable
private fun SettingSwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
    ) {
        Text(label)
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
