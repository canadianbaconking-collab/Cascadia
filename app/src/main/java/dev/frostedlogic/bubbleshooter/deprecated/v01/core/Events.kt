package dev.frostedlogic.bubbleshooter.deprecated.v01.core

sealed interface GameEvent {
    data class Popped(val indices: IntArray) : GameEvent
    data class Fell(val indices: IntArray) : GameEvent
    data object ShotFired : GameEvent
    data class RoomCleared(val roomIndex: Int) : GameEvent
    data object RunWon : GameEvent
    data object RunLost : GameEvent
    data class Bombed(val indices: IntArray) : GameEvent
    data object Painted : GameEvent
    data object BoonsRerolled : GameEvent
    data class ComboUp(val combo: Int) : GameEvent
}
