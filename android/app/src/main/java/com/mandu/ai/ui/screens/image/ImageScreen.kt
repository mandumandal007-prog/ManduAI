package com.mandu.ai.ui.screens.image

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import com.mandu.ai.core.data.dto.ImageItem
import com.mandu.ai.core.data.repo.ImageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ImageUiState(val loading: Boolean = false, val images: List<ImageItem> = emptyList(), val error: String? = null)

@HiltViewModel
class ImageViewModel @Inject constructor(private val repo: ImageRepository) : ViewModel() {
    private val _state = MutableStateFlow(ImageUiState())
    val state: StateFlow<ImageUiState> = _state.asStateFlow()

    fun generate(prompt: String) {
        if (prompt.isBlank()) return
        _state.value = _state.value.copy(loading = true, error = null)
        viewModelScope.launch {
            try {
                val res = repo.generate(prompt)
                _state.value = _state.value.copy(loading = false, images = res.images)
            } catch (e: Exception) {
                _state.value = _state.value.copy(loading = false, error = e.message ?: "Failed")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageScreen(onBack: () -> Unit, vm: ImageViewModel = hiltViewModel()) {
    val state by vm.state.collectAsState()
    var prompt by remember { mutableStateOf("") }
    Scaffold(topBar = { TopAppBar(title = { Text("Image") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } }) }) { pad ->
        Column(modifier = Modifier.fillMaxSize().padding(pad).padding(16.dp)) {
            OutlinedTextField(value = prompt, onValueChange = { prompt = it }, label = { Text("Describe the image") }, modifier = Modifier.fillMaxWidth())
            Button(onClick = { vm.generate(prompt) }, enabled = !state.loading && prompt.isNotBlank(), modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                Text(if (state.loading) "Generating..." else "Generate")
            }
            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp)) }
            LazyVerticalGrid(columns = GridCells.Fixed(2), modifier = Modifier.fillMaxSize().padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.images) { img ->
                    val url = img.url ?: img.b64?.let { "data:image/png;base64,$it" }
                    if (url != null) AsyncImage(model = url, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxWidth().aspectRatio(1f))
                }
            }
        }
    }
}
