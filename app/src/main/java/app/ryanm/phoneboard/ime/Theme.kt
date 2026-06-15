import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

private val RedBg      = Color(0xFFC52727)
private val RedKey     = Color(0xFFD04E4E)
private val RedKeyDark = Color(0xFFCA3838)
private val RedPressed = Color(0xFFE59C9C)
private val RedWedge   = Color(0xFFEFC3C3)
private val RedAccent  = Color(0xFFF44336)

private val OnRed      = Color(0xFFFFFFFF)
private val OnLight    = Color(0xFF000000) // only used if you put light surfaces somewhere

val GboardRedDark: ColorScheme = darkColorScheme(
    // Core
    primary = RedAccent,
    onPrimary = OnRed,

    secondary = RedKeyDark,
    onSecondary = OnRed,

    tertiary = RedPressed,
    onTertiary = OnRed,

    // Background/surfaces (keyboard)
    background = RedBg,
    onBackground = OnRed,

    surface = RedKey,                 // default key surface
    onSurface = OnRed,

    surfaceVariant = RedKeyDark,      // darker keys/strips if you want
    onSurfaceVariant = OnRed,

    // “Containers” are super handy for pressed/wedge overlays
    primaryContainer = RedKey,
    onPrimaryContainer = OnRed,

    secondaryContainer = RedPressed,  // pressed key overlay / active key
    onSecondaryContainer = OnRed,

    tertiaryContainer = RedWedge,     // wedge highlight
    onTertiaryContainer = OnLight,    // wedge is light; black text can be more legible if needed

    // Borders / separators
    outline = RedKeyDark
)

val GboardRedLight: ColorScheme = lightColorScheme(
    // If you ever want a “light mode” variant, this keeps it coherent.
    // (Gboard’s red theme is effectively “dark UI with red surfaces”, so you may not need this.)

    primary = RedAccent,
    onPrimary = OnRed,

    secondary = RedKeyDark,
    onSecondary = OnRed,

    tertiary = RedPressed,
    onTertiary = OnRed,

    background = RedBg,
    onBackground = OnRed,

    surface = RedKey,
    onSurface = OnRed,

    surfaceVariant = RedKeyDark,
    onSurfaceVariant = OnRed,

    primaryContainer = RedKey,
    onPrimaryContainer = OnRed,

    secondaryContainer = RedPressed,
    onSecondaryContainer = OnRed,

    tertiaryContainer = RedWedge,
    onTertiaryContainer = OnLight,

    outline = RedKeyDark
)
