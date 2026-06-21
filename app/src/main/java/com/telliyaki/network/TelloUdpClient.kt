package com.telliyaki.network

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.TimeoutCancellationException
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.SocketException
import java.net.SocketTimeoutException

/**
 * Tello EDU との UDP 通信クライアント。
 *
 * - コマンド送信先: 192.168.10.1:8889
 * - 応答受信: 同じソケットで受信（"ok" / "error" / 数値）
 * - プロトコルは Tello SDK 1.3 のテキストコマンドを前提とする。
 *
 * 通信処理は [Dispatchers.IO] 上で実行される。
 * suspend 関数はコルーチンキャンセルに協調的に対応する。
 */
class TelloUdpClient {
    private val _isConnected = MutableStateFlow(false)
    val isConnectedFlow: StateFlow<Boolean> = _isConnected

    @Volatile
    private var socket: DatagramSocket? = null
    private var telloAddress: InetAddress = InetAddress.getByName(TELLO_IP)

    private val TAG = "TelloUdpClient"

    // 応答受信のソケットタイムアウト（1コマンドあたり）
    private val responseTimeoutMs = 1500

    // connect() の排他制御（同時呼び出しでソケットリークを防ぐ）
    private val connectMutex = Mutex()

    /**
     * Tello 実機への接続状態を返す（同期版）。
     * [BlockEditorScreen] などから呼ばれる。
     */
    fun isConnected(): Boolean = _isConnected.value

    /**
     * Tello に接続し、command モードへ移行する。
     * 事前に端末が Tello の Wi-Fi (192.168.10.x) に接続されている必要がある。
     * 既に接続済み（かつソケットが有効）の場合は何もせずに戻る。
     *
     * @throws TelloException 接続または command モード移行に失敗した場合
     */
    suspend fun connect() {
        connectMutex.withLock {
            // 既に接続済みかつソケットが有効ならスキップ
            if (_isConnected.value && socket?.isClosed == false) return@withLock
            // 古いソケットが残っていれば閉じる
            closeSocket()
            Log.d(TAG, "Connecting to Tello...")
            withContext(Dispatchers.IO) {
                try {
                    // エフェメラルポートを使用（固定ポートの競合を回避。
                    // Tello は送信元ポートに応答を返すため、任意のポートでOK）
                    val s = DatagramSocket().apply {
                        soTimeout = responseTimeoutMs
                    }
                    socket = s

                    // command モードへ移行。応答 "ok" で成功。
                    val response = sendCommandInternal("command")
                    if (response != "ok") {
                        closeSocket()
                        throw TelloException("command モードへの移行に失敗しました (response=$response)")
                    }
                    _isConnected.value = true
                    Log.d(TAG, "Connected to Tello")
                } catch (e: IOException) {
                    closeSocket()
                    throw TelloException("ソケットのオープンに失敗しました", e)
                } catch (e: RuntimeException) {
                    // SecurityException など
                    closeSocket()
                    throw TelloException("ソケットのオープンに失敗しました", e)
                }
            }
        }
    }

    /**
     * ソケットを閉じて接続状態をリセットする。
     */
    fun disconnect() {
        Log.d(TAG, "Disconnecting from Tello...")
        closeSocket()
        _isConnected.value = false
        Log.d(TAG, "Disconnected from Tello")
    }

    /**
     * コマンドを送信し、Tello からの応答を待つ。
     *
     * @param command Tello SDK のコマンド文字列（例: "takeoff", "forward 100"）
     * @return 応答文字列（"ok" / "error" / 数値）
     * @throws TelloException 送受信に失敗、または未接続の場合
     */
    suspend fun sendCommand(command: String): String {
        Log.d(TAG, "Sending command: $command")
        return withContext(Dispatchers.IO) {
            ensureConnected()
            sendCommandInternal(command)
        }
    }

    /**
     * 現在の高度を問い合わせる。
     * @return 高度 [cm]
     * @throws TelloException 応答が数値でない場合
     */
    suspend fun queryHeight(): Int {
        Log.d(TAG, "Querying height...")
        val response = sendCommand("height?")
        return response.toIntOrNull()
            ?: throw TelloException("高度のパースに失敗: $response")
    }

    /**
     * バッテリー残量を問い合わせる。
     * @return バッテリー残量 [%]
     * @throws TelloException 応答が数値でない場合
     */
    suspend fun queryBattery(): Int {
        Log.d(TAG, "Querying battery...")
        val response = sendCommand("battery?")
        return response.toIntOrNull()
            ?: throw TelloException("バッテリーのパースに失敗: $response")
    }

    /**
     * 緊急停止コマンドを送信する。
     * 応答を待たず、例外を投げない（緊急時は確実に送信だけ行う）。
     * UDP送信は非ブロッキングのため非suspendで実装。
     */
    fun emergency() {
        Log.d(TAG, "EMERGENCY STOP!")
        val s = socket ?: run {
            Log.w(TAG, "emergency: socket が null のため送信できません")
            return
        }
        try {
            val data = "emergency".toByteArray()
            val packet = DatagramPacket(data, data.size, telloAddress, TELLO_PORT)
            s.send(packet)
        } catch (e: IOException) {
            Log.e(TAG, "emergency 送信失敗", e)
        }
    }

    // ---- 内部ヘルパー ----

    /**
     * ソケット経由でコマンドを送信し、応答を受信して返す。
     * 応答タイムアウト時は [TelloException] を投げる。
     * コルーチンキャンセル時はソケットを閉じて即座にブロック解除する。
     */
    private suspend fun sendCommandInternal(command: String): String {
        val s = socket ?: throw TelloException("ソケットが未初期化です")

        // 送信
        try {
            val data = command.toByteArray()
            val packet = DatagramPacket(data, data.size, telloAddress, TELLO_PORT)
            s.send(packet)
        } catch (e: IOException) {
            throw TelloException("コマンド送信に失敗: $command", e)
        }

        // 受信：コルーチンキャンセル時はソケットを閉じて receive() のブロックを即座に解除
        return try {
            withTimeout(responseTimeoutMs.toLong()) {
                suspendCancellableCoroutine { cont ->
                    cont.invokeOnCancellation {
                        // キャンセル時にソケットを閉じて receive() のブロックを解除
                        runCatching { s.close() }
                        // 接続状態をリセット（再 connect() を可能にする）
                        _isConnected.value = false
                    }
                    try {
                        val buffer = ByteArray(RESPONSE_BUFFER_SIZE)
                        val packet = DatagramPacket(buffer, buffer.size)
                        s.receive(packet)
                        if (cont.isActive) {
                            cont.resume(String(packet.data, 0, packet.length).trim())
                        }
                    } catch (e: SocketTimeoutException) {
                        if (cont.isActive) {
                            cont.resumeWithException(TelloException("応答タイムアウト: $command"))
                        }
                    } catch (e: SocketException) {
                        if (!cont.isCancelled) {
                            cont.resumeWithException(TelloException("ソケットが閉じられました: $command", e))
                        }
                        // キャンセルの場合は cont が既にキャンセル済みなので何もしない
                    } catch (e: IOException) {
                        if (cont.isActive) {
                            cont.resumeWithException(TelloException("応答受信に失敗: $command", e))
                        }
                    }
                }
            }
        } catch (e: TimeoutCancellationException) {
            throw TelloException("応答タイムアウト: $command")
        }
    }

    private fun ensureConnected() {
        if (socket == null || !_isConnected.value) {
            throw TelloException("Tello に接続されていません")
        }
    }

    private fun closeSocket() {
        socket?.runCatching { close() }
        socket = null
    }

    companion object {
        private const val TELLO_IP = "192.168.10.1"
        private const val TELLO_PORT = 8889
        private const val RESPONSE_BUFFER_SIZE = 256
    }
}

/**
 * Tello 通信に関する例外。
 */
class TelloException(message: String, cause: Throwable? = null) : Exception(message, cause)
