package dev.frostedlogic.bubbleshooter.core

import kotlin.math.hypot

object Physics {
    private const val GRAVITY = 1.35f

    fun radiusForTier(tier: Int): Float = when (tier) {
        3 -> 0.09f
        2 -> 0.065f
        1 -> 0.045f
        else -> 0f
    }

    fun makeBall(x: Float, y: Float, tier: Int, vx: Float): Ball =
        Ball(x = x, y = y, vx = vx, vy = -0.45f, radius = radiusForTier(tier), tier = tier)

    fun stepBall(ball: Ball, dt: Float): Ball {
        val slowFactor = if (ball.slowTime > 0f) 0.65f else 1f
        var vx = ball.vx * slowFactor
        var vy = ball.vy + GRAVITY * dt * slowFactor
        var x = ball.x + vx * dt
        var y = ball.y + vy * dt

        if (x - ball.radius < 0f) {
            x = ball.radius
            vx = -vx
        } else if (x + ball.radius > 1f) {
            x = 1f - ball.radius
            vx = -vx
        }
        if (y - ball.radius < 0f) {
            y = ball.radius
            vy = -vy
        }
        if (y + ball.radius > 1f) {
            y = 1f - ball.radius
            vy = -kotlin.math.abs(vy)
        }

        return ball.copy(x = x, y = y, vx = vx, vy = vy, slowTime = (ball.slowTime - dt).coerceAtLeast(0f))
    }

    fun projectileHitsBall(projectile: Projectile, ball: Ball): Boolean {
        if (!projectile.active) return false
        return kotlin.math.abs(projectile.x - ball.x) <= ball.radius && projectile.y <= ball.y + ball.radius
    }

    fun playerHitsBall(player: Player, ball: Ball): Boolean =
        hypot((player.x - ball.x).toDouble(), (player.y - ball.y).toDouble()) < (World.PLAYER_RADIUS + ball.radius)

    fun split(ball: Ball): List<Ball> {
        if (ball.tier <= 1) return emptyList()
        val nextTier = ball.tier - 1
        val yKick = -0.68f
        val speed = 0.42f + (3 - nextTier) * 0.1f
        val radius = radiusForTier(nextTier)
        return listOf(
            Ball(ball.x, ball.y, -speed, yKick, radius, nextTier, ball.slowTime),
            Ball(ball.x, ball.y, speed, yKick, radius, nextTier, ball.slowTime)
        )
    }
}
