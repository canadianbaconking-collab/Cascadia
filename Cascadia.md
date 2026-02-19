
# Bubble Shooter Roguelite — Cascadia

## Intent
Build an Android-first, ultra-accessible Bubble Shooter roguelite where:
- Base game is instantly understood (shoot colors, match 3, pop).
- Roguelite layer adds *shot modifiers and board rules*, not sprawling systems.
- Runs are short (2–4 minutes) and replayable.
- Deterministic by seed (offline, no telemetry).

---

## North Star
**The base game must require effectively zero tutorial.**
If a mechanic needs explanation beyond an icon + one sentence, it is probably out for MVP.

---

## MVP Scope (One Evening)

### Core 30-second loop
1) Enter a room board.
2) You have **N shots** (start at 12).
3) Shoot bubble → attaches to grid.
4) If you create a **cluster of 3+ same color**, it pops.
5) Bubbles disconnected from the top **fall** (floating rule).
6) Room clears when **Goals remaining = 0**.
7) Choose **1 of 3 boons** → next room.

### Win/Lose
- Win room: clear all **Goal bubbles**.
- Lose run: shots reach 0 with goals remaining.
- Run completion: clear **8 rooms** (Room 8 is a boss-modified room).

### Determinism
- Each run uses a **seed**.
- Seed drives:
  - room layouts / template selection
  - initial board colors
  - next-bubble sequence
  - boon offerings

No external calls. No analytics. No uploads.


## Project Skeleton (required)

### Package
Use package: `dev.frostedlogic.bubbleshooter`

### File tree (authoritative)
app/src/main/java/dev/frostedlogic/bubbleshooter/
  MainActivity.kt
  ui/
    GameScreen.kt
    Render.kt
    Input.kt
  core/
    GameState.kt
    GameReducer.kt
    Actions.kt
    Events.kt
    RNG.kt
    Grid.kt
    Match.kt
    Rooms.kt
    Boons.kt
    Boss.kt

### Core rules
- `core/` MUST NOT import Compose or Android UI classes.
- `ui/` MAY import Compose; it must only read state + dispatch actions.

### Class + type names (authoritative)

core/GameState.kt
- data class `GameState(
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
    val lastEvents: List<GameEvent>
  )`
- enum class `Phase { Playing, ChoosingBoon, WonRun, LostRun }`

core/Actions.kt
- sealed interface `GameAction`
  - data class `AimChanged(val angleRad: Float) : GameAction`
  - object `Shoot : GameAction`
  - data class `PickBoon(val boon: BoonId) : GameAction`
  - object `RerollBoons : GameAction` (may be stubbed)
  - object `NextRoom : GameAction`
  - data class `NewRun(val seed: Long) : GameAction`

core/GameReducer.kt
- object `GameReducer`
  - fun `reduce(state: GameState, action: GameAction): GameState`

core/RNG.kt
- data class `RngState(val s0: Long, val s1: Long)`
- object `Rng`
  - fun `fromSeed(seed: Long): RngState`
  - fun `nextInt(state: RngState, bound: Int): Pair<RngState, Int>`
  - fun `nextFloat01(state: RngState): Pair<RngState, Float>`

core/Grid.kt
- data class `GridState(val rows: Int, val cols: Int, val cells: IntArray, val topAnchored: Boolean = true)`
- enum class `BubbleColor { Red, Green, Blue, Goal }`
- data class `Bubble(val color: BubbleColor, val isGoal: Boolean = false)`
- object `Grid`
  - fun `empty(rows: Int, cols: Int): GridState`
  - fun `get(grid: GridState, r: Int, c: Int): Int`  // returns cell code
  - fun `set(grid: GridState, r: Int, c: Int, code: Int): GridState`
  - fun `neighbors(r: Int, c: Int, rows: Int, cols: Int, staggered: Boolean = true): IntArray`

core/Match.kt
- object `Match`
  - fun `findCluster(grid: GridState, startIndex: Int, matchCode: Int): IntArray`
  - fun `clearCluster(grid: GridState, indices: IntArray): GridState`
  - fun `findTopConnected(grid: GridState): BooleanArray`
  - fun `clearFloating(grid: GridState, connected: BooleanArray): Pair<GridState, IntArray>` // returns new grid + cleared indices

core/Rooms.kt
- data class `RoomSpec(val rows: Int, val cols: Int, val goalCount: Int, val stoneCount: Int, val colors: Int, val shots: Int, val bossFog: Boolean)`
- object `Rooms`
  - fun `roomSpec(state: GameState): RoomSpec`
  - fun `generateRoom(rng: RngState, spec: RoomSpec): Pair<RngState, GridState>`
  - fun `countGoals(grid: GridState): Int`

core/Boons.kt
- enum class `BoonId { ShotsPlus, Sights, BombShot, Paint, Sticky }`
- data class `ChoiceState(val options: List<BoonId>, val canReroll: Boolean = false)`
- object `Boons`
  - fun `offer(rng: RngState, owned: Set<BoonId>): Pair<RngState, ChoiceState>`
  - fun `applyOnRoomStart(state: GameState): GameState` (may be stubbed)
  - fun `applyOnShoot(state: GameState): GameState` (hook point; may be stubbed)

core/Boss.kt
- data class `BossState(val fogActive: Boolean)`
- object `Boss`
  - fun `applyRoomModifiers(state: GameState, room: RoomSpec): GameState`

core/Events.kt
- sealed interface `GameEvent`
  - data class `Popped(val indices: IntArray) : GameEvent`
  - data class `Fell(val indices: IntArray) : GameEvent`
  - object `ShotFired : GameEvent`
  - data class `RoomCleared(val roomIndex: Int) : GameEvent`
  - object `RunWon : GameEvent`
  - object `RunLost : GameEvent`

ui/GameScreen.kt
- @Composable `GameScreen()`: owns `GameState` + calls reducer on actions
- uses a fixed timestep loop (e.g., `LaunchedEffect` + frame clock) ONLY for animations; core game state is action-driven

ui/Render.kt
- fun `renderGame(canvas: DrawScope, state: GameState, aimAngleRad: Float)` (or similar)
- Must draw: grid bubbles, shooter bubble, next bubble, HUD

ui/Input.kt
- pointer input to compute aim angle, dispatch AimChanged, dispatch Shoot on release

### Notes / allowed simplifications for Cycle 0
- You may implement placement by snapping to nearest empty cell along a ray; keep it deterministic.
- If physics ray-to-cell feels heavy, use a simpler rule: choose column by aim angle and fill lowest available slot (acceptable for MVP).
- BombShot/Sticky can be stubs if needed; ShotsPlus + Sights + Paint are the priority boons.

## Board Model

### Grid
- Use a bubble shooter style grid (staggered rows recommended), but keep it simple.
- Each cell may contain a bubble or be empty.
- Bubble attaches to nearest valid cell along the aim ray.

### Matching
- After placement, run a flood-fill from the placed bubble to find same-color connected cluster.
- If cluster size >= 3 → pop all in cluster.

### Floating rule
- After pops, find all bubbles connected to the **top row** (or an implicit top anchor).
- Any bubbles not connected fall and are removed (with fall animation).

---

## Bubble Types (MVP)
**MVP minimum:** 3 colors + Goal.

1) Normal colored bubble: R/G/B (expand later).
2) Goal bubble: must be removed to clear room.
   - Easiest: Goal bubble is a special bubble that is removed if it becomes part of a popped cluster
     OR if it is adjacent to a popped cluster (choose one behavior and keep consistent).

**Optional for later cycles**
- Stone bubble: blocks; removed only by bomb-type effects.
- Bomb bubble: pops neighbors.
- Wildcard bubble: matches any color.

---

## Input + UI (Android-first)
- Drag to aim, release to shoot.
- Aim line shows trajectory with **1 wall bounce** (preview).
- HUD:
  - Room #
  - Shots remaining
  - Goals remaining
- Bottom:
  - Shooter + current bubble
  - Next bubble preview
  - (optional) boon buttons like Paint if acquired

---

## Boons (Start with 5 implemented)
Boons must be small, legible, and synergistic.

### Implement in MVP (5)
1) **+2 Shots**
2) **Sights**: show 2-bounce preview line (upgrade over 1-bounce)
3) **Bomb Shot**: every 5th shot becomes a bomb (radius 1)
4) **Paint**: once per room, change current bubble color (button)
5) **Sticky**: on placement, convert 1 adjacent bubble to your color if present

### Future boons
- Pierce (passes through 1 bubble)
- Chain (pop adjacent size-2 clusters)
- Reroll boon choices (1 per run)
- Magnet (slight aim assist to nearest matching cluster)

---

## Boss Modifier (Room 8 MVP)
Pick 1 boss modifier and ship it.

**Boss: Fog**
- Preview line is hidden.
- If the player has Sights boon, preview is visible (Sights counters Fog).

---

## Architecture (important)
### Hard rule: keep game logic pure Kotlin (no Compose dependencies)
- `GameState` holds all data.
- `reduce(state, action) -> newState` is deterministic.
- RNG is a deterministic seeded generator owned by the state.
- Renderer (Compose Canvas) reads state and draws it.
- Input generates actions (AimChanged, ShootPressed, PickBoon, etc).
- FX (sounds/particles) triggered by emitted events, not by logic code calling Android APIs.

This keeps an “escape hatch” to move rendering to an engine later if needed.

---

## One-Evening Definition of Done
A run is playable end-to-end:
- Start new seeded run
- Clear rooms 1–8 OR lose by running out of shots
- Boon selection appears after each room
- Boss modifier applies on room 8
- Win/Lose screen includes: seed, rooms cleared, boons taken, goals cleared

Minimum feel requirements:
- Pop animation (scale + fade)
- Falling bubbles animate down
- Aim line is readable

---

# The Sane Iteration Loop (Token-Burner That Doesn’t Create a Feature Monster)

## Goal
Use the LLM to generate *spike-sized hypotheses* that can be tested quickly.
Never let it create a giant backlog of interacting systems.

### Candidate Board (max 8 items)
Each candidate includes:
- Hypothesis: what “fun” improves
- ComplexityCost (1–5)
- InteractionRisk (1–5)
- ProofTest: how we’ll know it worked
- Status: Proposed / Implemented / Cut / Merged

### Budget Gate (per cycle)
- Complexity sum <= 7
- Interaction sum <= 7
- Pick: **2 features + 1 polish/debt task**
- Mandatory: play at least **2 full runs** before next cycle.

### Fun Metrics (3)
- TTFF (time to first satisfying pop) <= 10 seconds
- Replay pull: desire to retry within 3 seconds of losing
- Clarity: can explain loss reason in one sentence

### Kill Rule (real simplification)
If complexity increases, remove or merge an *implemented* system, not just “delete backlog items.”

---

## First 3 Cycles (example)
### Cycle 0 (MVP)
- Implement: core loop + 5 boons OR cut down to 3 boons if time
- Polish: juice + aim readability

### Cycle 1
- Feature: Stone bubbles
- Feature: Bomb Shot boon
- Polish: goal highlighting / readability

### Cycle 2
- Feature: Shop every 3 rooms (optional currency)
- Feature: Wildcard boon
- Polish: loss clarity screen (“why you lost”)

### Cycle 3
- Feature: more boss modifiers (2–3 total)
- Feature: Paint boon UI polish + balance
- Polish: aim assist tuning (optional)

---

## Constraints (non-negotiable)
- No new “systems” without retiring or merging another system.
- No feature that needs a tutorial paragraph.
- Prefer “board rule” bosses over AI enemies.
- Keep runs short; don’t add meta-progression until core is addictive.

---

## Codex Tasks (what to do next)
1) Create Android project using Kotlin + Compose.
2) Implement pure Kotlin game core (`GameState`, reducer, RNG, grid, matching, floating).
3) Implement Compose Canvas renderer + input.
4) Add boon selection UI.
5) Add seed display and new-run seeded restart.

