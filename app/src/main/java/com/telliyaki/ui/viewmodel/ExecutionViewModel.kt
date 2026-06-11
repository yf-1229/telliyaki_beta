package com.telliyaki.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.telliyaki.data.BlocklyCommand
import com.telliyaki.network.TelloUdpClient
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

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
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    suspend fun executeCommands(commands: List<BlocklyCommand>) {
        isExecuting = true
        isEmergencyStopped = false
        emergencyFlag = false
        logs = emptyList()

        // バッテリー監視を開始
        startBatteryMonitor()

        for (command in commands) {
            if (emergencyFlag) {
                addLog("緊急停止されました")
                break
            }
            executeCommand(command)
        }

        // バッテリー監視を停止
        stopBatteryMonitor()

        isExecuting = false
        if (!emergencyFlag) {
            addLog("すべてのコマンドを実行しました")
        }
    }

    private fun startBatteryMonitor() {
        batteryMonitorJob?.cancel()
        batteryMonitorJob = scope.launch {
            while (true) {
                batteryLevel = telloClient.queryBattery()
                delay(30_000) // 30秒ごとに更新
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
            is BlocklyCommand.TakeOff -> kotlinx.coroutines.delay(2000)
            is BlocklyCommand.Land -> kotlinx.coroutines.delay(2000)
            is BlocklyCommand.MoveForward -> kotlinx.coroutines.delay(1000)
            is BlocklyCommand.MoveBack -> kotlinx.coroutines.delay(1000)
            is BlocklyCommand.MoveLeft -> kotlinx.coroutines.delay(1000)
            is BlocklyCommand.MoveRight -> kotlinx.coroutines.delay(1000)
            is BlocklyCommand.MoveUp -> kotlinx.coroutines.delay(1000)
            is BlocklyCommand.MoveDown -> kotlinx.coroutines.delay(1000)
            is BlocklyCommand.RotateCW -> kotlinx.coroutines.delay(1000)
            is BlocklyCommand.RotateCCW -> kotlinx.coroutines.delay(1000)
            is BlocklyCommand.Wait -> kotlinx.coroutines.delay(command.milliseconds.toLong())
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
        // TODO: 実際のTelloへのemergencyコマンド送信
    }

    private fun addLog(message: String) {
        logs = logs + message
    }

    fun clearLogs() {
        logs = emptyList()
    }
}
