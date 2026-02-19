package dev.frostedlogic.bubbleshooter.core

sealed interface GameAction {
    data class Tick(val dt: Float) : GameAction
    data class MovePlayer(val normalizedX: Float) : GameAction
    data object Shoot : GameAction
    data class PickBoon(val boon: BoonId) : GameAction
    data class NewRun(val seed: Long) : GameAction
}
