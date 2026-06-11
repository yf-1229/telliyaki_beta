package com.telliyaki.network

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class TelloUdpClient {
    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected

    private val _currentHeight = MutableStateFlow(0)
    val currentHeight: StateFlow<Int> = _currentHeight

    private val TAG = "TelloUdpClient"

    // TODO: 実機接続時に実装
    // private var socket: DatagramSocket? = null
    // private val telloIp = "192.168.10.1"
    // private val telloPort = 8889

    suspend fun connect() {
        Log.d(TAG, "Connecting to Tello...")
        // TODO: UDP ソケットを開き、command モードに入る
        // socket = DatagramSocket()
        // sendRaw("command")
        _isConnected.value = true
        Log.d(TAG, "Connected to Tello (stub)")
    }

    fun disconnect() {
        Log.d(TAG, "Disconnecting from Tello...")
        // TODO: ソケットを閉じる
        // socket?.close()
        // socket = null
        _isConnected.value = false
        Log.d(TAG, "Disconnected from Tello (stub)")
    }

    fun sendCommand(command: String) {
        Log.d(TAG, "Sending command: $command")
        // TODO: UDP でコマンドを送信
        // val data = command.toByteArray()
        // val packet = DatagramPacket(data, data.size, InetAddress.getByName(telloIp), telloPort)
        // socket?.send(packet)

        // スタブ: 高度シミュレーション
        simulateCommand(command)
    }

    fun queryHeight(): Int {
        Log.d(TAG, "Querying height...")
        // TODO: height? コマンドを送信して応答を待つ
        // sendRaw("height?")
        // val response = receiveResponse()
        // return response.toIntOrNull() ?: 0

        // スタブ: 現在のシミュレーション高度を返す
        return _currentHeight.value
    }

    fun queryBattery(): Int {
        Log.d(TAG, "Querying battery...")
        // TODO: UDP で battery? 送信 → 応答パース
        // sendRaw("battery?")
        // val response = receiveResponse()
        // return response.toIntOrNull() ?: 0

        // スタブ: 固定値を返す
        return 85
    }

    fun emergency() {
        Log.d(TAG, "EMERGENCY STOP!")
        // TODO: emergency コマンドを送信
        // sendRaw("emergency")
    }

    // スタブ: コマンドをシミュレートして高度を更新
    private fun simulateCommand(command: String) {
        val parts = command.split(" ")
        when (parts[0]) {
            "takeoff" -> {
                _currentHeight.value = 50
            }
            "land" -> {
                _currentHeight.value = 0
            }
            "up" -> {
                val distance = parts.getOrNull(1)?.toIntOrNull() ?: 0
                _currentHeight.value += distance
            }
            "down" -> {
                val distance = parts.getOrNull(1)?.toIntOrNull() ?: 0
                _currentHeight.value = (_currentHeight.value - distance).coerceAtLeast(0)
            }
        }
        Log.d(TAG, "Simulated height: ${_currentHeight.value}cm")
    }
}
