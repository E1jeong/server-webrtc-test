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
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "01 · Signaling",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "서버 연결",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                OutlinedTextField(
                    value = serverUrl,
                    onValueChange = onUrlChange,
                    label = { Text("WebSocket URL", fontSize = 11.sp) },
                    placeholder = { Text("ws://localhost:8080/ws", fontSize = 11.sp) },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodySmall,
                    enabled = !isConnectedOrConnecting,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { if (!isConnectedOrConnecting) onConnect() }),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = operatorId,
                    onValueChange = onOperatorIdChange,
                    label = { Text("Operator ID", fontSize = 11.sp) },
                    placeholder = { Text("operator-test-01", fontSize = 11.sp) },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodySmall,
                    enabled = !isConnectedOrConnecting,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { if (!isConnectedOrConnecting) onConnect() }),
                    modifier = Modifier.fillMaxWidth()
                )

                if (isConnectedOrConnecting) {
                    OutlinedButton(
                        onClick = onDisconnect,
                        shape = RoundedCornerShape(6.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFFD32F2F)
                        ),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 0.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                    ) {
                        Text("연결 해제", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                } else {
                    Button(
                        onClick = onConnect,
                        shape = RoundedCornerShape(6.dp),
                        enabled = serverUrl.isNotBlank() && operatorId.isNotBlank(),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 0.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                    ) {
                        Text(
                            text = if (connectionStatus == ConnectionStatus.CONNECTING) "연결 중..." else "서버 연결",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}
