package com.example.connecct.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.connecct.ui.viewmodel.SSHKey

@Composable
fun KeyCard(
    key: SSHKey,
    onDelete: (SSHKey) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(key.name, fontWeight = FontWeight.Bold)
            Text("Type: ${key.type}")
            Text("Added: ${key.addedAt}")
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = { onDelete(key) }) {
                Text("Delete")
            }
        }
    }
}