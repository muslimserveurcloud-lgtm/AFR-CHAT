package com.afrchat.app.ui.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun ReportsScreen(onBack: () -> Unit, viewModel: AdminViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Signalements") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, null) } }
            )
        }
    ) { padding ->
        LazyColumn(Modifier.padding(padding)) {
            items(state.reports, key = { it.id }) { report ->
                Column(Modifier.fillMaxWidth().padding(16.dp)) {
                    Text("Cible : ${report.targetType} (${report.targetId.take(10)}…)", style = MaterialTheme.typography.titleMedium)
                    Text("Motif : ${report.reason}")
                    if (report.details.isNotBlank()) Text(report.details, style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(8.dp))
                    Row {
                        TextButton(onClick = { viewModel.resolveReport(report.id, "reviewed") }) { Text("Traiter") }
                        TextButton(onClick = { viewModel.resolveReport(report.id, "dismissed") }) { Text("Ignorer") }
                    }
                    HorizontalDivider()
                }
            }
        }
    }
}
