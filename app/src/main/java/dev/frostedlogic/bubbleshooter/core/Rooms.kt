package dev.frostedlogic.bubbleshooter.core

data class RoomSpec(
    val rows: Int,
    val cols: Int,
    val goalCount: Int,
    val stoneCount: Int,
    val colors: Int,
    val shots: Int,
    val bossFog: Boolean
)

object Rooms {
    fun roomSpec(state: GameState): RoomSpec {
        val room = state.roomIndex
        val goals = 4 + room
        val shots = 12 + (room / 2)
        val stones = if (room >= 3) (room - 2).coerceAtMost(8) else 0
        return RoomSpec(10, 8, goals, stones, 3, shots, room >= 8)
    }

    fun generateRoom(rng: RngState, spec: RoomSpec): Pair<RngState, GridState> {
        var state = rng
        val cells = IntArray(spec.rows * spec.cols) { Grid.EMPTY }
        val fillRows = 4
        val templates = listOf(0, 1, 2)
        val (rTemplate, templateIndex) = Rng.nextInt(state, templates.size)
        state = rTemplate
        for (r in 0 until fillRows) {
            for (c in 0 until spec.cols) {
                val idx = r * spec.cols + c
                val shouldFill = when (templateIndex) {
                    0 -> true
                    1 -> (r + c) % 2 == 0
                    else -> c !in 3..4 || r < 2
                }
                if (!shouldFill) continue
                val (next, color) = Rng.nextInt(state, spec.colors)
                state = next
                cells[idx] = color + 1
            }
        }
        var goalsPlaced = 0
        while (goalsPlaced < spec.goalCount) {
            val (next, idx) = Rng.nextInt(state, fillRows * spec.cols)
            state = next
            if (cells[idx] != Grid.EMPTY && cells[idx] != Grid.GOAL) {
                cells[idx] = Grid.GOAL
                goalsPlaced++
            }
        }
        var stonesPlaced = 0
        while (stonesPlaced < spec.stoneCount) {
            val (next, idx) = Rng.nextInt(state, fillRows * spec.cols)
            state = next
            if (cells[idx] != Grid.EMPTY && cells[idx] != Grid.GOAL && cells[idx] != Grid.STONE) {
                cells[idx] = Grid.STONE
                stonesPlaced++
            }
        }
        return state to GridState(spec.rows, spec.cols, cells)
    }

    fun countGoals(grid: GridState): Int = grid.cells.count { it == Grid.GOAL }
}
