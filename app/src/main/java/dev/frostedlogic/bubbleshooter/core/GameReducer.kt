package dev.frostedlogic.bubbleshooter.core

import kotlin.math.PI

object GameReducer {
    fun reduce(state: GameState, action: GameAction): GameState {
        return when (action) {
            is GameAction.AimChanged -> state.copy(aimAngleRad = if (state.boons.contains(BoonId.Magnet)) action.angleRad * 0.85f else action.angleRad)
            is GameAction.NewRun -> GameState.new(action.seed)
            GameAction.Shoot -> shoot(state)
            is GameAction.PickBoon -> pickBoon(state, action.boon)
            GameAction.NextRoom -> if (state.phase == Phase.ChoosingBoon) startRoom(state.copy(phase = Phase.Playing)) else state
            GameAction.RerollBoons -> rerollBoons(state)
            GameAction.UsePaint -> usePaint(state)
            GameAction.Tick -> state.copy(runTicks = state.runTicks + 1)
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
            combo = 0,
            whiffStreak = 0,
            goalsAtRoomStart = Rooms.countGoals(grid),
            cycle = (state.roomIndex - 1) / 2 + 1,
            lossReason = null,
            replayPrompt = false
        )
        val modded = Boss.applyRoomModifiers(base, spec)
        return Boons.applyOnRoomStart(modded)
    }

    private fun shoot(state: GameState): GameState {
        if (state.phase != Phase.Playing || state.shotsLeft <= 0) return state
        val events = mutableListOf<GameEvent>(GameEvent.ShotFired)
        var grid = placeBubble(state.grid, state.currentBubble, withTremor(state))
        val placed = findLastPlacedIndex(state.grid, grid)
        var poppedCount = 0

        if (placed >= 0) {
            val code = grid.cells[placed]
            val cluster = Match.findCluster(grid, placed, code)
            if (cluster.size >= 3) {
                poppedCount += cluster.size
                grid = Match.clearCluster(grid, cluster)
                events += GameEvent.Popped(cluster)
                if (state.boons.contains(BoonId.ChainPop)) {
                    val chain = findChainPop(grid)
                    if (chain.isNotEmpty()) {
                        poppedCount += chain.size
                        grid = Match.clearCluster(grid, chain)
                        events += GameEvent.Popped(chain)
                    }
                }
            }

            if (state.boons.contains(BoonId.BombShot) && (state.shotsFired + 1) % 5 == 0) {
                val bomb = blastRadius(grid, placed)
                if (bomb.isNotEmpty()) {
                    poppedCount += bomb.size
                    grid = Match.clearCluster(grid, bomb)
                    events += GameEvent.Bombed(bomb)
                }
            }

            if (state.boons.contains(BoonId.Sticky) && code in Grid.RED..Grid.PURPLE) {
                val sticky = stickyConvert(grid, placed, code)
                grid = sticky.first
                poppedCount += sticky.second
            }

            if (poppedCount > 0) {
                val connected = Match.findTopConnected(grid)
                val (cleared, fell) = Match.clearFloating(grid, connected)
                grid = cleared
                if (fell.isNotEmpty()) {
                    poppedCount += fell.size
                    events += GameEvent.Fell(fell)
                }
            }
        }

        val goalsRemaining = Rooms.countGoals(grid)
        val shotsLeft = state.shotsLeft - 1
        val shotsFired = state.shotsFired + 1
        val (rngNext, generatedNext) = nextBubble(state.rng)
        val weighted = if (state.whiffStreak >= 2) weightedBubbleForGrid(rngNext, grid) else (rngNext to generatedNext)
        val rngOut = weighted.first
        val mercyNext = weighted.second

        val combo = if (poppedCount > 0) state.combo + 1 else 0
        var bonusShots = 0
        if (combo > 0 && combo % 3 == 0) bonusShots = 1

        if (combo > 0) events += GameEvent.ComboUp(combo)

        var nextState = state.copy(
            grid = grid,
            shotsLeft = shotsLeft + bonusShots,
            currentBubble = state.nextBubble,
            nextBubble = mercyNext,
            rng = rngOut,
            lastEvents = events,
            shotsFired = shotsFired,
            goalsCleared = state.goalsCleared + (Rooms.countGoals(state.grid) - goalsRemaining),
            combo = combo,
            bestCombo = maxOf(state.bestCombo, combo),
            whiffStreak = if (poppedCount == 0) state.whiffStreak + 1 else 0,
            ttffTicks = state.ttffTicks ?: if (poppedCount > 0) state.runTicks else null
        )

        if (goalsRemaining <= 0) {
            events += GameEvent.RoomCleared(state.roomIndex)
            if (state.roomIndex >= 8) {
                return nextState.copy(phase = Phase.WonRun, lastEvents = events + GameEvent.RunWon, roomsCleared = 8)
            }
            val (rngOffer, choice) = Boons.offer(nextState.rng, nextState.boons.toSet(), nextState.rerollsLeft)
            return nextState.copy(phase = Phase.ChoosingBoon, pendingChoice = choice, rng = rngOffer, roomsCleared = state.roomIndex, lastEvents = events)
        }

        if (shotsLeft <= 0) {
            return nextState.copy(
                phase = Phase.LostRun,
                lastEvents = events + GameEvent.RunLost,
                roomsCleared = state.roomIndex - 1,
                replayPrompt = state.runTicks < 300,
                lossReason = "Out of shots with $goalsRemaining goals left"
            )
        }

        nextState = Boons.applyOnShoot(nextState)
        return nextState
    }

    private fun rerollBoons(state: GameState): GameState {
        if (state.phase != Phase.ChoosingBoon || state.rerollsLeft <= 0) return state
        val (rngOffer, choice) = Boons.offer(state.rng, state.boons.toSet(), state.rerollsLeft - 1)
        return state.copy(rng = rngOffer, pendingChoice = choice, rerollsLeft = state.rerollsLeft - 1, lastEvents = listOf(GameEvent.BoonsRerolled))
    }

    private fun usePaint(state: GameState): GameState {
        if (state.phase != Phase.Playing || state.paintCharges <= 0) return state
        val paintable = listOf(BubbleColor.Red, BubbleColor.Green, BubbleColor.Blue, BubbleColor.Yellow, BubbleColor.Purple)
        val idx = paintable.indexOf(state.currentBubble.color)
        val nextColor = paintable[(idx + 1).coerceAtMost(paintable.lastIndex)]
        return state.copy(currentBubble = Bubble(nextColor), paintCharges = state.paintCharges - 1, lastEvents = listOf(GameEvent.Painted))
    }

    private fun pickBoon(state: GameState, boon: BoonId): GameState {
        if (state.phase != Phase.ChoosingBoon) return state
        if (state.pendingChoice?.options?.contains(boon) != true) return state
        val boons = (state.boons + boon).distinct()
        return startRoom(state.copy(roomIndex = state.roomIndex + 1, boons = boons, phase = Phase.Playing))
    }

    private fun nextBubble(rng: RngState): Pair<RngState, Bubble> {
        val (next, value) = Rng.nextInt(rng, 10)
        val color = when (value) {
            0, 1 -> BubbleColor.Red
            2, 3 -> BubbleColor.Green
            4, 5 -> BubbleColor.Blue
            6 -> BubbleColor.Yellow
            7 -> BubbleColor.Purple
            8 -> BubbleColor.Goal
            else -> BubbleColor.Wild
        }
        return next to Bubble(color = color, isGoal = color == BubbleColor.Goal)
    }

    private fun weightedBubbleForGrid(rng: RngState, grid: GridState): Pair<RngState, Bubble> {
        val counts = listOf(Grid.RED, Grid.GREEN, Grid.BLUE, Grid.YELLOW, Grid.PURPLE).associateWith { code -> grid.cells.count { it == code } }
        val best = counts.maxByOrNull { it.value }?.key ?: Grid.RED
        val color = when (best) {
            Grid.RED -> BubbleColor.Red
            Grid.GREEN -> BubbleColor.Green
            Grid.BLUE -> BubbleColor.Blue
            Grid.YELLOW -> BubbleColor.Yellow
            else -> BubbleColor.Purple
        }
        return rng to Bubble(color)
    }

    private fun withTremor(state: GameState): Float {
        if (!state.boss.tremorActive || state.shotsFired % 3 != 2) return state.aimAngleRad
        return (state.aimAngleRad + 0.22f).coerceIn((-PI / 2).toFloat(), (PI / 2).toFloat())
    }

    private fun blastRadius(grid: GridState, center: Int): IntArray {
        val r = center / grid.cols
        val c = center % grid.cols
        val out = mutableSetOf(center)
        Grid.neighbors(r, c, grid.rows, grid.cols, true).forEach { n ->
            if (grid.cells[n] != Grid.STONE) out += n
        }
        return out.filter { grid.cells[it] != Grid.EMPTY }.toIntArray()
    }

    private fun stickyConvert(grid: GridState, placed: Int, code: Int): Pair<GridState, Int> {
        val r = placed / grid.cols
        val c = placed % grid.cols
        val cells = grid.cells.copyOf()
        for (n in Grid.neighbors(r, c, grid.rows, grid.cols, true)) {
            if (cells[n] in Grid.RED..Grid.PURPLE && cells[n] != code) {
                cells[n] = code
                return grid.copy(cells = cells) to 1
            }
        }
        return grid to 0
    }

    private fun findChainPop(grid: GridState): IntArray {
        for (i in grid.cells.indices) {
            val code = grid.cells[i]
            if (code !in Grid.RED..Grid.PURPLE) continue
            val cluster = Match.findCluster(grid, i, code)
            if (cluster.size == 2) return cluster
        }
        return intArrayOf()
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
