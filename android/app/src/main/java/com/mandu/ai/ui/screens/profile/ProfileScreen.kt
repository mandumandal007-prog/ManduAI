package com.mandu.ai.ui.screens.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.mandu.ai.core.data.local.SessionStore
import com.mandu.ai.core.data.repo.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileUiState(val email: String? = null, val name: String? = null, val dark: Boolean = false, val systemPrompt: String = "")

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val repo: AuthRepository,
    private val session: SessionStore,
) : ViewModel() {
    val state: StateFlow<ProfileUiState> = kotlinx.coroutines.flow.combine(
        repo.emailFlow, repo.nameFlow, session.darkTheme, session.systemPrompt,
    ) { email, name, dark, sp -> ProfileUiState(email, name, dark, sp) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ProfileUiState())

    fun setDark(dark: Boolean) { viewModelScope.launch { session.setDarkTheme(dark) } }
    fun setSystemPrompt(p: String) { viewModelScope.launch { session.setSystemPrompt(p) } }
    fun logout(onDone: () -> Unit) { viewModelScope.launch { repo.logout(); onDone() } }
}

@Composable
fun ProfileScreen(onLogout: () -> Unit, vm: ProfileViewModel = hiltViewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text("Profile", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(bottom = 12.dp))
        Text(state.name ?: "Mandu user", style = MaterialTheme.typography.titleMedium)
        Text(state.email ?: "", style = MaterialTheme.typography.bodyMedium)
        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Dark theme")
            Switch(checked = state.dark, onCheckedChange = { vm.setDark(it) })
        }
        OutlinedTextField(
            value = state.systemPrompt,
            onValueChange = { vm.setSystemPrompt(it) },
            label = { Text("System prompt") },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            minLines = 3,
        )
        Button(onClick = { vm.logout(onLogout) }, modifier = Modifier.fillMaxWidth().padding(top = 24.dp)) {
            Text("Log out")
        }
    }
}
