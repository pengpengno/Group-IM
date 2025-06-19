package com.github.im.group.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.absoluteValue

@Composable
fun UserAvatar(
    username: String,
    modifier: Modifier = Modifier,
    size: Int = 48
) {
    val initials = getInitials(username)
    val backgroundColor = getColorFromName(username)

    Box(
        modifier = modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initials,
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
fun getInitials(name: String): String {
    return name.trim()
        .takeIf { it.isNotEmpty() }
        ?.let {
            if (it.first().isLetter()) it.first().uppercase()
            else it.first().toString()
        } ?: "?"
}

fun getColorFromName(name: String): Color {
    val colors = listOf(
        Color(0xFFEF5350), // Red
        Color(0xFFAB47BC), // Purple
        Color(0xFF5C6BC0), // Indigo
        Color(0xFF29B6F6), // Blue
        Color(0xFF66BB6A), // Green
        Color(0xFFFFA726), // Orange
        Color(0xFF8D6E63), // Brown
        Color(0xFF26C6DA), // Cyan
        Color(0xFFEC407A)  // Pink
    )
    val index = (name.hashCode().absoluteValue) % colors.size
    return colors[index]
}
