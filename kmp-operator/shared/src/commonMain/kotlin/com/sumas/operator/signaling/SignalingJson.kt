package com.sumas.operator.signaling

import kotlinx.serialization.json.Json

val SignalingJson: Json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
    isLenient = true
}
