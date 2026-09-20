package com.afrchat.app.ui.admin

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun AdminDashboardScreen(
    onBack: () -> Unit,
    onOpenReports: () -> Unit,
    viewModel: AdminViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Espace administrateur") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, null) } }
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding).padding(16.dp)) {
            Text("Statistiques", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            if (state.isLoading) CircularProgressIndicator()
            else {
                StatRow("Utilisateurs", state.stats["totalUsers"]?.toString() ?: "—")
                StatRow("Messages (30 derniers jours)", state.stats["messagesLast30Days"]?.toString() ?: "—")
                StatRow("Groupes actifs", state.stats["activeGroups"]?.toString() ?: "—")
                StatRow("Signalements ouverts", state.reports.size.toString())
            }

            Spacer(Modifier.height(24.dp))
            Button(onClick = onOpenReports, modifier = Modifier.fillMaxWidth()) {
                Text("Voir les signalements (${state.reports.size})")
            }
        }
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label)
        Text(value, fontWeight = FontWeight.SemiBold)
    }
}
