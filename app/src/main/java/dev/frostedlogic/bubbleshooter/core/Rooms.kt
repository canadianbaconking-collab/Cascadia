package dev.frostedlogic.bubbleshooter.core

object Rooms {
    fun spawn(room: Int, rng: RngState): Pair<RngState, List<Ball>> {
        var next = rng
        val count = (1 + (room - 1) / 3).coerceAtMost(3)
        val tierBase = if (room < 3) 3 else if (room < 6) 2 else 3
        val speed = 0.28f + room * 0.03f
        val balls = mutableListOf<Ball>()
        repeat(count) { i ->
            val (r1, xRand) = Rng.nextFloat(next)
            next = r1
            val x = 0.2f + xRand * 0.6f
            val dir = if (i % 2 == 0) 1 else -1
            balls += Physics.makeBall(x = x, y = 0.25f + i * 0.1f, tier = tierBase, vx = speed * dir)
        }
        return next to balls
    }
}
