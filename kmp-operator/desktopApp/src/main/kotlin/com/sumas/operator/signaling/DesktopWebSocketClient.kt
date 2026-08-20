package com.sumas.operator.signaling

import com.sumas.operator.model.ConnectionStatus
import com.sumas.operator.model.LogDirection
import com.sumas.operator.model.PeerType
import java.net.URI
import java.net.http.HttpClient
import java.net.http.WebSocket
import java.time.Duration
import java.util.concurrent.CompletionStage

class DesktopWebSocketClient(
    private val httpClient: HttpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .build()
) {
    @Volatile
    private var webSocket: WebSocket? = null

    @Volatile
    private var listener: SignalingListener? = null

    @Volatile
    private var currentOperatorId: String = ""

    val isConnected: Boolean
        get() = webSocket != null

    fun connect(
        serverUrl: String,
        peerId: String,
        listener: SignalingListener,
        peerType: PeerType = PeerType.OPERATOR
    ) {
        disconnect()

        this.listener = listener
        this.currentOperatorId = peerId

        val uri = try {
            val parsed = URI.create(serverUrl.trim())
            val scheme = parsed.scheme?.lowercase()
            if (scheme != "ws" && scheme != "wss") {
                listener.onLog(LogDirection.INFO, "올바르지 않은 WebSocket URL 스킴입니다 (ws:// 또는 wss:// 필요): $serverUrl")
                listener.onConnectionStatusChanged(ConnectionStatus.ERROR)
                return
            }
            parsed
        } catch (e: Exception) {
            listener.onLog(LogDirection.INFO, "URL 형식이 잘못되었습니다: ${e.message}")
            listener.onConnectionStatusChanged(ConnectionStatus.ERROR)
            return
        }

        listener.onLog(LogDirection.INFO, "$serverUrl 연결을 시작합니다.")
        listener.onConnectionStatusChanged(ConnectionStatus.CONNECTING)

        val socketListener = object : WebSocket.Listener {
            private val textBuffer = StringBuilder()

            override fun onOpen(webSocket: WebSocket) {
                this@DesktopWebSocketClient.webSocket = webSocket
                listener.onConnectionStatusChanged(ConnectionStatus.CONNECTED)

                // Send register message automatically
                val registerMessage = SignalingMessage.RegisterMessage(
                    peerId = peerId,
                    peerType = peerType
                )
                send(registerMessage)
                webSocket.request(1)
            }

            override fun onText(webSocket: WebSocket, data: CharSequence, last: Boolean): CompletionStage<*>? {
                textBuffer.append(data)
                if (last) {
                    val fullText = textBuffer.toString()
                    textBuffer.setLength(0)

                    try {
                        val message = SignalingJson.decodeFromString(SignalingMessage.serializer(), fullText)
                        listener.onLog(LogDirection.RECV, fullText)
                        listener.onMessageReceived(message)
                    } catch (e: Exception) {
                        listener.onLog(LogDirection.INFO, "해석할 수 없는 서버 메시지: $fullText")
                    }
                }
                webSocket.request(1)
                return null
            }

            override fun onError(webSocket: WebSocket, error: Throwable) {
                listener.onConnectionStatusChanged(ConnectionStatus.ERROR)
                listener.onLog(LogDirection.INFO, "WebSocket 연결 오류: ${error.message ?: error.toString()}")
            }

            override fun onClose(webSocket: WebSocket, statusCode: Int, reason: String): CompletionStage<*>? {
                if (this@DesktopWebSocketClient.webSocket === webSocket) {
                    this@DesktopWebSocketClient.webSocket = null
                }
                listener.onConnectionStatusChanged(ConnectionStatus.DISCONNECTED)
                listener.onLog(LogDirection.INFO, "연결 종료: $statusCode${if (reason.isNotBlank()) " / $reason" else ""}")
                return null
            }
        }

        try {
            httpClient.newWebSocketBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .buildAsync(uri, socketListener)
                .exceptionally { throwable ->
                    listener.onConnectionStatusChanged(ConnectionStatus.ERROR)
                    listener.onLog(LogDirection.INFO, "서버 연결 실패: ${throwable.message ?: throwable.toString()}")
                    null
                }
        } catch (e: Exception) {
            listener.onConnectionStatusChanged(ConnectionStatus.ERROR)
            listener.onLog(LogDirection.INFO, "연결 초기화 실패: ${e.message ?: e.toString()}")
        }
    }

    fun send(message: SignalingMessage): Boolean {
        val socket = webSocket
        val activeListener = listener
        if (socket == null) {
            activeListener?.onLog(LogDirection.INFO, "WebSocket이 연결되지 않았습니다.")
            return false
        }

        return try {
            val json = SignalingJson.encodeToString(SignalingMessage.serializer(), message)
            socket.sendText(json, true)
            activeListener?.onLog(LogDirection.SEND, json)
            true
        } catch (e: Exception) {
            activeListener?.onLog(LogDirection.INFO, "메시지 전송 실패: ${e.message}")
            false
        }
    }

    fun disconnect() {
        val socket = webSocket
        webSocket = null
        if (socket != null) {
            try {
                socket.sendClose(WebSocket.NORMAL_CLOSURE, "operator disconnected")
            } catch (_: Exception) {
                socket.abort()
            }
        }
    }
}
