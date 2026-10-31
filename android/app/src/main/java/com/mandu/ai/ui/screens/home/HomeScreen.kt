package com.mandu.ai.ui.screens.home

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import com.mandu.ai.ui.screens.chat.ChatViewModel

@Composable
fun HomeScreen(
    onOpenChat: (String?) -> Unit,
    onOpenVoice: () -> Unit,
    onOpenImage: () -> Unit,
    onOpenSearch: () -> Unit,
    chatVm: ChatViewModel = hiltViewModel(),
) {
    val chatState = chatVm.state.collectAsStateWithLifecycle().value
    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Text("Mandu AI", style = MaterialTheme.typography.headlineMedium)
        Text("Your personal AI assistant", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(bottom = 24.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            QuickCard("Chat", Icons.Default.Chat, Modifier.weight(1f)) { onOpenChat(null) }
            QuickCard("Voice", Icons.Default.Mic, Modifier.weight(1f), onOpenVoice)
        }
        Spacer(Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            QuickCard("Image", Icons.Default.Image, Modifier.weight(1f), onOpenImage)
            QuickCard("Search", Icons.Default.Search, Modifier.weight(1f), onOpenSearch)
        }
        Text("Recent chats", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 24.dp, bottom = 8.dp))
        chatState.conversations.take(5).forEach { c ->
            ListItem(
                headlineContent = { Text(c.title) },
                modifier = Modifier.fillMaxWidth(),
                trailingContent = { TextButton(onClick = { onOpenChat(c.id) }) { Text("Open") } },
            )
        }
    }
}

@Composable
private fun QuickCard(label: String, icon: ImageVector, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = modifier.height(100.dp)) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = label)
            Text(label, modifier = Modifier.padding(top = 8.dp))
        }
    }
}
