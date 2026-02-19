package dev.frostedlogic.bubbleshooter.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import dev.frostedlogic.bubbleshooter.core.BoonId
import dev.frostedlogic.bubbleshooter.core.BubbleColor
import dev.frostedlogic.bubbleshooter.core.GameState
import dev.frostedlogic.bubbleshooter.core.Grid

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
    else -> Color.Transparent
}

private fun colorForBubble(color: BubbleColor): Color = when (color) {
    BubbleColor.Red -> Color(0xFFD84A4A)
    BubbleColor.Green -> Color(0xFF4CAF50)
    BubbleColor.Blue -> Color(0xFF4A90E2)
    BubbleColor.Goal -> Color(0xFFFFC107)
}
