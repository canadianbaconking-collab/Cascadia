package dev.frostedlogic.bubbleshooter.core

data class GridState(val rows: Int, val cols: Int, val cells: IntArray, val topAnchored: Boolean = true)

enum class BubbleColor { Red, Green, Blue, Goal }

data class Bubble(val color: BubbleColor, val isGoal: Boolean = false)

object Grid {
    const val EMPTY = 0
    const val RED = 1
    const val GREEN = 2
    const val BLUE = 3
    const val GOAL = 4
    const val STONE = 5

    fun empty(rows: Int, cols: Int): GridState = GridState(rows, cols, IntArray(rows * cols) { EMPTY })

    fun get(grid: GridState, r: Int, c: Int): Int {
        if (r !in 0 until grid.rows || c !in 0 until grid.cols) return EMPTY
        return grid.cells[r * grid.cols + c]
    }

    fun set(grid: GridState, r: Int, c: Int, code: Int): GridState {
        if (r !in 0 until grid.rows || c !in 0 until grid.cols) return grid
        val copy = grid.cells.copyOf()
        copy[r * grid.cols + c] = code
        return grid.copy(cells = copy)
    }

    fun neighbors(r: Int, c: Int, rows: Int, cols: Int, staggered: Boolean = true): IntArray {
        val out = ArrayList<Int>(6)
        val evenRow = r % 2 == 0
        val offsets = if (!staggered) {
            arrayOf(
                intArrayOf(-1, 0), intArrayOf(-1, -1),
                intArrayOf(0, -1), intArrayOf(0, 1),
                intArrayOf(1, 0), intArrayOf(1, -1)
            )
        } else if (evenRow) {
            arrayOf(
                intArrayOf(-1, -1), intArrayOf(-1, 0),
                intArrayOf(0, -1), intArrayOf(0, 1),
                intArrayOf(1, -1), intArrayOf(1, 0)
            )
        } else {
            arrayOf(
                intArrayOf(-1, 0), intArrayOf(-1, 1),
                intArrayOf(0, -1), intArrayOf(0, 1),
                intArrayOf(1, 0), intArrayOf(1, 1)
            )
        }
        for (o in offsets) {
            val nr = r + o[0]
            val nc = c + o[1]
            if (nr in 0 until rows && nc in 0 until cols) out += nr * cols + nc
        }
        return out.toIntArray()
    }

    fun codeFromBubble(bubble: Bubble): Int = when (bubble.color) {
        BubbleColor.Red -> RED
        BubbleColor.Green -> GREEN
        BubbleColor.Blue -> BLUE
        BubbleColor.Goal -> GOAL
    }

    fun isColorCode(code: Int): Boolean = code == RED || code == GREEN || code == BLUE || code == GOAL
}
