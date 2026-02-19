package dev.frostedlogic.bubbleshooter.core

object Boss {
    const val IMPULSE_INTERVAL = 5f

    fun applyTremor(state: GameState): GameState {
        var rng = state.rng
        val out = state.balls.map { ball ->
            val (r, n) = Rng.nextFloat(rng)
            rng = r
            val impulse = (n - 0.5f) * 0.5f
            ball.copy(vx = (ball.vx + impulse).coerceIn(-1.5f, 1.5f))
        }
        return state.copy(balls = out, rng = rng)
    }
}
