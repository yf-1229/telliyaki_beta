package com.telliyaki.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.telliyaki.data.BlocklyCommand
import com.telliyaki.network.TelloUdpClient
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ExecutionViewModel(
    private val telloClient: TelloUdpClient = TelloUdpClient()
) : ViewModel() {
    var logs by mutableStateOf<List<String>>(emptyList())
        private set

    var isExecuting by mutableStateOf(false)
        private set

    var isEmergencyStopped by mutableStateOf(false)
        private set

    var batteryLevel by mutableStateOf(100)
        private set

    private var emergencyFlag = false
    private var batteryMonitorJob: Job? = null
    private var executionJob: Job? = null

    fun startExecution(commands: List<BlocklyCommand>) {
        if (isExecuting) return
        isExecuting = true
        executionJob?.cancel()
        executionJob = viewModelScope.launch {
            executeCommands(commands)
        }
    }

    private suspend fun executeCommands(commands: List<BlocklyCommand>) {
        isEmergencyStopped = false
        emergencyFlag = false
        logs = emptyList()

        // バッテリー監視を開始
        startBatteryMonitor()

        try {
            for (command in commands) {
                if (emergencyFlag) {
                    addLog("緊急停止されました")
                    break
                }
                executeCommand(command)
            }

            if (!emergencyFlag) {
                addLog("すべてのコマンドを実行しました")
            }
        } finally {
            // バッテリー監視を停止
            stopBatteryMonitor()
            isExecuting = false
        }
    }

    private fun startBatteryMonitor() {
        batteryMonitorJob?.cancel()
        batteryLevel = telloClient.queryBattery()
        batteryMonitorJob = viewModelScope.launch {
            while (true) {
                delay(30_000) // 30秒ごとに更新
                batteryLevel = telloClient.queryBattery()
            }
        }
    }

    private fun stopBatteryMonitor() {
        batteryMonitorJob?.cancel()
        batteryMonitorJob = null
    }

    private suspend fun executeCommand(command: BlocklyCommand) {
        if (emergencyFlag) return

        addLog("実行中: ${command.toDisplayString()}")

        // TODO: 実際のTelloへのUDP送信をここに実装
        when (command) {
            is BlocklyCommand.TakeOff -> delay(2000)
            is BlocklyCommand.Land -> delay(2000)
            is BlocklyCommand.MoveForward -> delay((command.distance * 10L).coerceAtLeast(500L))
            is BlocklyCommand.MoveBack -> delay((command.distance * 10L).coerceAtLeast(500L))
            is BlocklyCommand.MoveLeft -> delay((command.distance * 10L).coerceAtLeast(500L))
            is BlocklyCommand.MoveRight -> delay((command.distance * 10L).coerceAtLeast(500L))
            is BlocklyCommand.MoveUp -> delay((command.distance * 10L).coerceAtLeast(500L))
            is BlocklyCommand.MoveDown -> delay((command.distance * 10L).coerceAtLeast(500L))
            is BlocklyCommand.RotateCW -> delay((command.degrees * 10L).coerceAtLeast(500L))
            is BlocklyCommand.RotateCCW -> delay((command.degrees * 10L).coerceAtLeast(500L))
            is BlocklyCommand.Wait -> delay(command.milliseconds.toLong())
            is BlocklyCommand.IfAltitude -> {
                // シミュレーション: 常にtrue分岐を実行
                for (cmd in command.trueBranch) {
                    if (emergencyFlag) break
                    executeCommand(cmd)
                }
            }
            is BlocklyCommand.Repeat -> {
                repeat(command.times) {
                    if (emergencyFlag) return
                    for (cmd in command.body) {
                        if (emergencyFlag) break
                        executeCommand(cmd)
                    }
                }
            }
            is BlocklyCommand.Sequence -> {
                for (cmd in command.children) {
                    if (emergencyFlag) break
                    executeCommand(cmd)
                }
            }
        }

        addLog("完了: ${command.toDisplayString()}")
    }

    fun emergencyStop() {
        emergencyFlag = true
        isEmergencyStopped = true
        isExecuting = false
        executionJob?.cancel()
        stopBatteryMonitor()
        telloClient.emergency()
    }

    private fun addLog(message: String) {
        logs = logs + message
    }

    fun clearLogs() {
        logs = emptyList()
    }

    override fun onCleared() {
        super.onCleared()
        if (isExecuting && !isEmergencyStopped) {
            telloClient.emergency()
        }
        stopBatteryMonitor()
        executionJob?.cancel()
    }
}
