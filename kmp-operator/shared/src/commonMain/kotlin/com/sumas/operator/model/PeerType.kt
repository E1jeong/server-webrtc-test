package com.sumas.operator.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class PeerType {
    @SerialName("device")
    DEVICE,

    @SerialName("operator")
    OPERATOR;

    val value: String
        get() = when (this) {
            DEVICE -> "device"
            OPERATOR -> "operator"
        }
}
