package com.sumas.operator.state

import com.sumas.operator.model.CallState
import com.sumas.operator.model.ConnectionStatus
import com.sumas.operator.model.EventLog
import com.sumas.operator.model.Peer
import com.sumas.operator.signaling.SignalingMessage

object OperatorReducer {
    private const val MAX_LOGS = 80

    fun reduce(state: OperatorState, action: OperatorAction): OperatorState {
        return when (action) {
            is OperatorAction.UpdateServerUrl -> state.copy(serverUrl = action.url)
            is OperatorAction.UpdateOperatorId -> state.copy(operatorId = action.id)
            is OperatorAction.UpdateConnectionStatus -> {
                val isCleared = action.status == ConnectionStatus.DISCONNECTED || action.status == ConnectionStatus.ERROR
                state.copy(
                    connectionStatus = action.status,
                    peers = if (isCleared) emptyList() else state.peers,
                    callState = if (isCleared) CallState.Idle else state.callState
                )
            }
            is OperatorAction.ReceiveMessage -> handleMessage(state, action.message)
            is OperatorAction.SendInvite -> {
                val nextCounter = state.callCounter + 1
                val callId = "call-${state.operatorId}-$nextCounter"
                state.copy(
                    callCounter = nextCounter,
                    callState = CallState.Calling(
                        targetPeerId = action.targetDeviceId,
                        callId = callId
                    )
                )
            }
            is OperatorAction.Hangup -> state.copy(callState = CallState.Idle)
            is OperatorAction.ToggleMicrophoneMute -> {
                when (val call = state.callState) {
                    is CallState.InCall -> state.copy(callState = call.copy(isMicrophoneMuted = action.isMuted))
                    else -> state
                }
            }
            is OperatorAction.UpdateCallStatusText -> {
                when (val call = state.callState) {
                    is CallState.InCall -> state.copy(callState = call.copy(statusText = action.statusText))
                    else -> state
                }
            }
            is OperatorAction.AddLog -> {
                val nextLogId = state.logCounter + 1
                val entry = EventLog(
                    id = nextLogId,
                    time = action.time,
                    direction = action.direction,
                    payload = action.payload
                )
                state.copy(
                    logCounter = nextLogId,
                    logs = (listOf(entry) + state.logs).take(MAX_LOGS)
                )
            }
            is OperatorAction.ClearLogs -> state.copy(logs = emptyList())
        }
    }

    private fun handleMessage(state: OperatorState, message: SignalingMessage): OperatorState {
        return when (message) {
            is SignalingMessage.RegisteredMessage -> {
                state.copy(
                    connectionStatus = ConnectionStatus.REGISTERED,
                    peers = message.peers.sortedBy { it.peerId }
                )
            }
            is SignalingMessage.PeerListMessage -> {
                state.copy(peers = message.peers.sortedBy { it.peerId })
            }
            is SignalingMessage.PeerOnlineMessage -> {
                val updated = (state.peers.filterNot { it.peerId == message.peerId } + Peer(message.peerId, message.peerType))
                    .sortedBy { it.peerId }
                state.copy(peers = updated)
            }
            is SignalingMessage.PeerOfflineMessage -> {
                val updated = state.peers.filterNot { it.peerId == message.peerId }
                val targetOffline = when (val call = state.callState) {
                    is CallState.Calling -> call.targetPeerId == message.peerId
                    is CallState.InCall -> call.peerId == message.peerId
                    CallState.Idle -> false
                }
                state.copy(
                    peers = updated,
                    callState = if (targetOffline) CallState.Idle else state.callState
                )
            }
            is SignalingMessage.CallAcceptMessage -> {
                when (val call = state.callState) {
                    is CallState.Calling -> {
                        if (call.callId == message.callId) {
                            val peer = message.from ?: call.targetPeerId
                            state.copy(
                                callState = CallState.InCall(
                                    peerId = peer,
                                    callId = message.callId,
                                    statusText = "통화 연결됨"
                                )
                            )
                        } else {
                            state
                        }
                    }
                    else -> state
                }
            }
            is SignalingMessage.CallRejectMessage -> {
                val isMyCall = when (val call = state.callState) {
                    is CallState.Calling -> call.callId == message.callId
                    is CallState.InCall -> call.callId == message.callId
                    CallState.Idle -> false
                }
                if (isMyCall) state.copy(callState = CallState.Idle) else state
            }
            is SignalingMessage.CallHangupMessage -> {
                val isMyCall = when (val call = state.callState) {
                    is CallState.Calling -> call.callId == message.callId
                    is CallState.InCall -> call.callId == message.callId
                    CallState.Idle -> false
                }
                if (isMyCall) state.copy(callState = CallState.Idle) else state
            }
            is SignalingMessage.ErrorMessage -> {
                state.copy(connectionStatus = ConnectionStatus.ERROR)
            }
            else -> state
        }
    }
}
