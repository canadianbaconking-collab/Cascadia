package dev.frostedlogic.bubbleshooter.core

data class RngState(val state: Long)

object Rng {
    fun fromSeed(seed: Long): RngState = RngState(if (seed == 0L) 0x9E3779B97F4A7C15UL.toLong() else seed)

    private fun nextLong(rng: RngState): Pair<RngState, Long> {
        var x = rng.state
        x = x xor (x shl 13)
        x = x xor (x ushr 7)
        x = x xor (x shl 17)
        return RngState(x) to x
    }

    fun nextInt(rng: RngState, bound: Int): Pair<RngState, Int> {
        require(bound > 0)
        val (next, v) = nextLong(rng)
        return next to ((v and Long.MAX_VALUE) % bound).toInt()
    }

    fun nextFloat(rng: RngState): Pair<RngState, Float> {
        val (next, v) = nextLong(rng)
        val bits = (v ushr 40).toInt() and 0xFFFFFF
        return next to (bits / 16777216f)
    }
}
