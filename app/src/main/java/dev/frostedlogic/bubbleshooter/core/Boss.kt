package dev.frostedlogic.bubbleshooter.core

data class BossState(
    val fogActive: Boolean,
    val tremorActive: Boolean = false
)

object Boss {
    fun applyRoomModifiers(state: GameState, room: RoomSpec): GameState {
        return state.copy(boss = BossState(fogActive = room.bossFog, tremorActive = room.bossFog && state.roomIndex % 2 == 0))
    }
}
