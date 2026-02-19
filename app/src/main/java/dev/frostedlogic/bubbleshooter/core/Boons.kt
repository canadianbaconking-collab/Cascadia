package dev.frostedlogic.bubbleshooter.core

enum class BoonId { ShotsPlus, Sights, BombShot, Paint, Sticky }

data class ChoiceState(val options: List<BoonId>, val canReroll: Boolean = false)

object Boons {
    fun offer(rng: RngState, owned: Set<BoonId>): Pair<RngState, ChoiceState> {
        var state = rng
        val pool = BoonId.entries.filterNot { owned.contains(it) }.ifEmpty { BoonId.entries }
        val picks = mutableListOf<BoonId>()
        while (picks.size < 3 && picks.size < pool.size) {
            val (next, idx) = Rng.nextInt(state, pool.size)
            state = next
            val choice = pool[idx]
            if (!picks.contains(choice)) picks += choice
        }
        return state to ChoiceState(options = picks)
    }

    fun applyOnRoomStart(state: GameState): GameState {
        var updated = state
        if (state.boons.contains(BoonId.ShotsPlus)) updated = updated.copy(shotsLeft = updated.shotsLeft + 2)
        return updated
    }

    fun applyOnShoot(state: GameState): GameState = state
}
