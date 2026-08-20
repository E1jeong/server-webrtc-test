package com.sumas.operator.state

import com.sumas.operator.media.DesktopMediaController
import com.sumas.operator.media.DesktopMediaListener
import com.sumas.operator.media.webrtc.WebrtcDesktopMediaController
import com.sumas.operator.model.CallState
import com.sumas.operator.model.ConnectionStatus
import com.sumas.operator.model.LogDirection
import com.sumas.operator.signaling.DesktopWebSocketClient
import com.sumas.operator.signaling.SignalingListener
import com.sumas.operator.signaling.SignalingMessage
import androidx.compose.ui.graphics.ImageBitmap
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
    private val client: DesktopWebSocketClient = DesktopWebSocketClient(),
    val mediaController: DesktopMediaController = WebrtcDesktopMediaController()
) : SignalingListener, DesktopMediaListener {

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val _state = MutableStateFlow(initialState)
    val state: StateFlow<OperatorState> = _state.asStateFlow()
    val remoteVideoFrame: StateFlow<ImageBitmap?> get() = mediaController.remoteVideoFrame

    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")

    init {
        mediaController.setListener(this)
    }

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
        mediaController.stopCall()
        dispatch(OperatorAction.UpdateConnectionStatus(ConnectionStatus.DISCONNECTED))
    }

    fun invite(targetDeviceId: String) {
        val currentCall = _state.value.callState
        if (currentCall !is CallState.Idle) {
            hangup()
        } else {
            mediaController.stopCall()
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
        mediaController.stopCall()
        dispatch(OperatorAction.Hangup)
    }

    fun toggleMicrophoneMute() {
        when (val call = _state.value.callState) {
            is CallState.InCall -> {
                val nextMuted = !call.isMicrophoneMuted
                dispatch(OperatorAction.ToggleMicrophoneMute(nextMuted))
                mediaController.setMicrophoneMuted(nextMuted)
            }
            else -> Unit
        }
    }

    fun clearLogs() {
        dispatch(OperatorAction.ClearLogs)
    }

    override fun onConnectionStatusChanged(status: ConnectionStatus) {
        if (status == ConnectionStatus.DISCONNECTED || status == ConnectionStatus.ERROR) {
            mediaController.stopCall()
        }
        dispatch(OperatorAction.UpdateConnectionStatus(status))
    }

    override fun onMessageReceived(message: SignalingMessage) {
        dispatch(OperatorAction.ReceiveMessage(message))
        when (message) {
            is SignalingMessage.RegisteredMessage -> {
                // Request full peer list for redundancy matching operator-web behavior
                client.send(SignalingMessage.PeerListMessage())
            }
            is SignalingMessage.CallAcceptMessage -> {
                val currentCall = _state.value.callState
                if (currentCall is CallState.InCall && currentCall.callId == message.callId) {
                    mediaController.startCall(callId = message.callId, targetDeviceId = currentCall.peerId)
                }
            }
            is SignalingMessage.WebRtcAnswerMessage -> {
                val currentCall = _state.value.callState
                if (currentCall is CallState.InCall && currentCall.callId == message.callId) {
                    if (message.from == null || message.from == currentCall.peerId) {
                        mediaController.handleRemoteAnswer(callId = message.callId, sdp = message.sdp)
                    }
                }
            }
            is SignalingMessage.WebRtcIceMessage -> {
                val currentCall = _state.value.callState
                if (currentCall is CallState.InCall && currentCall.callId == message.callId) {
                    if (message.from == null || message.from == currentCall.peerId) {
                        mediaController.handleRemoteIceCandidate(
                            callId = message.callId,
                            candidate = message.candidate,
                            sdpMid = message.sdpMid,
                            sdpMLineIndex = message.sdpMLineIndex
                        )
                    }
                }
            }
            is SignalingMessage.CallHangupMessage,
            is SignalingMessage.CallRejectMessage -> {
                mediaController.stopCall()
            }
            is SignalingMessage.PeerOfflineMessage -> {
                val currentCall = _state.value.callState
                if (currentCall is CallState.Idle) {
                    mediaController.stopCall()
                }
            }
            is SignalingMessage.ErrorMessage -> {
                if (message.code != "peer_offline") {
                    mediaController.stopCall()
                }
            }
            else -> Unit
        }
    }

    override fun onLog(direction: LogDirection, payload: String) {
        val time = LocalTime.now().format(timeFormatter)
        dispatch(OperatorAction.AddLog(time = time, direction = direction, payload = payload))
    }

    override fun onLocalOffer(callId: String, targetDeviceId: String, sdp: String) {
        val currentCall = _state.value.callState
        if (currentCall is CallState.InCall && currentCall.callId == callId) {
            client.send(
                SignalingMessage.WebRtcOfferMessage(
                    callId = callId,
                    to = targetDeviceId,
                    sdp = sdp
                )
            )
        }
    }

    override fun onLocalIceCandidate(
        callId: String,
        targetDeviceId: String,
        candidate: String,
        sdpMid: String?,
        sdpMLineIndex: Int?
    ) {
        val currentCall = _state.value.callState
        if (currentCall is CallState.InCall && currentCall.callId == callId) {
            client.send(
                SignalingMessage.WebRtcIceMessage(
                    callId = callId,
                    to = targetDeviceId,
                    candidate = candidate,
                    sdpMid = sdpMid,
                    sdpMLineIndex = sdpMLineIndex
                )
            )
        }
    }

    override fun onMediaStatusChanged(statusText: String) {
        dispatch(OperatorAction.UpdateCallStatusText(statusText))
    }

    override fun onMediaReadyChanged(isReady: Boolean) {
        dispatch(OperatorAction.UpdateMediaReady(isReady))
    }

    override fun onError(error: String) {
        val time = LocalTime.now().format(timeFormatter)
        dispatch(OperatorAction.AddLog(time = time, direction = LogDirection.INFO, payload = "미디어 오류: $error"))
        hangup()
    }

    fun cleanup() {
        mediaController.setListener(null)
        mediaController.stopCall()
        (mediaController as? WebrtcDesktopMediaController)?.release()
        disconnect()
        scope.cancel()
    }
}
