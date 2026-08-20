package com.sumas.operator.state

import com.sumas.operator.model.CallState
import com.sumas.operator.model.ConnectionStatus
import com.sumas.operator.model.LogDirection
import com.sumas.operator.signaling.DesktopWebSocketClient
import com.sumas.operator.signaling.SignalingListener
import com.sumas.operator.signaling.SignalingMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class DesktopOperatorManager(
    initialState: OperatorState = OperatorState(),
    private val client: DesktopWebSocketClient = DesktopWebSocketClient()
) : SignalingListener {

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val _state = MutableStateFlow(initialState)
    val state: StateFlow<OperatorState> = _state.asStateFlow()

    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")

    fun dispatch(action: OperatorAction) {
        _state.update { currentState ->
            OperatorReducer.reduce(currentState, action)
        }
    }

    fun updateServerUrl(url: String) {
        dispatch(OperatorAction.UpdateServerUrl(url))
    }

    fun updateOperatorId(id: String) {
        dispatch(OperatorAction.UpdateOperatorId(id))
    }

    fun connect() {
        val currentState = _state.value
        client.connect(currentState.serverUrl, currentState.operatorId, this)
    }

    fun disconnect() {
        client.disconnect()
        dispatch(OperatorAction.UpdateConnectionStatus(ConnectionStatus.DISCONNECTED))
    }

    fun invite(targetDeviceId: String) {
        val currentCall = _state.value.callState
        if (currentCall !is CallState.Idle) {
            hangup()
        }

        dispatch(OperatorAction.SendInvite(targetDeviceId))
        val nextCall = _state.value.callState
        if (nextCall is CallState.Calling) {
            client.send(
                SignalingMessage.CallInviteMessage(
                    callId = nextCall.callId,
                    to = targetDeviceId
                )
            )
        }
    }

    fun hangup() {
        when (val call = _state.value.callState) {
            is CallState.Calling -> {
                client.send(
                    SignalingMessage.CallHangupMessage(
                        callId = call.callId,
                        to = call.targetPeerId
                    )
                )
            }
            is CallState.InCall -> {
                client.send(
                    SignalingMessage.CallHangupMessage(
                        callId = call.callId,
                        to = call.peerId
                    )
                )
            }
            CallState.Idle -> Unit
        }
        dispatch(OperatorAction.Hangup)
    }

    fun clearLogs() {
        dispatch(OperatorAction.ClearLogs)
    }

    override fun onConnectionStatusChanged(status: ConnectionStatus) {
        dispatch(OperatorAction.UpdateConnectionStatus(status))
    }

    override fun onMessageReceived(message: SignalingMessage) {
        dispatch(OperatorAction.ReceiveMessage(message))
        if (message is SignalingMessage.RegisteredMessage) {
            // Request full peer list for redundancy matching operator-web behavior
            client.send(SignalingMessage.PeerListMessage())
        }
    }

    override fun onLog(direction: LogDirection, payload: String) {
        val time = LocalTime.now().format(timeFormatter)
        dispatch(OperatorAction.AddLog(time = time, direction = direction, payload = payload))
    }

    fun cleanup() {
        disconnect()
        scope.cancel()
    }
}
