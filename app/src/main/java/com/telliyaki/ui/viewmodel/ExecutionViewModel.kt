package com.telliyaki.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.telliyaki.data.BlocklyCommand
import com.telliyaki.network.TelloException
import com.telliyaki.network.TelloUdpClient
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

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

    @Volatile
    private var emergencyFlag = false
    private var batteryMonitorJob: Job? = null
    private var executionJob: Job? = null
    private val logMutex = Mutex()

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

        // Tello に接続（command モードへ移行）
        try {
            addLog("Tello に接続中...")
            telloClient.connect()
            addLog("Tello に接続しました")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            addLog("Tello への接続に失敗しました: ${e.message}")
            isExecuting = false
            return
        }

        // バッテリー監視を開始
        startBatteryMonitor()

        try {
            for (command in commands) {
                if (emergencyFlag) {
                    addLog("緊急停止されました")
                    break
                }
                try {
                    executeCommand(command)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: TelloException) {
                    addLog("通信エラー: ${e.message}")
                    break
                }
            }

            if (!emergencyFlag) {
                addLog("すべてのコマンドを実行しました")
            }
        } finally {
            // バッテリー監視を停止
            stopBatteryMonitor()
            isExecuting = false
            // disconnect は onCleared で行う（接続状態を維持）
        }
    }

    private fun startBatteryMonitor() {
        batteryMonitorJob?.cancel()
        batteryMonitorJob = viewModelScope.launch {
            // 初回クエリを即座に実行（失敗時はデフォルト値のまま）
            try {
                batteryLevel = telloClient.queryBattery()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // 初回取得失敗は継続
            }
            while (true) {
                delay(30_000) // 30秒ごとに更新
                try {
                    batteryLevel = telloClient.queryBattery()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    // バッテリー取得失敗は続行（次回再試行）
                }
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

        when (command) {
            is BlocklyCommand.TakeOff -> {
                sendCommandChecked("takeoff")
                cancellableDelay(2000)
            }
            is BlocklyCommand.Land -> {
                sendCommandChecked("land")
                cancellableDelay(2000)
            }
            is BlocklyCommand.MoveForward -> {
                sendCommandChecked("forward ${command.distance}")
                cancellableDelay((command.distance * 10L).coerceAtLeast(500L))
            }
            is BlocklyCommand.MoveBack -> {
                sendCommandChecked("back ${command.distance}")
                cancellableDelay((command.distance * 10L).coerceAtLeast(500L))
            }
            is BlocklyCommand.MoveLeft -> {
                sendCommandChecked("left ${command.distance}")
                cancellableDelay((command.distance * 10L).coerceAtLeast(500L))
            }
            is BlocklyCommand.MoveRight -> {
                sendCommandChecked("right ${command.distance}")
                cancellableDelay((command.distance * 10L).coerceAtLeast(500L))
            }
            is BlocklyCommand.MoveUp -> {
                sendCommandChecked("up ${command.distance}")
                cancellableDelay((command.distance * 10L).coerceAtLeast(500L))
            }
            is BlocklyCommand.MoveDown -> {
                sendCommandChecked("down ${command.distance}")
                cancellableDelay((command.distance * 10L).coerceAtLeast(500L))
            }
            is BlocklyCommand.RotateCW -> {
                sendCommandChecked("cw ${command.degrees}")
                cancellableDelay((command.degrees * 10L).coerceAtLeast(500L))
            }
            is BlocklyCommand.RotateCCW -> {
                sendCommandChecked("ccw ${command.degrees}")
                cancellableDelay((command.degrees * 10L).coerceAtLeast(500L))
            }
            is BlocklyCommand.Wait -> cancellableDelay(command.milliseconds.toLong())
            is BlocklyCommand.IfAltitude -> {
                val currentHeight = telloClient.queryHeight()
                val condition = evaluateCondition(currentHeight, command.threshold, command.operator)
                val branch = if (condition) command.trueBranch else command.falseBranch
                for (cmd in branch) {
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

        if (!emergencyFlag) {
            addLog("完了: ${command.toDisplayString()}")
        }
    }

    private fun evaluateCondition(current: Int, threshold: Int, operator: String): Boolean {
        return when (operator) {
            "GT" -> current > threshold
            "LT" -> current < threshold
            "GE" -> current >= threshold
            "LE" -> current <= threshold
            else -> false
        }
    }

    /**
     * コマンドを送信し、応答が "ok" であることを検証する。
     * "error" や予期しない応答の場合は [TelloException] を投げ、
     * 呼び出し元の try/catch で処理を中断できるようにする。
     */
    private suspend fun sendCommandChecked(command: String) {
        val response = telloClient.sendCommand(command)
        if (response != "ok") {
            throw TelloException("コマンド拒否: $command (response=$response)")
        }
    }

    private suspend fun cancellableDelay(totalMillis: Long) {
        val chunk = 100L
        var remaining = totalMillis
        while (remaining > 0) {
            delay(minOf(chunk, remaining))
            remaining -= chunk
        }
    }

    fun emergencyStop() {
        emergencyFlag = true
        isEmergencyStopped = true
        isExecuting = false
        executionJob?.cancel()
        stopBatteryMonitor()
        telloClient.emergency()
    }

    private suspend fun addLog(message: String) {
        logMutex.withLock {
            logs = logs + message
        }
    }

    fun clearLogs() {
        logs = emptyList()
    }

    override fun onCleared() {
        super.onCleared()
        if (isExecuting && !isEmergencyStopped) {
            telloClient.emergency()
            // emergency パケットがカーネル送信バッファから確実に送出されるよう
            // ソケットクローズ前に短い待機を入れる
            try {
                Thread.sleep(50)
            } catch (_: InterruptedException) {
                Thread.currentThread().interrupt()
            }
        }
        stopBatteryMonitor()
        executionJob?.cancel()
        telloClient.disconnect()
    }
}
