package com.example.connecct.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.connecct.ui.viewmodel.SSHKeys

@Composable
fun KeyCard(
    key: SSHKeys,
    onDelete: (SSHKeys) -> Unit,
    onClick: (SSHKeys) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable{onClick(key)}
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(key.name, fontWeight = FontWeight.Bold)
            Text("Type: ${key.type}")
            Text("Added: ${key.addedAt}")
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = { onDelete(key) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text("Delete")
            }
        }
    }
}