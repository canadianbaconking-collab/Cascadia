package dev.frostedlogic.bubbleshooter.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import dev.frostedlogic.bubbleshooter.core.GameAction
import dev.frostedlogic.bubbleshooter.core.GameReducer
import dev.frostedlogic.bubbleshooter.core.GameState
import dev.frostedlogic.bubbleshooter.core.Phase
import dev.frostedlogic.bubbleshooter.core.Rooms

@Composable
fun GameScreen() {
    var state by remember { mutableStateOf(GameState.new(seed = 1337L)) }

    MaterialTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF121212))
                .padding(8.dp)
        ) {
            Text("Seed ${state.seed} • Room ${state.roomIndex}", color = Color.White)
            Text("Shots ${state.shotsLeft} • Goals ${Rooms.countGoals(state.grid)}", color = Color.White)
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(520.dp)
                    .pointerInput(state.phase) {
                        detectDragGestures(
                            onDragStart = { pos ->
                                val origin = Offset(size.width / 2f, size.height)
                                val angle = computeAimAngle(origin, pos)
                                state = GameReducer.reduce(state, GameAction.AimChanged(angle))
                            },
                            onDrag = { change, _ ->
                                val origin = Offset(size.width / 2f, size.height)
                                val angle = computeAimAngle(origin, change.position)
                                state = GameReducer.reduce(state, GameAction.AimChanged(angle))
                            },
                            onDragEnd = {
                                state = GameReducer.reduce(state, GameAction.Shoot)
                            }
                        )
                    }
            ) {
                renderGame(this, state, state.aimAngleRad)
            }

            if (state.phase == Phase.ChoosingBoon) {
                Text("Choose a boon", color = Color.White)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.pendingChoice?.options.orEmpty().forEach { boon ->
                        Button(onClick = { state = GameReducer.reduce(state, GameAction.PickBoon(boon)) }) {
                            Text(boon.name)
                        }
                    }
                }
            }

            if (state.phase == Phase.WonRun || state.phase == Phase.LostRun) {
                Text(
                    if (state.phase == Phase.WonRun) "Run Won!" else "Run Lost",
                    color = Color.White,
                    modifier = Modifier.padding(top = 8.dp)
                )
                Text("Rooms cleared ${state.roomsCleared} • Goals cleared ${state.goalsCleared}", color = Color.White)
                Text("Boons: ${state.boons.joinToString()}", color = Color.White)
                Button(onClick = { state = GameReducer.reduce(state, GameAction.NewRun(state.seed + 1)) }) {
                    Text("New Seeded Run")
                }
            }
        }
    }
}
