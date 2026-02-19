package dev.frostedlogic.bubbleshooter.deprecated.v01.core

enum class BoonId { ShotsPlus, Sights, BombShot, Paint, Sticky, Magnet, ChainPop, LuckyWild }

data class ChoiceState(val options: List<BoonId>, val canReroll: Boolean = false)

object Boons {
    fun offer(rng: RngState, owned: Set<BoonId>, rerollsLeft: Int = 0): Pair<RngState, ChoiceState> {
        var state = rng
        val pool = BoonId.entries.filterNot { owned.contains(it) }.ifEmpty { BoonId.entries }
        val picks = mutableListOf<BoonId>()
        while (picks.size < 3 && picks.size < pool.size) {
            val (next, idx) = Rng.nextInt(state, pool.size)
            state = next
            val choice = pool[idx]
            if (!picks.contains(choice)) picks += choice
        }
        return state to ChoiceState(options = picks, canReroll = rerollsLeft > 0)
    }

    fun applyOnRoomStart(state: GameState): GameState {
        var updated = state
        if (state.boons.contains(BoonId.ShotsPlus)) updated = updated.copy(shotsLeft = updated.shotsLeft + 2)
        if (state.boons.contains(BoonId.Paint)) updated = updated.copy(paintCharges = 1)
        return updated
    }

    fun applyOnShoot(state: GameState): GameState {
        var out = state
        if (state.boons.contains(BoonId.LuckyWild) && state.shotsFired % 4 == 0) {
            out = out.copy(nextBubble = Bubble(BubbleColor.Wild))
        }
        return out
    }

    fun description(boon: BoonId): String = when (boon) {
        BoonId.ShotsPlus -> "+2 shots each room"
        BoonId.Sights -> "Shows longer trajectory"
        BoonId.BombShot -> "Every 5th shot explodes"
        BoonId.Paint -> "1x per room recolor current bubble"
        BoonId.Sticky -> "Convert a neighbor to your color"
        BoonId.Magnet -> "Small aim assist toward center"
        BoonId.ChainPop -> "Pop size-2 clusters after main pop"
        BoonId.LuckyWild -> "Periodic wild bubbles"
    }
}
