package com.sintesistudio.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val GreenScheme = lightColorScheme(
    primary = Color(0xFF1B5E20),
    onPrimary = Color.White,
    secondary = Color(0xFF2E7D32),
    surface = Color(0xFFF1F8E9),
    onSurface = Color(0xFF1B1B1B)
)

@Composable
fun SintesiStudioTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = GreenScheme, content = content)
}
