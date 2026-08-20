package com.sumas.operator.state

import com.sumas.operator.model.CallState
import com.sumas.operator.model.ConnectionStatus
import com.sumas.operator.model.EventLog
import com.sumas.operator.model.Peer
import com.sumas.operator.model.PeerType

data class OperatorState(
    val operatorId: String = "operator-test-01",
    val serverUrl: String = "ws://localhost:8080/ws",
    val connectionStatus: ConnectionStatus = ConnectionStatus.DISCONNECTED,
    val peers: List<Peer> = emptyList(),
    val callState: CallState = CallState.Idle,
    val callStatusMessage: String = "통화 대기",
    val logs: List<EventLog> = emptyList(),
    val callCounter: Int = 0,
    val logCounter: Long = 0L
) {
    val devices: List<Peer>
        get() = peers.filter { it.peerType == PeerType.DEVICE }
}
