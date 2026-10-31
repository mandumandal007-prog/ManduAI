package com.mandu.ai.ui.screens.connectors

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mandu.ai.core.data.dto.ConnectorCatalogItem
import com.mandu.ai.core.data.dto.ConnectorDto
import com.mandu.ai.core.data.repo.ConnectorRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ConnectorsUiState(
    val catalog: List<ConnectorCatalogItem> = emptyList(),
    val connections: List<ConnectorDto> = emptyList(),
    val error: String? = null,
)

@HiltViewModel
class ConnectorsViewModel @Inject constructor(private val repo: ConnectorRepository) : ViewModel() {
    private val _state = MutableStateFlow(ConnectorsUiState())
    val state: StateFlow<ConnectorsUiState> = _state.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            try {
                _state.value = ConnectorsUiState(repo.catalog(), repo.connections())
            } catch (e: Exception) {
                _state.value = _state.value.copy(error = e.message ?: "Failed to load")
            }
        }
    }

    fun connect(ctx: android.content.Context, provider: String) {
        viewModelScope.launch {
            try {
                val url = repo.startOAuth(provider).url
                ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            } catch (e: Exception) {
                _state.value = _state.value.copy(error = e.message ?: "Failed")
            }
        }
    }

    fun disconnect(provider: String) {
        viewModelScope.launch {
            try {
                repo.disconnect(provider)
                refresh()
            } catch (e: Exception) {
                _state.value = _state.value.copy(error = e.message ?: "Failed")
            }
        }
    }
}

@Composable
fun ConnectorsScreen(vm: ConnectorsViewModel = hiltViewModel()) {
    val state by vm.state.collectAsState()
    val ctx = LocalContext.current
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Connectors", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(bottom = 8.dp))
        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(bottom = 8.dp)) }
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(state.catalog) { item ->
                val conn = state.connections.firstOrNull { it.provider == item.id }
                ListItem(
                    headlineContent = { Text(item.label, fontWeight = FontWeight.SemiBold) },
                    supportingContent = { Text(conn?.status ?: "Not connected") },
                    trailingContent = {
                        if (conn?.status == "connected") {
                            TextButton(onClick = { vm.disconnect(item.id) }) { Text("Disconnect") }
                        } else {
                            Button(onClick = { vm.connect(ctx, item.id) }) { Text("Connect") }
                        }
                    },
                )
            }
        }
    }
}
