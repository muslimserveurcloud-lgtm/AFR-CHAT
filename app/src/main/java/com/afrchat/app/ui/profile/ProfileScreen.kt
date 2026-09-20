package com.afrchat.app.ui.profile

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.afrchat.app.ui.components.AvatarImage

@Composable
fun ProfileScreen(
    onBack: () -> Unit,
    onLoggedOut: () -> Unit,
    onOpenAdmin: () -> Unit,
    onEditProfile: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val user by viewModel.user.collectAsState()
    val context = LocalContext.current
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { viewModel.updatePhoto(context, it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profil") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, null) } }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).padding(24.dp).fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box {
                AvatarImage(user?.photoUrl, size = 100.dp)
                IconButton(
                    onClick = { photoPicker.launch("image/*") },
                    modifier = Modifier.align(Alignment.BottomEnd)
                ) { Icon(Icons.Filled.Edit, contentDescription = "Changer la photo") }
            }
            viewModel.uploadProgress?.let { LinearProgressIndicator(progress = { it / 100f }) }

            Spacer(Modifier.height(16.dp))
            Text(user?.fullName.orEmpty(), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(user?.email.orEmpty(), color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            Text(user?.statusMessage.orEmpty(), style = MaterialTheme.typography.bodyMedium)

            Spacer(Modifier.height(32.dp))

            OutlinedButton(onClick = onEditProfile, modifier = Modifier.fillMaxWidth()) {
                Text("Modifier le profil")
            }
            Spacer(Modifier.height(12.dp))

            if (user?.isAdmin == true) {
                OutlinedButton(onClick = onOpenAdmin, modifier = Modifier.fillMaxWidth()) {
                    Text("Espace administrateur")
                }
                Spacer(Modifier.height(12.dp))
            }

            Button(
                onClick = { viewModel.logout(onLoggedOut) },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.fillMaxWidth()
            ) { Text("Se déconnecter") }
        }
    }
}
