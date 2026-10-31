package com.mandu.ai.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import com.mandu.ai.core.data.local.SessionStore
import dagger.hilt.android.EntryPointAccessors
import androidx.compose.ui.platform.LocalContext
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

private val Purple80 = Color(0xFFD0BCFF)
private val PurpleGrey80 = Color(0xFFCCC2DC)
private val Pink80 = Color(0xFFEFB8C8)

private val Purple40 = Color(0xFF6650a4)
private val PurpleGrey40 = Color(0xFF625b71)
private val Pink40 = Color(0xFF7D5260)

private val DarkColors = darkColorScheme(
    primary = Purple80, secondary = PurpleGrey80, tertiary = Pink80,
    background = Color(0xFF0F0A1F), surface = Color(0xFF171227),
    onBackground = Color(0xFFEDE7FF), onSurface = Color(0xFFEDE7FF),
)

private val LightColors = lightColorScheme(
    primary = Purple40, secondary = PurpleGrey40, tertiary = Pink40,
    background = Color(0xFFFDFBFF), surface = Color(0xFFFFFFFF),
)

@EntryPoint
@InstallIn(SingletonComponent::class)
interface SessionEntryPoint { fun sessionStore(): SessionStore }

@Composable
fun ManduTheme(content: @Composable () -> Unit) {
    val ctx = LocalContext.current
    val store = EntryPointAccessors.fromApplication(ctx.applicationContext, SessionEntryPoint::class.java).sessionStore()
    val darkPref by store.darkTheme.collectAsState(initial = isSystemInDarkTheme())
    val colors = if (darkPref) DarkColors else LightColors

    MaterialTheme(colorScheme = colors, typography = MaterialTheme.typography, content = content)
}
