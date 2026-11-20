package com.v2ray.ang.ui.compose

import android.app.Activity
import android.graphics.BitmapFactory
import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ColorScheme
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
import com.v2ray.ang.R
import com.v2ray.ang.handler.MmkvManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

/**
 * Bunker VPN Cyber Theme Palettes
 */
enum class BunkerThemePalette(
    val id: String,
    val titleRes: Int,
    val title: String,
    val primaryColor: Color,
    val secondaryColor: Color,
    val tertiaryColor: Color,
    val backgroundColor: Color,
    val surfaceColor: Color,
    val cardColor: Color,
    val accentActiveColor: Color
) {
    CYBER_VIOLET(
        id = "cyber_violet",
        titleRes = R.string.theme_cyber_purple,
        title = "🔮 Cyber Purple",
        primaryColor = Color(0xFFA855F7),
        secondaryColor = Color(0xFFC084FC),
        tertiaryColor = Color(0xFF818CF8),
        backgroundColor = Color(0xFF0B0912),
        surfaceColor = Color(0xFF120F1D),
        cardColor = Color(0xFF201B30),
        accentActiveColor = Color(0xFFA855F7)
    ),
    EMERALD_MATRIX(
        id = "emerald_matrix",
        titleRes = R.string.theme_emerald_matrix,
        title = "⚡ Emerald Matrix",
        primaryColor = Color(0xFF10B981),
        secondaryColor = Color(0xFF34D399),
        tertiaryColor = Color(0xFF6EE7B7),
        backgroundColor = Color(0xFF041009),
        surfaceColor = Color(0xFF081C10),
        cardColor = Color(0xFF0E2E1B),
        accentActiveColor = Color(0xFF10B981)
    ),
    ARCTIC_CYAN(
        id = "arctic_cyan",
        titleRes = R.string.theme_arctic_cyan,
        title = "❄️ Arctic Cyan",
        primaryColor = Color(0xFF06B6D4),
        secondaryColor = Color(0xFF38BDF8),
        tertiaryColor = Color(0xFF7DD3FC),
        backgroundColor = Color(0xFF050E17),
        surfaceColor = Color(0xFF0A1826),
        cardColor = Color(0xFF10273D),
        accentActiveColor = Color(0xFF06B6D4)
    ),
    CYBER_AMBER(
        id = "cyber_amber",
        titleRes = R.string.theme_amber_neon,
        title = "🌇 Amber Neon",
        primaryColor = Color(0xFFF59E0B),
        secondaryColor = Color(0xFFFBBF24),
        tertiaryColor = Color(0xFFFCD34D),
        backgroundColor = Color(0xFF100C04),
        surfaceColor = Color(0xFF1B1408),
        cardColor = Color(0xFF2B200D),
        accentActiveColor = Color(0xFFF59E0B)
    ),
    CRIMSON_RED(
        id = "crimson_red",
        titleRes = R.string.theme_blood_ruby,
        title = "🩸 Blood Ruby",
        primaryColor = Color(0xFFEF4444),
        secondaryColor = Color(0xFFF87171),
        tertiaryColor = Color(0xFFFCA5A5),
        backgroundColor = Color(0xFF120606),
        surfaceColor = Color(0xFF1C0A0A),
        cardColor = Color(0xFF2E1212),
        accentActiveColor = Color(0xFFEF4444)
    ),
    STEALTH_MONO(
        id = "stealth_mono",
        titleRes = R.string.theme_black_titan,
        title = "🖤 Black Titan",
        primaryColor = Color(0xFF94A3B8),
        secondaryColor = Color(0xFFCBD5E1),
        tertiaryColor = Color(0xFFE2E8F0),
        backgroundColor = Color(0xFF000000),
        surfaceColor = Color(0xFF0C0E12),
        cardColor = Color(0xFF161A22),
        accentActiveColor = Color(0xFFCBD5E1)
    );

    val notifColorInt: Int
        get() = when (this) {
            CYBER_VIOLET -> 0xFF7C3AED.toInt()
            EMERALD_MATRIX -> 0xFF10B981.toInt()
            ARCTIC_CYAN -> 0xFF06B6D4.toInt()
            CYBER_AMBER -> 0xFFF59E0B.toInt()
            CRIMSON_RED -> 0xFFEF4444.toInt()
            STEALTH_MONO -> 0xFF94A3B8.toInt()
        }

    companion object {
        fun fromId(id: String?): BunkerThemePalette {
            return entries.find { it.id.equals(id, ignoreCase = true) } ?: CYBER_VIOLET
        }
    }
}

val LocalThemePalette = compositionLocalOf { BunkerThemePalette.CYBER_VIOLET }

fun buildDarkColorScheme(palette: BunkerThemePalette): ColorScheme {
    return darkColorScheme(
        primary = palette.primaryColor,
        onPrimary = palette.backgroundColor,
        primaryContainer = palette.cardColor,
        onPrimaryContainer = Color.White,
        secondary = palette.secondaryColor,
        onSecondary = palette.backgroundColor,
        secondaryContainer = palette.surfaceColor,
        onSecondaryContainer = Color.White,
        tertiary = palette.tertiaryColor,
        onTertiary = palette.backgroundColor,
        tertiaryContainer = palette.cardColor,
        onTertiaryContainer = Color.White,
        error = Color(0xFFFFB4AB),
        errorContainer = Color(0xFF93000A),
        onError = Color(0xFF690005),
        onErrorContainer = Color(0xFFFFDAD6),
        background = palette.backgroundColor,
        onBackground = Color(0xFFF1EEF7),
        surface = palette.surfaceColor,
        onSurface = Color(0xFFF1EEF7),
        surfaceVariant = palette.cardColor,
        onSurfaceVariant = Color(0xFFD6CEE5),
        outline = palette.primaryColor.copy(alpha = 0.45f),
        outlineVariant = palette.primaryColor.copy(alpha = 0.2f),
        inverseSurface = Color(0xFFF1EEF7),
        inverseOnSurface = palette.backgroundColor,
        inversePrimary = palette.secondaryColor,
        scrim = Color(0xFF000000),
        surfaceTint = palette.primaryColor,
        surfaceContainerLowest = palette.backgroundColor,
        surfaceContainerLow = palette.surfaceColor,
        surfaceContainer = palette.surfaceColor,
        surfaceContainerHigh = palette.cardColor,
        surfaceContainerHighest = palette.cardColor,
    )
}

fun buildLightColorScheme(palette: BunkerThemePalette): ColorScheme {
    return lightColorScheme(
        primary = palette.primaryColor,
        onPrimary = Color.White,
        primaryContainer = palette.primaryColor.copy(alpha = 0.15f),
        onPrimaryContainer = Color.Black,
        secondary = palette.secondaryColor,
        onSecondary = Color.White,
        secondaryContainer = palette.secondaryColor.copy(alpha = 0.15f),
        onSecondaryContainer = Color.Black,
        tertiary = palette.tertiaryColor,
        onTertiary = Color.White,
        tertiaryContainer = palette.tertiaryColor.copy(alpha = 0.15f),
        onTertiaryContainer = Color.Black,
        error = Color(0xFFBA1A1A),
        errorContainer = Color(0xFFFFDAD6),
        onError = Color.White,
        onErrorContainer = Color(0xFF410002),
        background = Color(0xFFF8FAFC),
        onBackground = Color(0xFF1E293B),
        surface = Color.White,
        onSurface = Color(0xFF1E293B),
        surfaceVariant = Color(0xFFF1F5F9),
        onSurfaceVariant = Color(0xFF475569),
        outline = palette.primaryColor.copy(alpha = 0.35f),
        outlineVariant = Color(0xFFCBD5E1),
        inverseSurface = Color(0xFF1E293B),
        inverseOnSurface = Color.White,
        inversePrimary = palette.primaryColor,
        scrim = Color.Black,
        surfaceTint = palette.primaryColor,
        surfaceContainerLowest = Color.White,
        surfaceContainerLow = Color(0xFFF8FAFC),
        surfaceContainer = Color(0xFFF1F5F9),
        surfaceContainerHigh = Color(0xFFE2E8F0),
        surfaceContainerHighest = Color(0xFFCBD5E1),
    )
}

// Semantic Colors
val colorPing = Color(0xFF009966) // Green
val colorPingRed = Color(0xFFFF0099) // Pink Red
val colorConfigType = Color(0xFFf97910) // Orange
val colorFabActive: Color
    @Composable
    get() = LocalThemePalette.current.accentActiveColor

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
        MmkvManager.decodeSettingsBool(AppConfig.PREF_DYNAMIC_COLOR, false)
    )
    val dynamicColorEnabled: StateFlow<Boolean> = _dynamicColorEnabled.asStateFlow()

    private val _themePalette = MutableStateFlow(
        BunkerThemePalette.fromId(
            MmkvManager.decodeSettingsString(AppConfig.PREF_THEME_PALETTE, AppConfig.DEFAULT_THEME_PALETTE)
        )
    )
    val themePalette: StateFlow<BunkerThemePalette> = _themePalette.asStateFlow()

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

    fun setThemePalette(palette: BunkerThemePalette) {
        MmkvManager.encodeSettings(AppConfig.PREF_THEME_PALETTE, palette.id)
        // Explicit user choice disables dynamic wallpaper override
        if (_dynamicColorEnabled.value) {
            setDynamicColorEnabled(false)
        }
        _themePalette.value = palette
    }

    fun setCustomWallpaper(path: String?) {
        MmkvManager.encodeSettings(AppConfig.PREF_CUSTOM_WALLPAPER, path ?: "")
        _customWallpaper.value = path?.takeIf { it.isNotBlank() }
    }

    fun refresh() {
        _themeMode.value =
            MmkvManager.decodeSettingsString(AppConfig.PREF_UI_MODE_NIGHT, "0") ?: "0"
        _dynamicColorEnabled.value =
            MmkvManager.decodeSettingsBool(AppConfig.PREF_DYNAMIC_COLOR, false)
        _themePalette.value =
            BunkerThemePalette.fromId(
                MmkvManager.decodeSettingsString(AppConfig.PREF_THEME_PALETTE, AppConfig.DEFAULT_THEME_PALETTE)
            )
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
    val currentPalette by ThemeManager.themePalette.collectAsState()
    val wallpaperPath by ThemeManager.customWallpaper.collectAsState()
    val hasWallpaper = !wallpaperPath.isNullOrBlank()

    val context = LocalContext.current
    val baseColorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> buildDarkColorScheme(currentPalette)
        else -> buildLightColorScheme(currentPalette)
    }

    val colorScheme = if (hasWallpaper) {
        baseColorScheme.copy(
            background = Color.Transparent,
            surface = if (darkTheme) currentPalette.surfaceColor.copy(alpha = 0.8f) else Color(0xCCFFFFFF),
            surfaceVariant = if (darkTheme) currentPalette.cardColor.copy(alpha = 0.85f) else Color(0xD9E7E0EC),
            surfaceContainer = if (darkTheme) currentPalette.surfaceColor.copy(alpha = 0.8f) else Color(0xCCF1F1F1)
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
        LocalThemePalette provides currentPalette,
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
