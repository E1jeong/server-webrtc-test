package com.sumas.operator.model

enum class LogDirection {
    SEND,
    RECV,
    INFO
}

data class EventLog(
    val id: Long,
    val time: String,
    val direction: LogDirection,
    val payload: String
)
