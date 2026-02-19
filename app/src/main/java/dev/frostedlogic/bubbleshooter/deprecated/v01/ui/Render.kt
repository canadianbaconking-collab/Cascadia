package dev.frostedlogic.bubbleshooter.deprecated.v01.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import dev.frostedlogic.bubbleshooter.deprecated.v01.core.BoonId
import dev.frostedlogic.bubbleshooter.deprecated.v01.core.BubbleColor
import dev.frostedlogic.bubbleshooter.deprecated.v01.core.GameState
import dev.frostedlogic.bubbleshooter.deprecated.v01.core.Grid

fun renderGame(canvas: DrawScope, state: GameState, aimAngleRad: Float) {
    with(canvas) {
        val bubbleSize = size.width / (state.grid.cols + 1)
        val topPad = 40f
        state.grid.cells.forEachIndexed { i, code ->
            if (code == Grid.EMPTY) return@forEachIndexed
            val r = i / state.grid.cols
            val c = i % state.grid.cols
            val x = c * bubbleSize + if (r % 2 == 0) bubbleSize * 0.5f else bubbleSize
            val y = topPad + r * bubbleSize
            drawCircle(colorFor(code), radius = bubbleSize * 0.42f, center = Offset(x, y))
            if (code == Grid.STONE) {
                drawCircle(Color.Black.copy(alpha = 0.45f), radius = bubbleSize * 0.2f, center = Offset(x, y))
            }
        }

        val shooter = Offset(size.width / 2f, size.height - bubbleSize)
        drawCircle(colorForBubble(state.currentBubble.color), radius = bubbleSize * 0.45f, center = shooter)
        drawCircle(colorForBubble(state.nextBubble.color), radius = bubbleSize * 0.28f, center = Offset(size.width - bubbleSize, size.height - bubbleSize))

        val showAim = !state.boss.fogActive || state.boons.contains(BoonId.Sights)
        if (showAim) {
            val len = size.height * 0.5f
            val end = Offset(
                shooter.x + kotlin.math.sin(aimAngleRad) * len,
                shooter.y - kotlin.math.cos(aimAngleRad) * len
            )
            drawLine(Color.White, shooter, end, strokeWidth = 4f)
            if (state.boons.contains(BoonId.Sights)) {
                val bounce = Offset(end.x.coerceIn(0f, size.width), end.y)
                val second = Offset(bounce.x - (end.x - shooter.x) * 0.45f, (bounce.y - (end.y - shooter.y) * 0.45f))
                drawLine(Color.White.copy(alpha = 0.55f), bounce, second, strokeWidth = 3f)
            }
            drawCircle(Color.White.copy(alpha = 0.2f), radius = 8f, center = end, style = Stroke(2f))
        }

        drawRect(Color(0x22000000), topLeft = Offset.Zero, size = Size(size.width, 30f))
    }
}

private fun colorFor(code: Int): Color = when (code) {
    Grid.RED -> Color(0xFFD84A4A)
    Grid.GREEN -> Color(0xFF4CAF50)
    Grid.BLUE -> Color(0xFF4A90E2)
    Grid.GOAL -> Color(0xFFFFC107)
    Grid.YELLOW -> Color(0xFFFFEB3B)
    Grid.PURPLE -> Color(0xFF9C27B0)
    Grid.WILD -> Color(0xFFFFFFFF)
    Grid.STONE -> Color(0xFF8D8D8D)
    else -> Color.Transparent
}

private fun colorForBubble(color: BubbleColor): Color = when (color) {
    BubbleColor.Red -> Color(0xFFD84A4A)
    BubbleColor.Green -> Color(0xFF4CAF50)
    BubbleColor.Blue -> Color(0xFF4A90E2)
    BubbleColor.Goal -> Color(0xFFFFC107)
    BubbleColor.Yellow -> Color(0xFFFFEB3B)
    BubbleColor.Purple -> Color(0xFF9C27B0)
    BubbleColor.Wild -> Color.White
}
