package com.telliyaki.network

import android.util.Log

class TelloUdpClient {
    private var currentHeight = 0

    private val TAG = "TelloUdpClient"

    // TODO: 実機接続時に実装
    // private var socket: DatagramSocket? = null
    // private val telloIp = "192.168.10.1"
    // private val telloPort = 8889

    /**
     * Tello実機への接続状態を確認
     * 現在はスタブ実装（常に false）
     * TODO: UDPでbattery?コマンドを送信し、応答があるか確認する
     */
    fun isConnected(): Boolean {
        // TODO: 実機接続時に UDP 接続テストを実装
        // return try {
        //     sendRaw("battery?")
        //     val response = receiveResponse(timeout = 1000)
        //     response != null && response.toIntOrNull() != null
        // } catch (e: Exception) {
        //     false
        // }
        return false
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
        return currentHeight
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
                currentHeight = 50
            }
            "land" -> {
                currentHeight = 0
            }
            "up" -> {
                val distance = parts.getOrNull(1)?.toIntOrNull() ?: 0
                currentHeight += distance
            }
            "down" -> {
                val distance = parts.getOrNull(1)?.toIntOrNull() ?: 0
                currentHeight = (currentHeight - distance).coerceAtLeast(0)
            }
        }
        Log.d(TAG, "Simulated height: ${currentHeight}cm")
    }
}
