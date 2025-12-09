package com.example.connecct.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.connecct.ui.components.DeviceCard
import com.example.connecct.ui.viewmodel.DeviceViewModel
import com.example.connecct.ui.viewmodel.Device
import android.util.Log
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import com.example.connecct.ui.state.ConnectionStatus
import com.example.connecct.ui.state.ConnectionUiEvent
import com.example.connecct.ui.viewmodel.ConnectionViewModel
import com.example.connecct.ui.viewmodel.DeviceViewModelFactory
import com.example.connecct.ui.viewmodel.KeysViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceScreen(
    viewModel: DeviceViewModel,
    connectionViewModel: ConnectionViewModel,
    keysViewModel: KeysViewModel,
    navController: NavController
) {
    val devices = viewModel.devices
    val context = LocalContext.current

    var selectedDevice by remember { mutableStateOf<Device?>(null) }
    var showDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Connected Devices", fontWeight = FontWeight.Bold) }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {

            if (devices.isEmpty()) {
                EmptyState()
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    items(devices) { device ->
                        DeviceCard(
                            device = device,
                            onClick = {
                                Log.d("DEVICE_SCREEN", "Clicked device: ${device.id}")
                                selectedDevice = device
                                showDialog = true
                            },
                            onDelete = {
                                Log.d("DEVICE_SCREEN", "Removing device: ${device.id}")
                                viewModel.removeDevice(device)
                            },
                            onConnect = {
                                val ui = connectionViewModel.uiState.value

                                if (ui.privateKeyPath.isBlank()) {
                                    Log.e("DEVICE_SCREEN", "Private key belum dipilih. Silakan pilih di Settings dulu.")

                                    return@DeviceCard
                                }

                                connectionViewModel.onEvent(
                                    ConnectionUiEvent.OnHostChanged(device.host)
                                )
                                connectionViewModel.onEvent(
                                    ConnectionUiEvent.OnUsernameChanged(device.user)
                                )

                                connectionViewModel.connectToServer(
                                    context = context,
                                    onSuccess = {
                                        navController.navigate("connect_beta") {
                                            launchSingleTop = true
                                        }
                                    }
                                )
                            },
                            onDisconnect = {
                                Log.d("DEVICE_SCREEN", "Disconnecting device: ${device.id}")
                                viewModel.setConnected(device.id, false)
                                connectionViewModel.disconnect()
                            }
                        )
                    }
                }
            }

            if (showDialog && selectedDevice != null) {

                val device = selectedDevice!!
                val displayName = device.deviceName.ifBlank { "${device.user}@${device.host}" }
                val ip = device.host
                val statusText = if (device.isOnline) "Online" else "Offline"

                AlertDialog(
                    onDismissRequest = { showDialog = false },
                    confirmButton = {
                        TextButton(onClick = { showDialog = false }) {
                            Text("Close")
                        }
                    },
                    title = { Text("Device Details", fontWeight = FontWeight.Bold) },
                    text = {
                        Column {
                            Text("Name: $displayName")
                            Text("User: ${device.user}")
                            Text("OS: ${device.os ?: "-"}")
                            Text("IP: $ip")
                            Text("Status: $statusText")
                            Text("Last seen (raw): ${device.lastSeen}")
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun EmptyState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("No SSH keys added yet.")
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Tap the + button to add a key.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}