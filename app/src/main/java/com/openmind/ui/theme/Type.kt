package com.openmind.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Usa fontes do sistema — sans-serif padrão Android é Roboto/Google Sans
val OpenMindTypography = Typography(
    displaySmall = TextStyle(
        fontWeight = FontWeight.W300,
        fontSize   = 36.sp,
        lineHeight = 44.sp,
        letterSpacing = (-0.5).sp,
    ),
    titleLarge = TextStyle(
        fontWeight = FontWeight.W600,
        fontSize   = 20.sp,
        lineHeight = 26.sp,
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.W500,
        fontSize   = 16.sp,
        lineHeight = 22.sp,
    ),
    bodyMedium = TextStyle(
        fontWeight = FontWeight.W400,
        fontSize   = 14.sp,
        lineHeight = 20.sp,
    ),
    bodySmall = TextStyle(
        fontWeight = FontWeight.W400,
        fontSize   = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.2.sp,
    ),
    labelSmall = TextStyle(
        fontWeight = FontWeight.W500,
        fontSize   = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.8.sp,
    ),
)
