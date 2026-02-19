package dev.frostedlogic.bubbleshooter.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import dev.frostedlogic.bubbleshooter.core.GameAction
import dev.frostedlogic.bubbleshooter.core.GameReducer
import dev.frostedlogic.bubbleshooter.core.GameState
import dev.frostedlogic.bubbleshooter.core.Phase

@Composable
fun GameScreen() {
    var state by remember { mutableStateOf(GameState.new(seed = 1337L)) }

    LaunchedEffect(Unit) {
        var lastNanos = 0L
        var accumulator = 0f
        while (true) {
            withFrameNanos { now ->
                if (lastNanos == 0L) lastNanos = now
                val frameDt = ((now - lastNanos).coerceAtMost(50_000_000L)) / 1_000_000_000f
                lastNanos = now
                accumulator += frameDt
                while (accumulator >= dev.frostedlogic.bubbleshooter.core.World.DT) {
                    state = GameReducer.reduce(state, GameAction.Tick(dev.frostedlogic.bubbleshooter.core.World.DT))
                    accumulator -= dev.frostedlogic.bubbleshooter.core.World.DT
                }
            }
        }
    }

    MaterialTheme {
        Column(
            modifier = Modifier.fillMaxSize().background(Color(0xFF0B1020)).padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Seed ${state.seed}  Room ${state.roomIndex}/8", color = Color.White)
            Text("HP ${state.player.hp}  Balls ${state.balls.size}  Boons ${state.boons.joinToString()}", color = Color.White)
            if (state.roomIndex == 8 && state.phase == Phase.Playing) Text("Boss: Tremor", color = Color(0xFFFFB86B))

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(520.dp)
                    .pointerInput(state.phase) {
                        detectDragGestures(
                            onDragStart = { pos -> state = GameReducer.reduce(state, GameAction.MovePlayer(normalizeX(pos, size.width))) },
                            onDrag = { change, _ -> state = GameReducer.reduce(state, GameAction.MovePlayer(normalizeX(change.position, size.width))) }
                        )
                    }
                    .pointerInput(state.phase) {
                        detectTapGestures { state = GameReducer.reduce(state, GameAction.Shoot) }
                    }
            ) { renderGame(this, state) }

            if (state.phase == Phase.ChoosingBoon) {
                Text("Pick a boon", color = Color.White)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.offeredBoons.forEach { boon ->
                        Button(onClick = { state = GameReducer.reduce(state, GameAction.PickBoon(boon)) }) { Text(boon.name) }
                    }
                }
            }

            if (state.phase == Phase.WonRun || state.phase == Phase.LostRun) {
                Text(if (state.phase == Phase.WonRun) "Run Won" else "Run Lost", color = Color.White)
                Button(onClick = { state = GameReducer.reduce(state, GameAction.NewRun(state.seed)) }) { Text("Retry") }
            }
        }
    }
}
