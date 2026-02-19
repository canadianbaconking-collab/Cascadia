package dev.frostedlogic.bubbleshooter.core

sealed interface GameAction {
    data class AimChanged(val angleRad: Float) : GameAction
    data object Shoot : GameAction
    data class PickBoon(val boon: BoonId) : GameAction
    data object RerollBoons : GameAction
    data object NextRoom : GameAction
    data class NewRun(val seed: Long) : GameAction
    data object UsePaint : GameAction
}
