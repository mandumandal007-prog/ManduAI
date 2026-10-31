package com.mandu.ai.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.mandu.ai.core.data.repo.AuthRepository
import com.mandu.ai.ui.screens.auth.LoginScreen
import com.mandu.ai.ui.screens.auth.RegisterScreen
import com.mandu.ai.ui.screens.chat.ChatScreen
import com.mandu.ai.ui.screens.connectors.ConnectorsScreen
import com.mandu.ai.ui.screens.explore.ExploreScreen
import com.mandu.ai.ui.screens.home.HomeScreen
import com.mandu.ai.ui.screens.image.ImageScreen
import com.mandu.ai.ui.screens.profile.ProfileScreen
import com.mandu.ai.ui.screens.search.SearchScreen
import com.mandu.ai.ui.screens.voice.VoiceScreen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class SessionViewModel @Inject constructor(repo: AuthRepository) : ViewModel() {
    val token = repo.tokenFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    val initialized = repo.tokenFlow.map { true }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
}

@Composable
fun ManduApp() {
    val sessionVm: SessionViewModel = hiltViewModel()
    val token by sessionVm.token.collectAsStateWithLifecycle(initialValue = null)
    val initialized by sessionVm.initialized.collectAsStateWithLifecycle(initialValue = false)

    if (!initialized) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val nav = rememberNavController()
    val startDest = if (token.isNullOrBlank()) "auth" else "main"
    NavHost(navController = nav, startDestination = startDest) {
        composable("auth") { AuthFlow(nav) }
        composable("main") { MainShell(nav) }
        composable("chat/{cid}") { backStack ->
            val cid = backStack.arguments?.getString("cid")
            ChatScreen(conversationId = cid, onBack = { nav.popBackStack() })
        }
        composable("voice") { VoiceScreen(onBack = { nav.popBackStack() }) }
        composable("image") { ImageScreen(onBack = { nav.popBackStack() }) }
        composable("search") { SearchScreen(onBack = { nav.popBackStack() }) }
    }
}

@Composable
private fun AuthFlow(nav: NavHostController) {
    val authNav = rememberNavController()
    NavHost(authNav, startDestination = "login") {
        composable("login") {
            LoginScreen(
                onLoginSuccess = { nav.navigate("main") { popUpTo("auth") { inclusive = true } } },
                onGoRegister = { authNav.navigate("register") },
            )
        }
        composable("register") {
            RegisterScreen(
                onRegisterSuccess = { nav.navigate("main") { popUpTo("auth") { inclusive = true } } },
                onBack = { authNav.popBackStack() },
            )
        }
    }
}

private data class Tab(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

private val TABS = listOf(
    Tab("home", "Home", Icons.Default.Home),
    Tab("chat", "Chat", Icons.Default.Chat),
    Tab("explore", "Explore", Icons.Default.Explore),
    Tab("connectors", "Connectors", Icons.Default.Extension),
    Tab("profile", "Profile", Icons.Default.Person),
)

@Composable
private fun MainShell(rootNav: NavHostController) {
    val innerNav = rememberNavController()
    val backStack by innerNav.currentBackStackEntryAsState()
    val current = backStack?.destination?.route

    Scaffold(
        bottomBar = {
            NavigationBar {
                TABS.forEach { tab ->
                    NavigationBarItem(
                        selected = current == tab.route,
                        onClick = {
                            if (current != tab.route) {
                                innerNav.navigate(tab.route) {
                                    popUpTo(innerNav.graph.startDestinationId) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) },
                    )
                }
            }
        }
    ) { pad ->
        NavHost(innerNav, startDestination = "home", modifier = Modifier.padding(pad)) {
            composable("home") { HomeScreen(onOpenChat = { cid -> rootNav.navigate("chat/$cid") }, onOpenVoice = { rootNav.navigate("voice") }, onOpenImage = { rootNav.navigate("image") }, onOpenSearch = { rootNav.navigate("search") }) }
            composable("chat") { ChatScreen(conversationId = null, onBack = null) }
            composable("explore") { ExploreScreen(onOpenImage = { rootNav.navigate("image") }, onOpenSearch = { rootNav.navigate("search") }, onOpenVoice = { rootNav.navigate("voice") }, onOpenChat = { rootNav.navigate("chat/") }) }
            composable("connectors") { ConnectorsScreen() }
            composable("profile") { ProfileScreen(onLogout = { rootNav.navigate("auth") { popUpTo("main") { inclusive = true } } }) }
        }
    }
}
