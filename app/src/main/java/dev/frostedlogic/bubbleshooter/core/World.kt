package dev.frostedlogic.bubbleshooter.core

data class Vec2(val x: Float, val y: Float)

data class Player(
    val x: Float = 0.5f,
    val y: Float = 0.92f,
    val speed: Float = 1.1f,
    val hp: Int = 3,
    val maxHp: Int = 4,
    val invulnTime: Float = 0f,
    val shieldCharges: Int = 0
)

data class Projectile(
    val active: Boolean = false,
    val x: Float = 0.5f,
    val y: Float = 0.9f,
    val vy: Float = -1.7f
)

data class Ball(
    val x: Float,
    val y: Float,
    val vx: Float,
    val vy: Float,
    val radius: Float,
    val tier: Int,
    val slowTime: Float = 0f
)

object World {
    const val WIDTH = 1f
    const val HEIGHT = 1f
    const val DT = 1f / 60f
    const val PLAYER_RADIUS = 0.035f
}
