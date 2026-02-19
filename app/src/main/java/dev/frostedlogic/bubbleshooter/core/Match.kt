package dev.frostedlogic.bubbleshooter.core

object Match {
    fun findCluster(grid: GridState, startIndex: Int, matchCode: Int): IntArray {
        if (startIndex !in grid.cells.indices || grid.cells[startIndex] != matchCode) return intArrayOf()
        val seen = BooleanArray(grid.cells.size)
        val stack = ArrayDeque<Int>()
        val out = ArrayList<Int>()
        stack.add(startIndex)
        while (stack.isNotEmpty()) {
            val idx = stack.removeLast()
            if (seen[idx]) continue
            seen[idx] = true
            if (grid.cells[idx] != matchCode) continue
            out += idx
            val r = idx / grid.cols
            val c = idx % grid.cols
            Grid.neighbors(r, c, grid.rows, grid.cols, true).forEach { n ->
                if (!seen[n]) stack.add(n)
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
