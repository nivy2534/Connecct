package com.example.connecct.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.connecct.ui.viewmodel.Device
import com.example.connecct.Conn.Connection
import android.content.Context

@Composable
fun DeviceCard(
    device: Device,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onConnect: (Device) -> Unit,
    onDisconnect: (Device) -> Unit
) {
    val displayName = device.deviceName.ifBlank { "${device.user}@${device.host}" }
    val ip = device.host
    val statusText = if (device.isOnline) "Online" else "Offline"
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable { onClick() }
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(displayName, style = MaterialTheme.typography.titleMedium)
            Text("IP: ${ip}", style = MaterialTheme.typography.bodyMedium)
            Text("Status: ${statusText}", style = MaterialTheme.typography.bodyMedium)
            Text("Last seen: ${device.lastSeen}", style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(8.dp))
            val isConnected = device.isConnected
            Button(
                onClick ={
                    if(isConnected)onDisconnect(device)
                    else onConnect(device)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if(isConnected) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text(if(isConnected)"Disconnect" else "Connect")
            }

            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                TextButton(onClick = onDelete) {
                    Text("Remove")
                }
            }
        }
    }
}