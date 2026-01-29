package com.cmm.certificates.core.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// Brand-ish seed: keep your blue vibe, but map it into M3 roles consistently.
private val PrimaryLight = Color(0xFF1D4ED8)          // ~blue-700
private val OnPrimaryLight = Color(0xFFFFFFFF)
private val PrimaryContainerLight = Color(0xFFDCE3FF) // light tinted container
private val OnPrimaryContainerLight = Color(0xFF0B1B5A)

private val SecondaryLight = Color(0xFF00639A)        // harmonized blue-cyan (less neon)
private val OnSecondaryLight = Color(0xFFFFFFFF)
private val SecondaryContainerLight = Color(0xFFCDE5FF)
private val OnSecondaryContainerLight = Color(0xFF001D32)

private val TertiaryLight = Color(0xFF5B5D72)         // neutral-ish tertiary for chips/accents
private val OnTertiaryLight = Color(0xFFFFFFFF)
private val TertiaryContainerLight = Color(0xFFE1E0F9)
private val OnTertiaryContainerLight = Color(0xFF181A2C)

// Neutral surfaces (M3 prefers neutrals for background/surface; tint comes from containers)
private val BackgroundLight = Color(0xFFFCFCFF)
private val OnBackgroundLight = Color(0xFF1A1B20)
private val SurfaceLight = Color(0xFFFCFCFF)
private val OnSurfaceLight = Color(0xFF1A1B20)
private val SurfaceVariantLight = Color(0xFFE2E1EC)
private val OnSurfaceVariantLight = Color(0xFF45464F)
private val OutlineLight = Color(0xFF767680)
private val OutlineVariantLight = Color(0xFFC6C5D0)

private val ErrorLight = Color(0xFFB3261E)
private val OnErrorLight = Color(0xFFFFFFFF)
private val ErrorContainerLight = Color(0xFFF9DEDC)
private val OnErrorContainerLight = Color(0xFF410E0B)

internal val LightColorScheme = lightColorScheme(
    primary = PrimaryLight,
    onPrimary = OnPrimaryLight,
    primaryContainer = PrimaryContainerLight,
    onPrimaryContainer = OnPrimaryContainerLight,

    secondary = SecondaryLight,
    onSecondary = OnSecondaryLight,
    secondaryContainer = SecondaryContainerLight,
    onSecondaryContainer = OnSecondaryContainerLight,

    tertiary = TertiaryLight,
    onTertiary = OnTertiaryLight,
    tertiaryContainer = TertiaryContainerLight,
    onTertiaryContainer = OnTertiaryContainerLight,

    background = BackgroundLight,
    onBackground = OnBackgroundLight,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceVariantLight,

    error = ErrorLight,
    onError = OnErrorLight,
    errorContainer = ErrorContainerLight,
    onErrorContainer = OnErrorContainerLight,

    outline = OutlineLight,
    outlineVariant = OutlineVariantLight,
)

// Dark: keep primary readable, push surfaces to neutral-dark, containers slightly tinted
private val PrimaryDark = Color(0xFFB6C4FF)
private val OnPrimaryDark = Color(0xFF001A6A)
private val PrimaryContainerDark = Color(0xFF00309D)
private val OnPrimaryContainerDark = Color(0xFFDCE3FF)

private val SecondaryDark = Color(0xFF95CCFF)
private val OnSecondaryDark = Color(0xFF003351)
private val SecondaryContainerDark = Color(0xFF004A73)
private val OnSecondaryContainerDark = Color(0xFFCDE5FF)

private val TertiaryDark = Color(0xFFC3C3DD)
private val OnTertiaryDark = Color(0xFF2C2E42)
private val TertiaryContainerDark = Color(0xFF424559)
private val OnTertiaryContainerDark = Color(0xFFE1E0F9)

private val BackgroundDark = Color(0xFF121318)
private val OnBackgroundDark = Color(0xFFE3E1E9)
private val SurfaceDark = Color(0xFF121318)
private val OnSurfaceDark = Color(0xFFE3E1E9)
private val SurfaceVariantDark = Color(0xFF45464F)
private val OnSurfaceVariantDark = Color(0xFFC6C5D0)
private val OutlineDark = Color(0xFF90909A)
private val OutlineVariantDark = Color(0xFF45464F)

private val ErrorDark = Color(0xFFF2B8B5)
private val OnErrorDark = Color(0xFF601410)
private val ErrorContainerDark = Color(0xFF8C1D18)
private val OnErrorContainerDark = Color(0xFFF9DEDC)

internal val DarkColorScheme = darkColorScheme(
    primary = PrimaryDark,
    onPrimary = OnPrimaryDark,
    primaryContainer = PrimaryContainerDark,
    onPrimaryContainer = OnPrimaryContainerDark,

    secondary = SecondaryDark,
    onSecondary = OnSecondaryDark,
    secondaryContainer = SecondaryContainerDark,
    onSecondaryContainer = OnSecondaryContainerDark,

    tertiary = TertiaryDark,
    onTertiary = OnTertiaryDark,
    tertiaryContainer = TertiaryContainerDark,
    onTertiaryContainer = OnTertiaryContainerDark,

    background = BackgroundDark,
    onBackground = OnBackgroundDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantDark,

    error = ErrorDark,
    onError = OnErrorDark,
    errorContainer = ErrorContainerDark,
    onErrorContainer = OnErrorContainerDark,

    outline = OutlineDark,
    outlineVariant = OutlineVariantDark,
)
