package com.sumas.operator.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sumas.operator.model.ConnectionStatus

@Composable
fun ConnectionPanel(
    serverUrl: String,
    operatorId: String,
    connectionStatus: ConnectionStatus,
    onUrlChange: (String) -> Unit,
    onOperatorIdChange: (String) -> Unit,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isConnectedOrConnecting = connectionStatus == ConnectionStatus.CONNECTING ||
            connectionStatus == ConnectionStatus.CONNECTED ||
            connectionStatus == ConnectionStatus.REGISTERED

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text(
                        text = "01 · Signaling",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "서버 연결",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = "Signaling 서버에 Operator Peer로 등록합니다.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = serverUrl,
                    onValueChange = onUrlChange,
                    label = { Text("WebSocket URL") },
                    placeholder = { Text("ws://localhost:8080/ws") },
                    singleLine = true,
                    enabled = !isConnectedOrConnecting,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { if (!isConnectedOrConnecting) onConnect() }),
                    modifier = Modifier.weight(3f)
                )

                Spacer(modifier = Modifier.width(12.dp))

                OutlinedTextField(
                    value = operatorId,
                    onValueChange = onOperatorIdChange,
                    label = { Text("Operator ID") },
                    placeholder = { Text("operator-test-01") },
                    singleLine = true,
                    enabled = !isConnectedOrConnecting,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { if (!isConnectedOrConnecting) onConnect() }),
                    modifier = Modifier.weight(2f)
                )

                Spacer(modifier = Modifier.width(12.dp))

                if (isConnectedOrConnecting) {
                    OutlinedButton(
                        onClick = onDisconnect,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFFD32F2F)
                        ),
                        modifier = Modifier.height(56.dp)
                    ) {
                        Text("연결 해제", fontWeight = FontWeight.Bold)
                    }
                } else {
                    Button(
                        onClick = onConnect,
                        shape = RoundedCornerShape(8.dp),
                        enabled = serverUrl.isNotBlank() && operatorId.isNotBlank(),
                        modifier = Modifier.height(56.dp)
                    ) {
                        Text(
                            text = if (connectionStatus == ConnectionStatus.CONNECTING) "연결 중..." else "서버 연결",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}
