package com.sumas.operator.media

data class IceCandidateData(
    val candidate: String,
    val sdpMid: String? = "0",
    val sdpMLineIndex: Int? = 0
)
