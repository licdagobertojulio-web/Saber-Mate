package co.sabermate.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = BluePrimaryDark,
    secondary = PencilYellow,
    tertiary = CorrectGreen,
    background = PaperDark,
    surface = SurfaceDark,
    onPrimary = SurfaceDark,
    onSecondary = PencilInk,
    onBackground = InkDark,
    onSurface = InkDark
)

private val LightColorScheme = lightColorScheme(
    primary = BluePrimary,
    secondary = PencilYellow,
    tertiary = CorrectGreen,
    background = PaperLight,
    surface = SurfaceLight,
    onPrimary = SurfaceLight,
    onSecondary = PencilInk,
    onBackground = InkLight,
    onSurface = InkLight
)

@Composable
fun SaberMateTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Keep distinctive Saber Mate branding by default
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
