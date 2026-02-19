# Cascadia — Bubble Shooter Roguelite (Android, Kotlin + Compose)

Cascadia is an Android-first bubble-shooter roguelite prototype based on `Cascadia.md`.
It keeps the core controls simple (drag to aim, release to shoot), then layers in deterministic
roguelite progression (seeded rooms + boon choices).

## Current feature set

1. **Seeded deterministic runs** for room generation, bubble sequence, and boon offers.
2. **8-room run goal** with run win at room 8 and loss on zero shots.
3. **Match-3 popping** via flood-fill cluster detection.
4. **Floating clear rule** (non-top-connected bubbles fall and clear).
5. **Goal bubbles** tracked per room for clear conditions.
6. **Goal-adjacency pop behavior** (goals touching a popped cluster also clear).
7. **Boss fog modifier** on room 8 hiding aim preview.
8. **Sights boon interaction** that counters fog and shows extra preview guidance.
9. **+2 Shots boon** applied on room start.
10. **Bomb Shot boon** (every 5th shot clears radius-1 area).
11. **Sticky boon** converting one adjacent bubble to shot color on placement.
12. **Paint boon** with once-per-room color cycling button.
13. **One-time boon reroll** support in boon-choice phase.
14. **Stone bubbles** generated in later rooms as blockers.
15. **Room template variation** for different board silhouettes.

## Project structure

```
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
```

- `core/` is pure Kotlin logic (no Compose dependencies).
- `ui/` reads state and dispatches actions into the reducer.

## Build

### Requirements
- JDK 17
- Android SDK / Build-Tools compatible with AGP 8.5.x

### Command

```bash
gradle :app:assembleDebug
```

> If your environment is missing SDK components, Gradle will fail before compilation.

## Gameplay notes

- Drag on the board to adjust aim.
- Release drag to shoot.
- Clear all goal bubbles to clear the room.
- Pick one boon after each room clear.
- Reach room 8 and clear it to win the run.

## Known MVP limitations

- Animation hooks exist conceptually via events, but full pop/fall animation timelines are not yet implemented.
- Aim preview is simplified and not full physics with accurate wall bounces.
- Some boons are intentionally lightweight implementations for iteration speed.
