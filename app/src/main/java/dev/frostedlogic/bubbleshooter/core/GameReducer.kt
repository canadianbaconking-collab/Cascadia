package dev.frostedlogic.bubbleshooter.core

import kotlin.math.abs

object GameReducer {
    fun reduce(state: GameState, action: GameAction): GameState {
        return when (action) {
            is GameAction.AimChanged -> state.copy(aimAngleRad = action.angleRad.coerceIn(-1.35f, 1.35f))
            is GameAction.NewRun -> GameState.new(action.seed)
            GameAction.Shoot -> shoot(state)
            is GameAction.PickBoon -> pickBoon(state, action.boon)
            GameAction.NextRoom -> if (state.phase == Phase.ChoosingBoon) startRoom(state.copy(phase = Phase.Playing)) else state
            GameAction.RerollBoons -> rerollBoons(state)
            GameAction.UsePaint -> usePaint(state)
        }
    }

    fun startRoom(state: GameState): GameState {
        val spec = Rooms.roomSpec(state)
        val (rngAfterRoom, grid) = Rooms.generateRoom(state.rng, spec)
        val (rng1, current) = nextBubble(rngAfterRoom)
        val (rng2, next) = nextBubble(rng1)
        val base = state.copy(
            phase = Phase.Playing,
            shotsLeft = spec.shots,
            grid = grid,
            currentBubble = current,
            nextBubble = next,
            rng = rng2,
            pendingChoice = null,
            lastEvents = emptyList(),
            paintAvailable = state.boons.contains(BoonId.Paint)
        )
        val modded = Boss.applyRoomModifiers(base, spec)
        return Boons.applyOnRoomStart(modded)
    }

    private fun shoot(state: GameState): GameState {
        if (state.phase != Phase.Playing || state.shotsLeft <= 0) return state
        val events = mutableListOf<GameEvent>(GameEvent.ShotFired)
        var nextState = state
        var grid = placeBubble(nextState.grid, nextState.currentBubble, nextState.aimAngleRad)
        val placed = findLastPlacedIndex(nextState.grid, grid)
        val isBomb = state.boons.contains(BoonId.BombShot) && (state.shotsFired + 1) % 5 == 0

        if (placed >= 0) {
            if (isBomb) {
                val bombIndices = bombArea(grid, placed)
                grid = Match.clearCluster(grid, bombIndices)
                events += GameEvent.Popped(bombIndices)
            }

            val code = grid.cells[placed]
            if (Grid.isColorCode(code)) {
                val cluster = Match.findCluster(grid, placed, code)
                if (cluster.size >= 3) {
                    val toClear = cluster.toMutableSet()
                    // Goal adjacency rule: goals touching popped clusters also pop.
                    cluster.forEach { idx ->
                        val r = idx / grid.cols
                        val c = idx % grid.cols
                        Grid.neighbors(r, c, grid.rows, grid.cols, true)
                            .filter { grid.cells[it] == Grid.GOAL }
                            .forEach { toClear += it }
                    }
                    val clearedIndices = toClear.toIntArray()
                    grid = Match.clearCluster(grid, clearedIndices)
                    events += GameEvent.Popped(clearedIndices)
                }
            }

            if (state.boons.contains(BoonId.Sticky)) {
                val stickyTarget = firstAdjacentColor(grid, placed)
                if (stickyTarget >= 0) {
                    val copy = grid.cells.copyOf()
                    copy[stickyTarget] = Grid.codeFromBubble(state.currentBubble)
                    grid = grid.copy(cells = copy)
                }
            }

            val connected = Match.findTopConnected(grid)
            val (cleared, fell) = Match.clearFloating(grid, connected)
            grid = cleared
            if (fell.isNotEmpty()) events += GameEvent.Fell(fell)
        }

        val goalsRemaining = Rooms.countGoals(grid)
        val shotsLeft = state.shotsLeft - 1
        val shotsFired = state.shotsFired + 1
        val (rngNext, newNext) = nextBubble(state.rng)

        nextState = nextState.copy(
            grid = grid,
            shotsLeft = shotsLeft,
            currentBubble = state.nextBubble,
            nextBubble = newNext,
            rng = rngNext,
            lastEvents = events,
            shotsFired = shotsFired,
            goalsCleared = state.goalsCleared + (Rooms.countGoals(state.grid) - goalsRemaining)
        )

        if (goalsRemaining <= 0) {
            events += GameEvent.RoomCleared(state.roomIndex)
            if (state.roomIndex >= 8) {
                return nextState.copy(phase = Phase.WonRun, lastEvents = events + GameEvent.RunWon, roomsCleared = 8)
            }
            val (rngOffer, choice) = Boons.offer(nextState.rng, nextState.boons.toSet(), nextState.rerollsLeft > 0)
            return nextState.copy(phase = Phase.ChoosingBoon, pendingChoice = choice, rng = rngOffer, roomsCleared = state.roomIndex, lastEvents = events)
        }

        if (shotsLeft <= 0) {
            return nextState.copy(phase = Phase.LostRun, lastEvents = events + GameEvent.RunLost, roomsCleared = state.roomIndex - 1)
        }

        return Boons.applyOnShoot(nextState)
    }

    private fun pickBoon(state: GameState, boon: BoonId): GameState {
        if (state.phase != Phase.ChoosingBoon) return state
        if (state.pendingChoice?.options?.contains(boon) != true) return state
        val boons = (state.boons + boon).distinct()
        return startRoom(state.copy(roomIndex = state.roomIndex + 1, boons = boons, phase = Phase.Playing))
    }

    private fun rerollBoons(state: GameState): GameState {
        if (state.phase != Phase.ChoosingBoon || state.rerollsLeft <= 0) return state
        val (rng, choice) = Boons.offer(state.rng, state.boons.toSet(), false)
        return state.copy(rng = rng, pendingChoice = choice, rerollsLeft = state.rerollsLeft - 1)
    }

    private fun usePaint(state: GameState): GameState {
        if (state.phase != Phase.Playing || !state.paintAvailable || state.paintUsedThisRoom) return state
        val nextColor = when (state.currentBubble.color) {
            BubbleColor.Red -> BubbleColor.Green
            BubbleColor.Green -> BubbleColor.Blue
            BubbleColor.Blue -> BubbleColor.Red
            BubbleColor.Goal -> BubbleColor.Red
        }
        return state.copy(currentBubble = Bubble(nextColor), paintUsedThisRoom = true)
    }

    private fun nextBubble(rng: RngState): Pair<RngState, Bubble> {
        val (next, value) = Rng.nextInt(rng, 10)
        val color = when {
            value <= 2 -> BubbleColor.Red
            value <= 5 -> BubbleColor.Green
            value <= 8 -> BubbleColor.Blue
            else -> BubbleColor.Goal
        }
        return next to Bubble(color = color, isGoal = color == BubbleColor.Goal)
    }

    private fun placeBubble(grid: GridState, bubble: Bubble, angle: Float): GridState {
        val normalized = (angle / 1.35f + 1f) / 2f
        val colFromAngle = (normalized * grid.cols).toInt().coerceIn(0, grid.cols - 1)
        val candidateCols = (0 until grid.cols).sortedBy { abs(it - colFromAngle) }
        for (col in candidateCols) {
            var targetRow = grid.rows - 1
            while (targetRow >= 0 && Grid.get(grid, targetRow, col) != Grid.EMPTY) targetRow--
            if (targetRow >= 0) return Grid.set(grid, targetRow, col, Grid.codeFromBubble(bubble))
        }
        return grid
    }

    private fun findLastPlacedIndex(old: GridState, new: GridState): Int {
        for (i in old.cells.indices) {
            if (old.cells[i] == Grid.EMPTY && new.cells[i] != Grid.EMPTY) return i
        }
        return -1
    }

    private fun bombArea(grid: GridState, center: Int): IntArray {
        val indices = mutableSetOf(center)
        val r = center / grid.cols
        val c = center % grid.cols
        Grid.neighbors(r, c, grid.rows, grid.cols, true)
            .filter { grid.cells[it] != Grid.STONE }
            .forEach { indices += it }
        return indices.toIntArray()
    }

    private fun firstAdjacentColor(grid: GridState, center: Int): Int {
        val r = center / grid.cols
        val c = center % grid.cols
        return Grid.neighbors(r, c, grid.rows, grid.cols, true)
            .firstOrNull { grid.cells[it] in listOf(Grid.RED, Grid.GREEN, Grid.BLUE, Grid.GOAL) } ?: -1
    }
}
