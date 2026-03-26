package com.github.im.group.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

object ThemeTokens {
    // Core Colors
    val BackgroundDark = Color(0xFF0C111B)
    val GlassWhite = Color(0xF2FFFFFF) // 0.95 opacity
    val GlassBorder = Color(0x4DFFFFFF) // 0.3 opacity
    
    // Gradients
    val PrimaryBlue = Color(0xFF3B82F6)
    val PrimaryBlueEnd = Color(0xFF2563EB)
    
    val PrimaryGradient = Brush.linearGradient(
        colors = listOf(PrimaryBlue, PrimaryBlueEnd)
    )
    
    // Animated Sphere Colors
    val Sphere1 = Color(0xFF3B82F6)
    val Sphere2 = Color(0xFF8B5CF6)
    val Sphere3 = Color(0xFFEC4899)
    
    // Text Colors
    val TextMain = Color(0xFF111827)
    val TextSecondary = Color(0xFF6B7280)
    val TextMuted = Color(0xFF94A3B8)
    val TextOnPrimary = Color.White
    
    // UI Elements
    val InputBorder = Color(0xFFE5E7EB)
    val InputFocus = Color(0xFF3B82F6)
}
