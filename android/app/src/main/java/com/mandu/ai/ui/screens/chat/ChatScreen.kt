package com.mandu.ai.ui.screens.chat

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    conversationId: String?,
    onBack: (() -> Unit)?,
    vm: ChatViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val ctx = LocalContext.current
    var input by remember { mutableStateOf("") }
    var showDrawer by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    LaunchedEffect(state.messages.size, state.messages.lastOrNull()?.content) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.lastIndex)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    if (onBack != null) IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") }
                    else IconButton(onClick = { showDrawer = true }) { Icon(Icons.Default.Menu, "Menu") }
                },
                title = {
                    Column {
                        Text("Mandu Chat", fontWeight = FontWeight.Bold)
                        Text(
                            state.activeId?.take(8)?.let { "Conversation $it" } ?: "New chat",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                },
                actions = {
                    if (onBack != null) {
                        IconButton(onClick = { showDrawer = true }) { Icon(Icons.Default.Menu, "Menu") }
                    }
                    IconButton(onClick = vm::newChat) { Icon(Icons.Default.Add, "New chat") }
                },
            )
        },
        bottomBar = {
            ChatInputBar(
                value = input,
                onChange = { input = it },
                streaming = state.streaming,
                onSend = { vm.send(input); input = "" },
                onStop = vm::stopGeneration,
            )
        },
    ) { pad ->
        Box(Modifier.padding(pad).fillMaxSize()) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (state.messages.isEmpty()) {
                    item { EmptyChatHint() }
                }
                items(state.messages) { msg ->
                    MessageBubble(
                        role = msg.role,
                        content = msg.content,
                        onCopy = { copyToClipboard(ctx, msg.content) },
                    )
                }
                if (state.error != null) {
                    item {
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                            Text("⚠️ ${state.error}", Modifier.padding(12.dp), color = MaterialTheme.colorScheme.onErrorContainer)
                        }
                    }
                }
                if (state.streaming) {
                    item {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                            CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(8.dp))
                            Text("Generating…", style = MaterialTheme.typography.bodySmall)
                            Spacer(Modifier.weight(1f))
                            TextButton(onClick = vm::regenerate) { Text("Regenerate") }
                        }
                    }
                }
            }

            if (showDrawer) {
                ConversationDrawer(
                    conversations = state.conversations,
                    activeId = state.activeId,
                    onClose = { showDrawer = false },
                    onSelect = { vm.select(it); showDrawer = false },
                    onDelete = { vm.delete(it) },
                    onNew = { vm.newChat(); showDrawer = false },
                )
            }
        }
    }
}

@Composable
private fun ChatInputBar(
    value: String,
    onChange: (String) -> Unit,
    streaming: Boolean,
    onSend: () -> Unit,
    onStop: () -> Unit,
) {
    Surface(tonalElevation = 4.dp) {
        Row(
            Modifier.padding(8.dp).fillMaxWidth().navigationBarsPadding(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = value, onValueChange = onChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("Ask Mandu anything…") },
                shape = RoundedCornerShape(24.dp),
                maxLines = 5,
            )
            Spacer(Modifier.width(8.dp))
            FloatingActionButton(
                onClick = if (streaming) onStop else onSend,
                containerColor = if (streaming) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
            ) {
                Icon(if (streaming) Icons.Default.Stop else Icons.Default.Send, "Send")
            }
        }
    }
}

@Composable
private fun EmptyChatHint() {
    Column(
        Modifier.fillMaxWidth().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(Icons.Default.AutoAwesome, null, Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(12.dp))
        Text("Ask anything", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text(
            "Chat, code, summarize, translate — your AI does it all.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
    }
}

@Composable
private fun MessageBubble(role: String, content: String, onCopy: () -> Unit) {
    val isUser = role == "user"
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = if (isUser) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.widthIn(max = 320.dp),
        ) {
            Column(Modifier.padding(12.dp)) {
                Text(
                    text = if (content.isBlank() && !isUser) "…" else content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isUser) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (!isUser && content.isNotBlank()) {
                    Row {
                        TextButton(onClick = onCopy) {
                            Icon(Icons.Default.ContentCopy, null, Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Copy", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BoxScope.ConversationDrawer(
    conversations: List<com.mandu.ai.core.data.dto.ConversationDto>,
    activeId: String?,
    onClose: () -> Unit,
    onSelect: (String) -> Unit,
    onDelete: (String) -> Unit,
    onNew: () -> Unit,
) {
    Box(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.4f))
    ) {
        Surface(
            modifier = Modifier.fillMaxHeight().width(300.dp),
            color = MaterialTheme.colorScheme.surface,
        ) {
            Column(Modifier.padding(16.dp).navigationBarsPadding()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Conversations", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = onClose) { Icon(Icons.Default.Close, "Close") }
                }
                Spacer(Modifier.height(8.dp))
                Button(onClick = onNew, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Add, null); Spacer(Modifier.width(8.dp)); Text("New chat")
                }
                Spacer(Modifier.height(12.dp))
                LazyColumn(Modifier.weight(1f)) {
                    items(conversations) { c ->
                        ListItem(
                            headlineContent = { Text(c.title, maxLines = 1) },
                            supportingContent = { Text(c.updatedAt?.take(10) ?: "") },
                            leadingContent = { Icon(Icons.Default.ChatBubble, null) },
                            trailingContent = {
                                IconButton(onClick = { onDelete(c.id) }) {
                                    Icon(Icons.Default.Delete, "Delete")
                                }
                            },
                            colors = ListItemDefaults.colors(
                                containerColor = if (c.id == activeId) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface,
                            ),
                            modifier = Modifier.padding(vertical = 2.dp),
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

private fun copyToClipboard(ctx: Context, text: String) {
    val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    cm.setPrimaryClip(ClipData.newPlainText("Mandu", text))
}
