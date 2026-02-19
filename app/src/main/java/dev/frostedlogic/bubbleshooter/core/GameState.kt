package dev.frostedlogic.bubbleshooter.core

enum class Phase { Playing, ChoosingBoon, WonRun, LostRun }

data class GameState(
    val seed: Long,
    val rng: RngState,
    val phase: Phase,
    val roomIndex: Int,
    val player: Player,
    val projectile: Projectile,
    val balls: List<Ball>,
    val boons: Set<BoonId>,
    val offeredBoons: List<BoonId>,
    val time: Float,
    val shootCooldown: Float,
    val shootTimer: Float,
    val bossTimer: Float,
    val lastEvents: List<GameEvent>
) {
    companion object {
        fun new(seed: Long): GameState {
            val rng = Rng.fromSeed(seed)
            val initial = GameState(
                seed = seed,
                rng = rng,
                phase = Phase.Playing,
                roomIndex = 1,
                player = Player(),
                projectile = Projectile(),
                balls = emptyList(),
                boons = emptySet(),
                offeredBoons = emptyList(),
                time = 0f,
                shootCooldown = 0.35f,
                shootTimer = 0f,
                bossTimer = Boss.IMPULSE_INTERVAL,
                lastEvents = emptyList()
            )
            return GameReducer.startRoom(initial)
        }
    }
}
