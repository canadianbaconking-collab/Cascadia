package dev.frostedlogic.bubbleshooter.ui

import androidx.compose.ui.geometry.Offset

fun normalizeX(position: Offset, width: Float): Float = if (width <= 0f) 0.5f else (position.x / width).coerceIn(0f, 1f)
