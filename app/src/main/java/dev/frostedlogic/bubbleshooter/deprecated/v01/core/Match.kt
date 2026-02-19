package dev.frostedlogic.bubbleshooter.deprecated.v01.core

object Match {
    fun findCluster(grid: GridState, startIndex: Int, matchCode: Int): IntArray {
        if (startIndex !in grid.cells.indices) return intArrayOf()
        val start = grid.cells[startIndex]
        if (start == Grid.EMPTY || start == Grid.STONE) return intArrayOf()
        val target = if (start == Grid.WILD) matchCode else start
        val seen = BooleanArray(grid.cells.size)
        val stack = ArrayDeque<Int>()
        val out = ArrayList<Int>()
        stack.add(startIndex)
        while (stack.isNotEmpty()) {
            val idx = stack.removeLast()
            if (seen[idx]) continue
            seen[idx] = true
            val code = grid.cells[idx]
            val matches = code == target || code == Grid.WILD || (target == Grid.WILD && Grid.isColor(code))
            if (!matches) continue
            out += idx
            val r = idx / grid.cols
            val c = idx % grid.cols
            Grid.neighbors(r, c, grid.rows, grid.cols, true).forEach { n ->
                if (!seen[n] && grid.cells[n] != Grid.STONE) stack.add(n)
            }
        }
        return out.toIntArray()
    }

    fun clearCluster(grid: GridState, indices: IntArray): GridState {
        if (indices.isEmpty()) return grid
        val copy = grid.cells.copyOf()
        indices.forEach { copy[it] = Grid.EMPTY }
        return grid.copy(cells = copy)
    }

    fun findTopConnected(grid: GridState): BooleanArray {
        val connected = BooleanArray(grid.cells.size)
        val queue = ArrayDeque<Int>()
        for (c in 0 until grid.cols) {
            val idx = c
            if (grid.cells[idx] != Grid.EMPTY) queue.add(idx)
        }
        while (queue.isNotEmpty()) {
            val idx = queue.removeFirst()
            if (connected[idx]) continue
            connected[idx] = true
            val r = idx / grid.cols
            val c = idx % grid.cols
            Grid.neighbors(r, c, grid.rows, grid.cols, true).forEach { n ->
                if (!connected[n] && grid.cells[n] != Grid.EMPTY) queue.add(n)
            }
        }
        return connected
    }

    fun clearFloating(grid: GridState, connected: BooleanArray): Pair<GridState, IntArray> {
        val copy = grid.cells.copyOf()
        val fell = ArrayList<Int>()
        for (i in copy.indices) {
            if (copy[i] != Grid.EMPTY && !connected[i]) {
                copy[i] = Grid.EMPTY
                fell += i
            }
        }
        return grid.copy(cells = copy) to fell.toIntArray()
    }
}
