package com.example.connecct.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.connecct.ui.viewmodel.Device

@Composable
fun DeviceCard(
    device: Device,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable { onClick() }
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(device.name, style = MaterialTheme.typography.titleMedium)
            Text("IP: ${device.ip}", style = MaterialTheme.typography.bodyMedium)
            Text("Status: ${device.status}")
            Spacer(Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                TextButton(onClick = onDelete) {
                    Text("Remove")
                }
            }
        }
    }
}