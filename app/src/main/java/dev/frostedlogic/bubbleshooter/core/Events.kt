package dev.frostedlogic.bubbleshooter.core

sealed interface GameEvent {
    data class Popped(val indices: IntArray) : GameEvent
    data class Fell(val indices: IntArray) : GameEvent
    data object ShotFired : GameEvent
    data class RoomCleared(val roomIndex: Int) : GameEvent
    data object RunWon : GameEvent
    data object RunLost : GameEvent
}
