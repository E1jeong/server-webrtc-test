package com.sumas.operator.model

import kotlinx.serialization.Serializable

@Serializable
data class Peer(
    val peerId: String,
    val peerType: PeerType
)
