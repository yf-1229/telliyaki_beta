package com.telliyaki.ui.screen

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.BatteryStd
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.telliyaki.data.BlocklyCommand
import com.telliyaki.ui.viewmodel.ExecutionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExecutionScreen(
    commands: List<BlocklyCommand>,
    executionViewModel: ExecutionViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val batteryLevel = executionViewModel.batteryLevel

    LaunchedEffect(commands) {
        executionViewModel.startExecution(commands)
    }

    LaunchedEffect(executionViewModel.logs.size) {
        if (executionViewModel.logs.isNotEmpty()) {
            listState.animateScrollToItem(executionViewModel.logs.size - 1)
        }
    }

    val borderModifier = if (batteryLevel <= 40) {
        val borderColor = when {
            batteryLevel <= 20 -> Color.Red
            else -> Color.Yellow
        }
        Modifier.border(4.dp, borderColor)
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .then(borderModifier)
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("実行中") },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "戻る")
                        }
                    }
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = when {
                            executionViewModel.isEmergencyStopped -> "緊急停止"
                            executionViewModel.isExecuting -> "実行中..."
                            else -> "完了"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            executionViewModel.isEmergencyStopped -> MaterialTheme.colorScheme.error
                            executionViewModel.isExecuting -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.tertiary
                        }
                    )
                    Text(
                        text = "コマンド: ${commands.size}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (batteryLevel <= 40) Icons.Default.BatteryAlert else Icons.Default.BatteryStd,
                        contentDescription = "バッテリー",
                        tint = when {
                            batteryLevel <= 20 -> Color.Red
                            batteryLevel <= 40 -> Color.Yellow
                            else -> MaterialTheme.colorScheme.onSurface
                        }
                    )
                    Text(
                        text = "バッテリー: ${batteryLevel}%",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            batteryLevel <= 20 -> Color.Red
                            batteryLevel <= 40 -> Color.Yellow
                            else -> MaterialTheme.colorScheme.onSurface
                        }
                    )
                }

                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(executionViewModel.logs) { log ->
                        Text(
                            text = log,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = { executionViewModel.emergencyStop() },
                    enabled = executionViewModel.isExecuting && !executionViewModel.isEmergencyStopped,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Default.Stop, contentDescription = null)
                    Spacer(modifier = Modifier.padding(4.dp))
                    Text(
                        text = "緊急停止",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
