package dev.frostedlogic.bubbleshooter.core

sealed interface GameEvent {
    data object RoomCleared : GameEvent
    data object HitTaken : GameEvent
    data object ShotFired : GameEvent
    data object RunWon : GameEvent
    data object RunLost : GameEvent
}
