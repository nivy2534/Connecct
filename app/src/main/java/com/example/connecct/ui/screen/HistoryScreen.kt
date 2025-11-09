package com.example.connecct.ui.screen

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.connecct.ui.viewmodel.HistoryViewModel
import com.example.connecct.ui.viewmodel.SSHConnectionHistory
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(viewModel: HistoryViewModel = viewModel()) {
    val context = LocalContext.current
    val historyList by viewModel.connectionHistory.collectAsState()

    val grouped = remember(historyList) {
        historyList.groupBy {
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                .format(Date(it.timestamp))
        }
    }
    val sortedDates = grouped.keys.sortedDescending()

    Scaffold(
        containerColor = Color.Black,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "History",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = MaterialTheme.typography.headlineMedium.fontSize
                        )
                        Text(
                            "${historyList.size} connection${if (historyList.size != 1) "s" else ""}",
                            color = Color.Gray,
                            fontSize = MaterialTheme.typography.bodyMedium.fontSize
                        )
                    }
                },
                actions = {
                    if (historyList.isNotEmpty()) {
                        TextButton(onClick = {
                            viewModel.clearAll()
                            Toast.makeText(context, "History cleared", Toast.LENGTH_SHORT).show()
                        }) {
                            Icon(
                                imageVector = Icons.Outlined.Delete,
                                contentDescription = null,
                                tint = Color(0xFFFF5555)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text("Clear All", color = Color(0xFFFF5555))
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black)
            )
        }
    ) { padding ->
        if (historyList.isEmpty()) {
            EmptyHistoryState(modifier = Modifier.padding(padding))
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(Color.Black)
                    .padding(bottom = 100.dp)
            ) {
                sortedDates.forEach { dateKey ->
                    val dateHeader = viewModel.getReadableDate(dateKey)
                    item {
                        Text(
                            text = dateHeader,
                            color = Color.Gray,
                            modifier = Modifier
                                .padding(start = 20.dp, top = 20.dp, bottom = 10.dp),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    items(grouped[dateKey] ?: emptyList()) { connection ->
                        HistoryCard(
                            connection = connection,
                            onReconnect = {
                                Toast.makeText(context, "Reconnect ${it.username}@${it.host}", Toast.LENGTH_SHORT).show()
                            },
                            onDelete = {
                                viewModel.deleteConnection(it)
                            }
                        )
                    }
                }

                item {
                    ConnectionStatistics(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 24.dp),
                        history = historyList
                    )
                }
            }
        }
    }
}

@Composable
fun HistoryCard(
    connection: SSHConnectionHistory,
    onReconnect: (SSHConnectionHistory) -> Unit,
    onDelete: (SSHConnectionHistory) -> Unit
) {
    val statusColor = when (connection.status) {
        "success" -> Color(0xFF00FF88)
        "failed" -> Color(0xFFFF5555)
        else -> Color.Gray
    }
    val icon = when (connection.status) {
        "success" -> Icons.Outlined.CheckCircle
        "failed" -> Icons.Outlined.Cancel
        else -> Icons.Outlined.Help
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0x0DFFFFFF), // rgba(255,255,255,0.05)
        ),
        onClick = { onReconnect(connection) }
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(icon, contentDescription = null, tint = statusColor)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            "${connection.username}@${connection.host}",
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            connection.formattedTime(),
                            color = Color.Gray,
                            fontSize = MaterialTheme.typography.bodySmall.fontSize
                        )
                    }
                }
                IconButton(onClick = { onDelete(connection) }) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = "Delete",
                        tint = Color(0xFFFF5555)
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Badge("Port: ${connection.port}", Color(0x1AFFFFFF))
                Badge(connection.keyName, Color(0x1A00FF88))
            }

            if (connection.status == "failed" && connection.error.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0x1AFF5555), RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Text("Error: ${connection.error}", color = Color(0xFFFF5555))
                }
            }

            Spacer(Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Tap to reconnect", color = Color.Gray, fontSize = 12.sp)
                Spacer(Modifier.width(4.dp))
                Icon(Icons.Outlined.ChevronRight, null, tint = Color.Gray, modifier = Modifier.size(12.dp))
            }
        }
    }
}

@Composable
fun Badge(text: String, background: Color) {
    Box(
        modifier = Modifier
            .background(background, RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(text, color = Color.LightGray, fontSize = 12.sp)
    }
}

@Composable
fun EmptyHistoryState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(40.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .background(Color(0x1AFFFFFF), RoundedCornerShape(40.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Outlined.AccessTime, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(40.dp))
        }
        Spacer(Modifier.height(24.dp))
        Text("No Connection History", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 24.sp)
        Spacer(Modifier.height(12.dp))
        Text(
            "Your connection attempts will appear here for quick reconnection",
            color = Color.Gray,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
fun ConnectionStatistics(modifier: Modifier = Modifier, history: List<SSHConnectionHistory>) {
    val success = history.count { it.status == "success" }
    val failed = history.count { it.status == "failed" }
    val uniqueHosts = history.map { it.host }.toSet().size

    Column(
        modifier = modifier
            .background(Color(0x0DFFFFFF), RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 12.dp)) {
            Icon(Icons.Outlined.BarChart, null, tint = Color(0xFF00FF88))
            Spacer(Modifier.width(8.dp))
            Text("Connection Statistics", color = Color.White, fontWeight = FontWeight.SemiBold)
        }

        Row(horizontalArrangement = Arrangement.SpaceAround, modifier = Modifier.fillMaxWidth()) {
            StatBox(success.toString(), "Successful", Color(0xFF00FF88))
            StatBox(failed.toString(), "Failed", Color(0xFFFF5555))
            StatBox(uniqueHosts.toString(), "Unique Hosts", Color.LightGray)
        }
    }
}

@Composable
fun StatBox(value: String, label: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = color, fontWeight = FontWeight.Bold, fontSize = 24.sp)
        Text(label, color = Color.Gray, fontSize = 12.sp)
    }
}