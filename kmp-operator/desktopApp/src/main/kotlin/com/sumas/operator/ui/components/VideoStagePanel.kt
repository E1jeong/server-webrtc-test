package com.sumas.operator.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sumas.operator.model.CallState
import kotlinx.coroutines.delay

@Composable
fun VideoStagePanel(
    videoFrame: ImageBitmap?,
    callState: CallState,
    callStatusMessage: String,
    onHangup: () -> Unit,
    onToggleMicrophoneMute: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "02 · Video",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "음성·영상 통화",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Call Actions
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    when (callState) {
                        is CallState.InCall -> {
                            val isMuted = callState.isMicrophoneMuted
                            val isMediaReady = callState.isMediaReady

                            OutlinedButton(
                                onClick = onToggleMicrophoneMute,
                                enabled = isMediaReady,
                                shape = RoundedCornerShape(4.dp),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Text(
                                    text = if (isMuted) "마이크 켜기" else "마이크 끄기",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            OutlinedButton(
                                onClick = onHangup,
                                shape = RoundedCornerShape(4.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = Color(0xFFD32F2F)
                                ),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 0.dp),
                                modifier = Modifier.height(28.dp),
                            ) {
                                Text(
                                    text = "통화 종료",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        is CallState.Calling -> {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFFFFA000).copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "● 연결 시도 중",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = Color(0xFFE65100)
                                )
                            }

                            OutlinedButton(
                                onClick = onHangup,
                                shape = RoundedCornerShape(4.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = Color(0xFFD32F2F)
                                ),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 0.dp),
                                modifier = Modifier.height(28.dp),
                            ) {
                                Text(
                                    text = "통화 취소",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        CallState.Idle -> {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    text = callStatusMessage,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Main Video Stage Area: 9:16 Portrait Black Box that fills vertical height and is completely filled by the video
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .aspectRatio(9f / 16f)
                    .align(Alignment.CenterHorizontally)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF081218))
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (videoFrame != null) {
                    // 1. Live Video Stream (Completely fills the 9:16 area without any side bars)
                    Image(
                        bitmap = videoFrame,
                        contentDescription = "단말 영상",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )

                    // Video overlay details (Timer & Peer badge)
                    if (callState is CallState.InCall) {
                        var elapsedSeconds by remember(callState.callId) { mutableStateOf(0L) }
                        LaunchedEffect(callState.callId) {
                            elapsedSeconds = 0L
                            while (true) {
                                delay(1000)
                                elapsedSeconds++
                            }
                        }
                        val minutes = elapsedSeconds / 60
                        val seconds = elapsedSeconds % 60
                        val timerText = "%02d:%02d".format(minutes, seconds)

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp)
                        ) {
                            // Top-Left: Live badge
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color.Black.copy(alpha = 0.7f),
                                modifier = Modifier.align(Alignment.TopStart)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF00E676))
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "${callState.peerId} · LIVE",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }

                            // Top-Right: Call Timer
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color.Black.copy(alpha = 0.7f),
                                modifier = Modifier.align(Alignment.TopEnd)
                            ) {
                                Text(
                                    text = timerText,
                                    color = Color(0xFF64B5F6),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                } else {
                    // 2. Video Placeholders
                    when (callState) {
                        is CallState.Calling -> {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(24.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(36.dp),
                                    color = Color(0xFFFFA000),
                                    strokeWidth = 3.dp
                                )
                                Spacer(modifier = Modifier.height(14.dp))
                                Text(
                                    text = "${callState.targetPeerId} 응답 대기 중...",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFFB74D),
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Call ID: ${callState.callId}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        is CallState.InCall -> {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(24.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(36.dp),
                                    color = Color(0xFF64B5F6),
                                    strokeWidth = 3.dp
                                )
                                Spacer(modifier = Modifier.height(14.dp))
                                Text(
                                    text = "단말 영상 스트림 연결 중...",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF90CAF9)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${callState.peerId} · ${callState.statusText}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        CallState.Idle -> {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(24.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.size(56.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "📹",
                                            fontSize = 24.sp
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(14.dp))
                                Text(
                                    text = "단말 영상 대기",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "단말 목록에서 통화 요청을 보내면 실시간 영상이 여기에 표시됩니다.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }

        }
    }
}
