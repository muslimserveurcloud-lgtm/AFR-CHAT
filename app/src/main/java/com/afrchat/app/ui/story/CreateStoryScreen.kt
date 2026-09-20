package com.afrchat.app.ui.story

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.afrchat.app.ui.components.AfrChatButton

@Composable
fun CreateStoryScreen(onPosted: () -> Unit, viewModel: CreateStoryViewModel = hiltViewModel()) {
    var text by remember { mutableStateOf("") }
    val posted by viewModel.posted.collectAsState()
    val context = LocalContext.current

    val mediaPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            val type = if (context.contentResolver.getType(it)?.startsWith("video") == true) "video" else "image"
            viewModel.postMedia(context, it, type)
        }
    }

    LaunchedEffect(posted) { if (posted) onPosted() }

    Column(
        Modifier.fillMaxSize().background(Color(0xFF5B4FE9)).padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("Nouveau statut", color = Color.White, style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(24.dp))
        OutlinedTextField(
            value = text, onValueChange = { text = it },
            placeholder = { Text("Quoi de neuf ?", color = Color.White.copy(alpha = 0.7f)) },
            colors = OutlinedTextFieldDefaults.colors(unfocusedTextColor = Color.White, focusedTextColor = Color.White),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(16.dp))
        AfrChatButton("Publier en texte", onClick = { if (text.isNotBlank()) viewModel.postText(text, "#5B4FE9") })
        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = { mediaPicker.launch("image/*") }, modifier = Modifier.fillMaxWidth()) {
            Text("Choisir une photo ou vidéo", color = Color.White)
        }
    }
}
