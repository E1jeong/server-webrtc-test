package com.sumas.operator.model

enum class ConnectionStatus(val label: String) {
    DISCONNECTED("연결 안 됨"),
    CONNECTING("연결 중"),
    CONNECTED("등록 중"),
    REGISTERED("온라인"),
    ERROR("연결 오류")
}
