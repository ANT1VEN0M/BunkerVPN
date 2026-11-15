package com.v2ray.ang.ui.compose

import android.app.Activity
import android.graphics.BitmapFactory
import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.v2ray.ang.AppConfig
import com.v2ray.ang.handler.MmkvManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

private val LightColor = lightColorScheme(
    primary = Color(0xFF000000), // Black
    onPrimary = Color(0xFFFFFFFF), // White
    primaryContainer = Color(0xFFE0E0E0), // Light Gray
    onPrimaryContainer = Color(0xFF000000), // Black
    secondary = Color(0xFFf97910), // Orange
    onSecondary = Color(0xFFFFFFFF), // White
    secondaryContainer = Color(0xFFFFE8D6), // Pale Orange
    onSecondaryContainer = Color(0xFF2B1700), // Dark Brown
    tertiary = Color(0xFF009966), // Green
    onTertiary = Color(0xFFFFFFFF), // White
    tertiaryContainer = Color(0xFFA0F2D0), // Light Green
    onTertiaryContainer = Color(0xFF00201A), // Dark Teal
    error = Color(0xFFBA1A1A), // Red
    errorContainer = Color(0xFFFFDAD6), // Light Red
    onError = Color(0xFFFFFFFF), // White
    onErrorContainer = Color(0xFF410002), // Dark Red
    background = Color(0xFFFFFFFF), // White
    onBackground = Color(0xFF1C1B1F), // Near Black
    surface = Color(0xFFFFFFFF), // White
    onSurface = Color(0xFF1C1B1F), // Near Black
    surfaceVariant = Color(0xFFE7E0EC), // Light Purple Gray
    onSurfaceVariant = Color(0xFF49454F), // Dark Gray
    outline = Color(0xFF79747E), // Medium Gray
    outlineVariant = Color(0xFFCAC4D0), // Light Gray
    inverseSurface = Color(0xFF313033), // Dark Gray
    inverseOnSurface = Color(0xFFF4EFF4), // Very Light Gray
    inversePrimary = Color(0xFFC0C0C0), // Silver Gray
    scrim = Color(0xFF000000), // Black
    surfaceTint = Color(0xFF000000), // Black
    surfaceContainerLowest = Color(0xFFFFFFFF), // White
    surfaceContainerLow = Color(0xFFF7F7F7), // Very Light Gray
    surfaceContainer = Color(0xFFF1F1F1), // Light Gray
    surfaceContainerHigh = Color(0xFFEBEBEB), // Light Gray
    surfaceContainerHighest = Color(0xFFE5E5E5), // Light Gray
)

private val DarkColor = darkColorScheme(
    primary = Color(0xFFA855F7), // Neon Cyber Purple
    onPrimary = Color(0xFF0B0912), // Deep Dark AMOLED
    primaryContainer = Color(0xFF3B185F), // Deep Purple Container
    onPrimaryContainer = Color(0xFFF3E8FF), // Soft Purple White
    secondary = Color(0xFFC084FC), // Bright Neon Violet
    onSecondary = Color(0xFF2E1065), // Deep Violet
    secondaryContainer = Color(0xFF28183E), // Dark Purple Card
    onSecondaryContainer = Color(0xFFF3E8FF), // Light Purple Text
    tertiary = Color(0xFF818CF8), // Electric Indigo
    onTertiary = Color(0xFF1E1B4B), // Dark Indigo
    tertiaryContainer = Color(0xFF312E81), // Deep Indigo Container
    onTertiaryContainer = Color(0xFFE0E7FF), // Soft Blue
    error = Color(0xFFFFB4AB), // Light Red
    errorContainer = Color(0xFF93000A), // Dark Red
    onError = Color(0xFF690005), // Deep Red
    onErrorContainer = Color(0xFFFFDAD6), // Light Red
    background = Color(0xFF0B0912), // True AMOLED Black with Purple Haze
    onBackground = Color(0xFFF1EEF7), // Off-white
    surface = Color(0xFF120F1D), // Dark Purple Slate Surface
    onSurface = Color(0xFFF1EEF7), // Off-white
    surfaceVariant = Color(0xFF201B30), // Elevated Card Surface
    onSurfaceVariant = Color(0xFFD6CEE5), // Muted Purple-Gray
    outline = Color(0xFF6B5C82), // Purple Outline
    outlineVariant = Color(0xFF332B45), // Dark Purple Outline
    inverseSurface = Color(0xFFF1EEF7),
    inverseOnSurface = Color(0xFF0B0912),
    inversePrimary = Color(0xFF7C3AED),
    scrim = Color(0xFF000000),
    surfaceTint = Color(0xFFA855F7), // Neon Purple Tint
    surfaceContainerLowest = Color(0xFF08060E),
    surfaceContainerLow = Color(0xFF100D1A),
    surfaceContainer = Color(0xFF161224),
    surfaceContainerHigh = Color(0xFF1E1930),
    surfaceContainerHighest = Color(0xFF27213E),
)

// Semantic Colors
val colorPing = Color(0xFF009966) // Green
val colorPingRed = Color(0xFFFF0099) // Pink Red
val colorConfigType = Color(0xFFf97910) // Orange
val colorFabActive = Color(0xFFf97910) // Orange
val colorFabInactiveLight = Color(0xFF9C9C9C) // Gray
val colorFabInactiveDark = Color(0xFF646464) // Dark Gray
val dividerColorLight = Color(0xFFE0E0E0) // Light Gray
val dividerColorDark = Color(0xFF424242) // Dark Gray

// Toast Colors 85%
val toastNormalBgLight = Color(0xD9353A3E) // Dark Gray
val toastNormalBgDark = Color(0xD94A4F54) // Darker Gray
val toastSuccessBg = Color(0xD9388E3C) // Green
val toastErrorBg = Color(0xD9D50000) // Red
val toastInfoBg = Color(0xD93F51B5) // Indigo Blue
val toastIconCircleBg = Color(0x33FFFFFF) // Semi-transparent White
val toastTextColor = Color.White // White

object ThemeManager {
    private val _themeMode = MutableStateFlow(
        MmkvManager.decodeSettingsString(AppConfig.PREF_UI_MODE_NIGHT, "0") ?: "0"
    )
    val themeMode: StateFlow<String> = _themeMode.asStateFlow()

    private val _dynamicColorEnabled = MutableStateFlow(
        MmkvManager.decodeSettingsBool(AppConfig.PREF_DYNAMIC_COLOR, true)
    )
    val dynamicColorEnabled: StateFlow<Boolean> = _dynamicColorEnabled.asStateFlow()

    private val _customWallpaper = MutableStateFlow(
        MmkvManager.decodeSettingsString(AppConfig.PREF_CUSTOM_WALLPAPER)?.takeIf { it.isNotBlank() }
    )
    val customWallpaper: StateFlow<String?> = _customWallpaper.asStateFlow()

    fun setThemeMode(mode: String) {
        MmkvManager.encodeSettings(AppConfig.PREF_UI_MODE_NIGHT, mode)
        _themeMode.value = mode
    }

    fun setDynamicColorEnabled(enabled: Boolean) {
        MmkvManager.encodeSettings(AppConfig.PREF_DYNAMIC_COLOR, enabled)
        _dynamicColorEnabled.value = enabled
    }

    fun setCustomWallpaper(path: String?) {
        MmkvManager.encodeSettings(AppConfig.PREF_CUSTOM_WALLPAPER, path ?: "")
        _customWallpaper.value = path?.takeIf { it.isNotBlank() }
    }

    fun refresh() {
        _themeMode.value =
            MmkvManager.decodeSettingsString(AppConfig.PREF_UI_MODE_NIGHT, "0") ?: "0"
        _dynamicColorEnabled.value =
            MmkvManager.decodeSettingsBool(AppConfig.PREF_DYNAMIC_COLOR, true)
        _customWallpaper.value =
            MmkvManager.decodeSettingsString(AppConfig.PREF_CUSTOM_WALLPAPER)?.takeIf { it.isNotBlank() }
    }
}

@Composable
fun WallpaperBackground(wallpaperPath: String) {
    val bitmap = remember(wallpaperPath) {
        try {
            val file = File(wallpaperPath)
            if (file.exists() && file.length() > 0) {
                BitmapFactory.decodeFile(file.absolutePath)?.asImageBitmap()
            } else null
        } catch (e: Exception) {
            null
        }
    }
    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        // Dark translucent AMOLED scrim for high contrast readability
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xB30B0912))
        )
    }
}

@Composable
fun resolveDarkTheme(): Boolean {
    val mode by ThemeManager.themeMode.collectAsState()
    return when (mode) {
        "1" -> false
        "2" -> true
        else -> isSystemInDarkTheme()
    }
}

val LocalDarkTheme = compositionLocalOf { false }

@Composable
fun AppTheme(
    darkTheme: Boolean = resolveDarkTheme(),
    content: @Composable () -> Unit
) {
    val dynamicColor by ThemeManager.dynamicColorEnabled.collectAsState()
    val wallpaperPath by ThemeManager.customWallpaper.collectAsState()
    val hasWallpaper = !wallpaperPath.isNullOrBlank()

    val context = LocalContext.current
    val baseColorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColor
        else -> LightColor
    }

    val colorScheme = if (hasWallpaper) {
        baseColorScheme.copy(
            background = Color.Transparent,
            surface = if (darkTheme) Color(0xCC120F1D) else Color(0xCCFFFFFF),
            surfaceVariant = if (darkTheme) Color(0xD9201B30) else Color(0xD9E7E0EC),
            surfaceContainer = if (darkTheme) Color(0xCC161224) else Color(0xCCF1F1F1)
        )
    } else {
        baseColorScheme
    }

    val snackbarController = rememberAppSnackbarController()

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val activity = view.context as? Activity ?: return@SideEffect
            val window = activity.window
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    CompositionLocalProvider(
        LocalDarkTheme provides darkTheme,
        LocalAppSnackbar provides snackbarController
    ) {
        MaterialTheme(
            colorScheme = colorScheme
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                if (hasWallpaper && wallpaperPath != null) {
                    WallpaperBackground(wallpaperPath = wallpaperPath!!)
                }
                AppSnackbarBridge(controller = snackbarController)
                content()
                AppSnackbarHost(hostState = snackbarController.hostState)
            }
        }
    }
}
