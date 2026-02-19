package dev.frostedlogic.bubbleshooter.core

enum class Phase { Playing, ChoosingBoon, WonRun, LostRun }

data class GameState(
    val seed: Long,
    val phase: Phase,
    val roomIndex: Int,
    val shotsLeft: Int,
    val boons: List<BoonId>,
    val grid: GridState,
    val currentBubble: Bubble,
    val nextBubble: Bubble,
    val rng: RngState,
    val boss: BossState,
    val pendingChoice: ChoiceState?,
    val lastEvents: List<GameEvent>,
    val aimAngleRad: Float = 0f,
    val roomsCleared: Int = 0,
    val goalsCleared: Int = 0,
    val paintCharges: Int = 0,
    val shotsFired: Int = 0,
    val combo: Int = 0,
    val bestCombo: Int = 0,
    val whiffStreak: Int = 0,
    val rerollsLeft: Int = 1,
    val runTicks: Int = 0,
    val ttffTicks: Int? = null,
    val replayPrompt: Boolean = false,
    val lossReason: String? = null,
    val goalsAtRoomStart: Int = 0,
    val cycle: Int = 0,
    val candidateBoard: List<String> = listOf("2 features", "1 polish", "complexity<=7", "risk<=7")
) {
    companion object {
        fun new(seed: Long): GameState {
            val rng = Rng.fromSeed(seed)
            val init = GameState(
                seed = seed,
                phase = Phase.Playing,
                roomIndex = 1,
                shotsLeft = 12,
                boons = emptyList(),
                grid = Grid.empty(10, 8),
                currentBubble = Bubble(BubbleColor.Red),
                nextBubble = Bubble(BubbleColor.Green),
                rng = rng,
                boss = BossState(false),
                pendingChoice = null,
                lastEvents = emptyList()
            )
            return GameReducer.startRoom(init)
        }
    }
}
