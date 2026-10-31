package com.mandu.ai.ui.screens.search

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mandu.ai.core.data.dto.SearchResult
import com.mandu.ai.core.data.repo.SearchRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SearchUiState(val loading: Boolean = false, val answer: String? = null, val results: List<SearchResult> = emptyList(), val error: String? = null)

@HiltViewModel
class SearchViewModel @Inject constructor(private val repo: SearchRepository) : ViewModel() {
    private val _state = MutableStateFlow(SearchUiState())
    val state: StateFlow<SearchUiState> = _state.asStateFlow()

    fun search(query: String) {
        if (query.isBlank()) return
        _state.value = _state.value.copy(loading = true, error = null)
        viewModelScope.launch {
            try {
                val res = repo.search(query)
                _state.value = _state.value.copy(loading = false, answer = res.answer, results = res.results)
            } catch (e: Exception) {
                _state.value = _state.value.copy(loading = false, error = e.message ?: "Failed")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(onBack: () -> Unit, vm: SearchViewModel = hiltViewModel()) {
    val state by vm.state.collectAsState()
    var query by remember { mutableStateOf("") }
    val uri = LocalUriHandler.current
    Scaffold(topBar = { TopAppBar(title = { Text("Search") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } }) }) { pad ->
        Column(modifier = Modifier.fillMaxSize().padding(pad).padding(16.dp)) {
            OutlinedTextField(value = query, onValueChange = { query = it }, label = { Text("Search the web") }, modifier = Modifier.fillMaxWidth())
            Button(onClick = { vm.search(query) }, enabled = !state.loading && query.isNotBlank(), modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                Text(if (state.loading) "Searching..." else "Search")
            }
            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp)) }
            state.answer?.let { Text(it, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 12.dp)) }
            LazyColumn(modifier = Modifier.fillMaxSize().padding(top = 8.dp)) {
                items(state.results) { r ->
                    ListItem(
                        headlineContent = { Text(r.title, fontWeight = FontWeight.SemiBold) },
                        supportingContent = { Text(r.snippet ?: "") },
                        modifier = Modifier.fillMaxWidth(),
                        trailingContent = { TextButton(onClick = { try { uri.open(r.url) } catch (_: Exception) {} }) { Text("Open") } },
                    )
                }
            }
        }
    }
}
