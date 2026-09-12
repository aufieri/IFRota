package br.edu.ifsp.ifrota.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

/**
 * Paleta derivada do protótipo IFRota no Figma. O app usa apenas o esquema claro:
 * o verde da marca é a identidade e o dynamic color do Android 12+ o substituiria.
 */
private val IFRotaColorScheme = lightColorScheme(
    primary = Green600,
    onPrimary = Surface1,
    primaryContainer = Green100,
    onPrimaryContainer = Green900,

    secondary = Green700,
    onSecondary = Surface1,
    secondaryContainer = Green50,
    onSecondaryContainer = Green900,

    tertiary = Amber500,
    onTertiary = Surface1,
    tertiaryContainer = Amber100,
    onTertiaryContainer = Amber800,

    background = Surface2,
    onBackground = Text1,
    surface = Surface1,
    onSurface = Text1,
    surfaceVariant = Surface3,
    onSurfaceVariant = Text2,

    outline = BorderSubtle,
    outlineVariant = Surface3,

    error = Red600,
    onError = Surface1,
    errorContainer = Red100,
    onErrorContainer = Red600
)

@Composable
fun IFRotaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = IFRotaColorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}
