package dev.frostedlogic.bubbleshooter.core

data class RngState(val s0: Long, val s1: Long)

object Rng {
    fun fromSeed(seed: Long): RngState {
        var x = seed xor 0x9E3779B97F4A7C15UL.toLong()
        x = x xor (x ushr 30)
        x *= 0xBF58476D1CE4E5B9UL.toLong()
        x = x xor (x ushr 27)
        x *= 0x94D049BB133111EBUL.toLong()
        x = x xor (x ushr 31)
        return RngState(x, x xor 0xD2B74407B1CE6E93UL.toLong())
    }

    private fun nextLong(state: RngState): Pair<RngState, Long> {
        var s1 = state.s0
        val s0 = state.s1
        s1 = s1 xor (s1 shl 23)
        val n = s1 xor s0 xor (s1 ushr 17) xor (s0 ushr 26)
        return RngState(s0, n) to (n + s0)
    }

    fun nextInt(state: RngState, bound: Int): Pair<RngState, Int> {
        require(bound > 0)
        val (next, raw) = nextLong(state)
        val v = (raw and Long.MAX_VALUE).rem(bound.toLong()).toInt()
        return next to v
    }

    fun nextFloat01(state: RngState): Pair<RngState, Float> {
        val (next, raw) = nextLong(state)
        val bits = (raw ushr 40).toInt() and 0xFFFFFF
        return next to (bits / 16777216f)
    }
}
