package com.example.connecct.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.connecct.ui.viewmodel.ConnectionViewModel
import com.example.connecct.util.TransferStatus
import com.example.connecct.util.TransferTask
import com.example.connecct.util.TransferType

@Composable
fun TransferQueueScreen(
    viewModel: ConnectionViewModel
) {
    val queue by viewModel.transerQueue.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        Text(
            text = "Transfer Queue",
            style = MaterialTheme.typography.titleLarge
        )

        Spacer(Modifier.height(12.dp))

        if (queue.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("No transfer tasks")
            }
        } else {
            LazyColumn {
                items(queue, key = { it.id }) { task ->
                    TransferQueueItem(task)
                }
            }
        }
    }
}

@Composable
fun TransferQueueItem(task: TransferTask) {

    val icon = when (task.type) {
        TransferType.UPLOAD -> Icons.Default.Upload
        TransferType.DOWNLOAD -> Icons.Default.Download
    }

    val statusText = when (task.status) {
        TransferStatus.QUEUED -> "Queued"
        TransferStatus.IN_PROGRESS -> "In Progress"
        TransferStatus.COMPLETED -> "Completed"
        TransferStatus.FAILED -> "Failed"
        TransferStatus.CANCELED -> "Canceled"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {

            // 🔹 BARIS ATAS
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(icon, contentDescription = null)

                Spacer(Modifier.width(8.dp))

                Column(Modifier.weight(1f)) {
                    Text(
                        text = task.fileName,
                        maxLines = 1
                    )
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                if (task.status == TransferStatus.IN_PROGRESS) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                }
            }

            // 🔹 PROGRESS
            if (task.status == TransferStatus.IN_PROGRESS ||
                task.status == TransferStatus.COMPLETED
            ) {
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = (task.progress.coerceIn(0, 100)) / 100f,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // 🔹 ERROR
            if (task.status == TransferStatus.FAILED && task.error != null) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = task.error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

