package dev.frostedlogic.bubbleshooter.deprecated.v01.core

data class RoomSpec(
    val rows: Int,
    val cols: Int,
    val goalCount: Int,
    val stoneCount: Int,
    val colors: Int,
    val shots: Int,
    val bossFog: Boolean,
    val pattern: Int
)

object Rooms {
    fun roomSpec(state: GameState): RoomSpec {
        val room = state.roomIndex
        val goals = 4 + room
        val shots = 12 + (room / 2)
        val colors = if (room < 3) 3 else if (room < 6) 4 else 5
        val stones = if (room < 4) 0 else room / 2
        return RoomSpec(10, 8, goals, stones, colors, shots, room >= 8, room % 4)
    }

    fun generateRoom(rng: RngState, spec: RoomSpec): Pair<RngState, GridState> {
        var state = rng
        val cells = IntArray(spec.rows * spec.cols) { Grid.EMPTY }
        val fillRows = 4 + (spec.pattern % 2)
        for (r in 0 until fillRows) {
            for (c in 0 until spec.cols) {
                if (spec.pattern == 1 && c % 2 == 0) continue
                if (spec.pattern == 2 && r % 2 == 1 && c in 2..5) continue
                val idx = r * spec.cols + c
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
        var stones = 0
        while (stones < spec.stoneCount) {
            val (next, idx) = Rng.nextInt(state, fillRows * spec.cols)
            state = next
            if (cells[idx] in Grid.RED..Grid.PURPLE) {
                cells[idx] = Grid.STONE
                stones++
            }
        }
        return state to GridState(spec.rows, spec.cols, cells)
    }

    fun countGoals(grid: GridState): Int = grid.cells.count { it == Grid.GOAL }
}
