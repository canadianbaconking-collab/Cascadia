package dev.frostedlogic.bubbleshooter.core

enum class BoonId { RapidFire, Ice, Shield }

data class BoonChoice(val options: List<BoonId>)

object Boons {
    fun offer(rng: RngState): Pair<RngState, BoonChoice> {
        var next = rng
        val pool = BoonId.entries.toMutableList()
        val picks = mutableListOf<BoonId>()
        repeat(3) {
            val (r, idx) = Rng.nextInt(next, pool.size)
            next = r
            picks += pool.removeAt(idx)
        }
        return next to BoonChoice(picks)
    }

    fun apply(state: GameState, boon: BoonId): GameState = when (boon) {
        BoonId.RapidFire -> state.copy(boons = state.boons + boon, shootCooldown = 0.2f)
        BoonId.Ice -> state.copy(boons = state.boons + boon)
        BoonId.Shield -> {
            val hp = (state.player.hp + 1).coerceAtMost(state.player.maxHp)
            state.copy(boons = state.boons + boon, player = state.player.copy(hp = hp, shieldCharges = state.player.shieldCharges + 1))
        }
    }
}
