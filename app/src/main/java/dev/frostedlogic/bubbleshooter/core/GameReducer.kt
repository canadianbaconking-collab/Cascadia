package dev.frostedlogic.bubbleshooter.core

object GameReducer {
    fun reduce(state: GameState, action: GameAction): GameState {
        return when (action) {
            is GameAction.AimChanged -> state.copy(aimAngleRad = action.angleRad)
            is GameAction.NewRun -> GameState.new(action.seed)
            GameAction.Shoot -> shoot(state)
            is GameAction.PickBoon -> pickBoon(state, action.boon)
            GameAction.NextRoom -> if (state.phase == Phase.ChoosingBoon) startRoom(state.copy(phase = Phase.Playing)) else state
            GameAction.RerollBoons -> state
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

        if (placed >= 0) {
            val code = grid.cells[placed]
            val cluster = Match.findCluster(grid, placed, code)
            if (cluster.size >= 3) {
                grid = Match.clearCluster(grid, cluster)
                events += GameEvent.Popped(cluster)
                val connected = Match.findTopConnected(grid)
                val (cleared, fell) = Match.clearFloating(grid, connected)
                grid = cleared
                if (fell.isNotEmpty()) events += GameEvent.Fell(fell)
            }
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
            val (rngOffer, choice) = Boons.offer(nextState.rng, nextState.boons.toSet())
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

    private fun nextBubble(rng: RngState): Pair<RngState, Bubble> {
        val (next, value) = Rng.nextInt(rng, 4)
        val color = when (value) {
            0 -> BubbleColor.Red
            1 -> BubbleColor.Green
            2 -> BubbleColor.Blue
            else -> BubbleColor.Goal
        }
        return next to Bubble(color = color, isGoal = color == BubbleColor.Goal)
    }

    private fun placeBubble(grid: GridState, bubble: Bubble, angle: Float): GridState {
        val colFromAngle = (((angle + Math.PI.toFloat() / 2f) / Math.PI.toFloat()) * grid.cols)
            .toInt().coerceIn(0, grid.cols - 1)
        var targetRow = grid.rows - 1
        while (targetRow >= 0 && Grid.get(grid, targetRow, colFromAngle) != Grid.EMPTY) targetRow--
        if (targetRow < 0) return grid
        return Grid.set(grid, targetRow, colFromAngle, Grid.codeFromBubble(bubble))
    }

    private fun findLastPlacedIndex(old: GridState, new: GridState): Int {
        for (i in old.cells.indices) {
            if (old.cells[i] == Grid.EMPTY && new.cells[i] != Grid.EMPTY) return i
        }
        return -1
    }
}
