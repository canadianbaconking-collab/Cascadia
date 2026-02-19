package dev.frostedlogic.bubbleshooter.core

object GameReducer {
    fun reduce(state: GameState, action: GameAction): GameState = when (action) {
        is GameAction.NewRun -> GameState.new(action.seed)
        is GameAction.MovePlayer -> if (state.phase == Phase.Playing) state.copy(player = state.player.copy(x = action.normalizedX.coerceIn(0.05f, 0.95f))) else state
        GameAction.Shoot -> shoot(state)
        is GameAction.PickBoon -> pickBoon(state, action.boon)
        is GameAction.Tick -> tick(state, action.dt)
    }

    fun startRoom(state: GameState): GameState {
        val (rng, balls) = Rooms.spawn(state.roomIndex, state.rng)
        return state.copy(
            phase = Phase.Playing,
            rng = rng,
            balls = balls,
            projectile = Projectile(active = false, x = state.player.x, y = state.player.y - 0.03f),
            offeredBoons = emptyList(),
            bossTimer = Boss.IMPULSE_INTERVAL,
            lastEvents = emptyList()
        )
    }

    private fun shoot(state: GameState): GameState {
        if (state.phase != Phase.Playing || state.projectile.active || state.shootTimer > 0f) return state
        return state.copy(
            projectile = Projectile(active = true, x = state.player.x, y = state.player.y - 0.02f),
            shootTimer = state.shootCooldown,
            lastEvents = listOf(GameEvent.ShotFired)
        )
    }

    private fun pickBoon(state: GameState, boon: BoonId): GameState {
        if (state.phase != Phase.ChoosingBoon || !state.offeredBoons.contains(boon)) return state
        val boosted = Boons.apply(state, boon)
        return if (boosted.roomIndex >= 8) boosted.copy(phase = Phase.WonRun, lastEvents = listOf(GameEvent.RunWon))
        else startRoom(boosted.copy(roomIndex = boosted.roomIndex + 1))
    }

    private fun tick(state: GameState, dt: Float): GameState {
        if (state.phase != Phase.Playing) return state
        var next = state.copy(
            time = state.time + dt,
            shootTimer = (state.shootTimer - dt).coerceAtLeast(0f),
            player = state.player.copy(invulnTime = (state.player.invulnTime - dt).coerceAtLeast(0f)),
            lastEvents = emptyList()
        )

        val projectile = if (next.projectile.active) {
            val y = next.projectile.y + next.projectile.vy * dt
            if (y < 0f) next.projectile.copy(active = false) else next.projectile.copy(y = y)
        } else next.projectile

        var balls = next.balls.map { Physics.stepBall(it, dt) }
        var projectileAfter = projectile

        if (projectileAfter.active) {
            val hitIndex = balls.indexOfFirst { Physics.projectileHitsBall(projectileAfter, it) }
            if (hitIndex >= 0) {
                val hit = balls[hitIndex]
                val spawned = Physics.split(hit).map {
                    if (next.boons.contains(BoonId.Ice)) it.copy(slowTime = 2f) else it
                }
                balls = balls.toMutableList().also { list ->
                    list.removeAt(hitIndex)
                    list.addAll(spawned)
                }
                projectileAfter = projectileAfter.copy(active = false)
            }
        }

        if (next.player.invulnTime <= 0f && balls.any { Physics.playerHitsBall(next.player, it) }) {
            val player = if (next.player.shieldCharges > 0) {
                next.player.copy(shieldCharges = next.player.shieldCharges - 1, invulnTime = 0.75f)
            } else next.player.copy(hp = next.player.hp - 1, invulnTime = 0.75f)
            next = next.copy(player = player, lastEvents = listOf(GameEvent.HitTaken))
        }

        if (next.roomIndex == 8) {
            val timer = next.bossTimer - dt
            if (timer <= 0f) {
                next = Boss.applyTremor(next)
                next = next.copy(bossTimer = Boss.IMPULSE_INTERVAL)
            } else next = next.copy(bossTimer = timer)
        }

        next = next.copy(projectile = projectileAfter, balls = balls)

        if (next.player.hp <= 0) return next.copy(phase = Phase.LostRun, lastEvents = listOf(GameEvent.RunLost))
        if (next.balls.isEmpty()) {
            val (rng, choice) = Boons.offer(next.rng)
            return next.copy(phase = Phase.ChoosingBoon, rng = rng, offeredBoons = choice.options, lastEvents = listOf(GameEvent.RoomCleared))
        }
        return next
    }
}
