package com.telliyaki.ui.screen

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.telliyaki.data.BlocklyCommand
import kotlin.math.cos
import kotlin.math.sin

data class DroneState(
    val x: Float = 0f,
    val y: Float = 0f,
    val heading: Float = 0f
)

fun simulateCommands(commands: List<BlocklyCommand>): List<DroneState> {
    val states = mutableListOf<DroneState>()
    var current = DroneState()
    states.add(current)

    fun processCommand(command: BlocklyCommand) {
        val headingRad = Math.toRadians(current.heading.toDouble())
        current = when (command) {
            is BlocklyCommand.TakeOff -> current
            is BlocklyCommand.Land -> current
            is BlocklyCommand.MoveForward -> {
                val dx = (command.distance * sin(headingRad)).toFloat()
                val dy = -(command.distance * cos(headingRad)).toFloat()
                current.copy(x = current.x + dx, y = current.y + dy)
            }
            is BlocklyCommand.MoveBack -> {
                val dx = (command.distance * sin(headingRad)).toFloat()
                val dy = -(command.distance * cos(headingRad)).toFloat()
                current.copy(x = current.x - dx, y = current.y - dy)
            }
            is BlocklyCommand.MoveLeft -> {
                val leftRad = Math.toRadians((current.heading - 90).toDouble())
                val dx = (command.distance * sin(leftRad)).toFloat()
                val dy = -(command.distance * cos(leftRad)).toFloat()
                current.copy(x = current.x + dx, y = current.y + dy)
            }
            is BlocklyCommand.MoveRight -> {
                val rightRad = Math.toRadians((current.heading + 90).toDouble())
                val dx = (command.distance * sin(rightRad)).toFloat()
                val dy = -(command.distance * cos(rightRad)).toFloat()
                current.copy(x = current.x + dx, y = current.y + dy)
            }
            is BlocklyCommand.MoveUp -> current
            is BlocklyCommand.MoveDown -> current
            is BlocklyCommand.RotateCW -> current.copy(heading = current.heading + command.degrees)
            is BlocklyCommand.RotateCCW -> current.copy(heading = current.heading - command.degrees)
            is BlocklyCommand.Wait -> current
            is BlocklyCommand.IfAltitude -> {
                // シミュレーション: true分岐のみ実行
                command.trueBranch.forEach { processCommand(it) }
                current
            }
            is BlocklyCommand.Repeat -> {
                repeat(command.times) {
                    command.body.forEach { processCommand(it) }
                }
                current
            }
            is BlocklyCommand.Sequence -> {
                command.children.forEach { processCommand(it) }
                current
            }
        }
        states.add(current)
    }

    for (command in commands) {
        processCommand(command)
    }

    return states
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreviewScreen(
    commands: List<BlocklyCommand>,
    onBackClick: () -> Unit,
    onExecuteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val states = remember(commands) { simulateCommands(commands) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("プレビュー") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "戻る")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onExecuteClick) {
                Icon(Icons.Default.PlayArrow, contentDescription = "実行")
            }
        },
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            DroneCanvas(
                states = states,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(16.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "コマンド数: ${commands.size}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "最終位置: (${states.lastOrNull()?.x?.toInt() ?: 0}, ${states.lastOrNull()?.y?.toInt() ?: 0})",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun DroneCanvas(
    states: List<DroneState>,
    modifier: Modifier = Modifier
) {
    val gridColor = Color(0xFFE0E0E0)
    val trajectoryColor = MaterialTheme.colorScheme.primary
    val droneColor = MaterialTheme.colorScheme.error

    Canvas(modifier = modifier) {
        val centerX = size.width / 2
        val centerY = size.height / 2
        val scale = 2f

        drawGrid(centerX, centerY, scale, gridColor)

        if (states.size > 1) {
            for (i in 0 until states.size - 1) {
                val from = states[i]
                val to = states[i + 1]
                drawLine(
                    color = trajectoryColor,
                    start = Offset(centerX + from.x * scale, centerY + from.y * scale),
                    end = Offset(centerX + to.x * scale, centerY + to.y * scale),
                    strokeWidth = 3f
                )
            }
        }

        val lastState = states.lastOrNull() ?: DroneState()
        drawDrone(
            centerX = centerX + lastState.x * scale,
            centerY = centerY + lastState.y * scale,
            heading = lastState.heading,
            color = droneColor,
            size = 20f
        )

        drawCircle(
            color = Color.Gray,
            radius = 4f,
            center = Offset(centerX, centerY)
        )
    }
}

private fun DrawScope.drawGrid(
    centerX: Float,
    centerY: Float,
    scale: Float,
    color: Color
) {
    val gridSpacing = 50f * scale
    val strokeWidth = 1f

    var x = centerX % gridSpacing
    while (x < size.width) {
        drawLine(
            color = color,
            start = Offset(x, 0f),
            end = Offset(x, size.height),
            strokeWidth = strokeWidth
        )
        x += gridSpacing
    }

    var y = centerY % gridSpacing
    while (y < size.height) {
        drawLine(
            color = color,
            start = Offset(0f, y),
            end = Offset(size.width, y),
            strokeWidth = strokeWidth
        )
        y += gridSpacing
    }
}

private fun DrawScope.drawDrone(
    centerX: Float,
    centerY: Float,
    heading: Float,
    color: Color,
    size: Float
) {
    rotate(heading, Offset(centerX, centerY)) {
        val path = Path().apply {
            moveTo(centerX, centerY - size)
            lineTo(centerX - size * 0.6f, centerY + size * 0.6f)
            lineTo(centerX + size * 0.6f, centerY + size * 0.6f)
            close()
        }
        drawPath(path = path, color = color)
        drawPath(
            path = path,
            color = Color.Black,
            style = Stroke(width = 2f)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewScreenPreview() {
    val commands = listOf(
        BlocklyCommand.TakeOff,
        BlocklyCommand.MoveForward(100),
        BlocklyCommand.RotateCW(90),
        BlocklyCommand.MoveForward(50),
        BlocklyCommand.Land
    )
    PreviewScreen(
        commands = commands,
        onBackClick = {},
        onExecuteClick = {}
    )
}
