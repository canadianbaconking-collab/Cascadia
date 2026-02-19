package dev.frostedlogic.bubbleshooter.core

data class BossState(val fogActive: Boolean)

object Boss {
    fun applyRoomModifiers(state: GameState, room: RoomSpec): GameState {
        return state.copy(boss = BossState(fogActive = room.bossFog))
    }
}
