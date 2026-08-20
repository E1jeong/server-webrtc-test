package com.sumas.operator.state

import com.sumas.operator.model.ConnectionStatus
import com.sumas.operator.model.LogDirection
import com.sumas.operator.signaling.SignalingMessage

sealed interface OperatorAction {
    data class UpdateServerUrl(val url: String) : OperatorAction
    data class UpdateOperatorId(val id: String) : OperatorAction
    data class UpdateConnectionStatus(val status: ConnectionStatus) : OperatorAction
    data class ReceiveMessage(val message: SignalingMessage) : OperatorAction
    data class SendInvite(val targetDeviceId: String) : OperatorAction
    data object Hangup : OperatorAction
    data class ToggleMicrophoneMute(val isMuted: Boolean) : OperatorAction
    data class UpdateCallStatusText(val statusText: String) : OperatorAction
    data class AddLog(val time: String, val direction: LogDirection, val payload: String) : OperatorAction
    data object ClearLogs : OperatorAction
}
