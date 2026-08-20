package com.sumas.operator.signaling

import com.sumas.operator.model.ConnectionStatus
import com.sumas.operator.model.LogDirection

interface SignalingListener {
    fun onConnectionStatusChanged(status: ConnectionStatus)
    fun onMessageReceived(message: SignalingMessage)
    fun onLog(direction: LogDirection, payload: String)
}
