package dev.frostedlogic.bubbleshooter.ui

import androidx.compose.ui.geometry.Offset
import kotlin.math.atan2

fun computeAimAngle(origin: Offset, pointer: Offset): Float {
    val dx = pointer.x - origin.x
    val dy = origin.y - pointer.y
    return atan2(dx, dy)
}
