package com.mandu.ai.ui.screens.voice

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    var recording by remember { mutableStateOf(false) }
    val perm = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted -> if (granted) recording = true }

    Scaffold(topBar = { TopAppBar(title = { Text("Voice") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } }) }) { pad ->
        Column(modifier = Modifier.fillMaxSize().padding(pad), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text(if (recording) "Listening..." else "Tap to talk", style = MaterialTheme.typography.titleMedium)
            FilledIconButton(onClick = {
                if (recording) recording = false
                else if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) recording = true
                else perm.launch(Manifest.permission.RECORD_AUDIO)
            }, modifier = Modifier.size(96.dp).padding(top = 24.dp)) {
                Icon(Icons.Default.Mic, contentDescription = "Mic", modifier = Modifier.size(40.dp))
            }
        }
    }
}
