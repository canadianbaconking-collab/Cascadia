package dev.frostedlogic.bubbleshooter.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import dev.frostedlogic.bubbleshooter.core.GameState
import dev.frostedlogic.bubbleshooter.core.World

fun renderGame(scope: DrawScope, state: GameState) = with(scope) {
    drawRect(Color(0xFF101626), size = size)

    val pX = state.player.x * size.width
    val pY = state.player.y * size.height
    drawCircle(Color(0xFF6EE7FF), radius = World.PLAYER_RADIUS * size.width, center = Offset(pX, pY))

    if (state.projectile.active) {
        val x = state.projectile.x * size.width
        val y = state.projectile.y * size.height
        drawLine(Color.White, Offset(x, y), Offset(x, y - 30f), strokeWidth = 4f)
    }

    state.balls.forEach { ball ->
        val alpha = if (state.roomIndex == 8) 0.55f else 1f
        val color = when (ball.tier) {
            3 -> Color(0xFFFF6B6B)
            2 -> Color(0xFFFFD166)
            else -> Color(0xFF95D5B2)
        }.copy(alpha = alpha)
        drawCircle(
            color = color,
            radius = ball.radius * size.width,
            center = Offset(ball.x * size.width, ball.y * size.height)
        )
    }

    drawRect(
        color = Color.Transparent,
        topLeft = Offset.Zero,
        size = Size(size.width, size.height),
    )
}
