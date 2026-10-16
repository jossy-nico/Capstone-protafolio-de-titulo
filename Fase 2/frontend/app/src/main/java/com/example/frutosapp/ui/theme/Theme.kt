package com.example.frutosapp.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = VerdePrincipal,
    onPrimary = Blanco,

    primaryContainer = VerdeSuave,
    onPrimaryContainer = VerdeOscuro,

    secondary = VerdeOscuro,
    onSecondary = Blanco,

    secondaryContainer = VerdeMuySuave,
    onSecondaryContainer = TextoPrincipal,

    background = FondoApp,
    onBackground = TextoPrincipal,

    surface = Blanco,
    onSurface = TextoPrincipal,

    surfaceVariant = FondoSuave,
    onSurfaceVariant = TextoSecundario,

    outline = BordeSuave,

    error = RojoError,
    onError = Color.White
)

@Composable
fun FrutosAppTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        content = content
    )
}