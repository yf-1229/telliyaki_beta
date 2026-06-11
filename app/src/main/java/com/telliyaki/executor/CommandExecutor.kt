package com.telliyaki.executor

import com.telliyaki.data.BlocklyCommand
import com.telliyaki.network.TelloUdpClient
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class CommandExecutor(
    private val telloClient: TelloUdpClient
) {
    private val _isExecuting = MutableStateFlow(false)
    val isExecuting: StateFlow<Boolean> = _isExecuting

    private val _logs = MutableStateFlow<List<String>>(emptyList())
    val logs: StateFlow<List<String>> = _logs

    private val _isEmergencyStopped = MutableStateFlow(false)
    val isEmergencyStopped: StateFlow<Boolean> = _isEmergencyStopped

    private var emergencyFlag = false

    suspend fun execute(commands: List<BlocklyCommand>) {
        _isExecuting.value = true
        _isEmergencyStopped.value = false
        emergencyFlag = false
        _logs.value = emptyList()

        for (command in commands) {
            if (emergencyFlag) {
                addLog("緊急停止されました")
                break
            }
            executeCommand(command)
        }

        _isExecuting.value = false
        if (!emergencyFlag) {
            addLog("すべてのコマンドを実行しました")
        }
    }

    private suspend fun executeCommand(command: BlocklyCommand) {
        if (emergencyFlag) return

        addLog("実行中: ${command.toDisplayString()}")

        when (command) {
            is BlocklyCommand.TakeOff -> {
                telloClient.sendCommand("takeoff")
                delay(2000)
            }
            is BlocklyCommand.Land -> {
                telloClient.sendCommand("land")
                delay(2000)
            }
            is BlocklyCommand.MoveForward -> {
                telloClient.sendCommand("forward ${command.distance}")
                delay(1000)
            }
            is BlocklyCommand.MoveBack -> {
                telloClient.sendCommand("back ${command.distance}")
                delay(1000)
            }
            is BlocklyCommand.MoveLeft -> {
                telloClient.sendCommand("left ${command.distance}")
                delay(1000)
            }
            is BlocklyCommand.MoveRight -> {
                telloClient.sendCommand("right ${command.distance}")
                delay(1000)
            }
            is BlocklyCommand.MoveUp -> {
                telloClient.sendCommand("up ${command.distance}")
                delay(1000)
            }
            is BlocklyCommand.MoveDown -> {
                telloClient.sendCommand("down ${command.distance}")
                delay(1000)
            }
            is BlocklyCommand.RotateCW -> {
                telloClient.sendCommand("cw ${command.degrees}")
                delay(1000)
            }
            is BlocklyCommand.RotateCCW -> {
                telloClient.sendCommand("ccw ${command.degrees}")
                delay(1000)
            }
            is BlocklyCommand.Wait -> {
                delay(command.milliseconds.toLong())
            }
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

        addLog("完了: ${command.toDisplayString()}")
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

    fun emergencyStop() {
        emergencyFlag = true
        _isEmergencyStopped.value = true
        telloClient.emergency()
    }

    private fun addLog(message: String) {
        _logs.value = _logs.value + message
    }

    fun clearLogs() {
        _logs.value = emptyList()
    }
}
