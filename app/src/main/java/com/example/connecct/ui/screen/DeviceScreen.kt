package com.example.connecct.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.connecct.ui.components.DeviceCard
import com.example.connecct.ui.viewmodel.DeviceViewModel
import com.example.connecct.ui.viewmodel.Device
import android.util.Log
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import com.example.connecct.ui.state.ConnectionStatus
import com.example.connecct.ui.state.ConnectionUiEvent
import com.example.connecct.ui.viewmodel.ConnectionViewModel


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceScreen(
    viewModel: DeviceViewModel,
    connectionViewModel: ConnectionViewModel,
    navController: NavController
) {
    val devices = viewModel.devices
    val context = LocalContext.current

    var selectedDevice by remember { mutableStateOf<Device?>(null) }
    var showDialog by remember { mutableStateOf(false) }

    // 🔥 state lokal: apakah connect barusan dipicu dari screen ini?
    var shouldRedirectAfterConnect by remember { mutableStateOf(false) }

    // observe uiState dari ConnectionViewModel
    val uiState by connectionViewModel.uiState.collectAsState()

    // 🔁 efek: cuma navigate kalau:
    // - kita memang sedang "nunggu redirect" (shouldRedirectAfterConnect = true)
    // - status berubah jadi CONNECTED
    LaunchedEffect(uiState.connectionStatus, shouldRedirectAfterConnect) {
        if (shouldRedirectAfterConnect &&
            uiState.connectionStatus == ConnectionStatus.CONNECTED
        ) {
            shouldRedirectAfterConnect = false   // biar cuma sekali
            navController.navigate("connect_beta") {
                launchSingleTop = true
            }
        }

        // Kalau gagal / disconnect, kita batalin niat redirect
        if (uiState.connectionStatus == ConnectionStatus.FAILED ||
            uiState.connectionStatus == ConnectionStatus.DISCONNECTED
        ) {
            shouldRedirectAfterConnect = false
        }
    }

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
                            onConnect = { dev ->
                                // 🚫 JANGAN navigate di sini lagi
                                // cukup set "niat redirect" + mulai koneksi

                                shouldRedirectAfterConnect = true

                                connectionViewModel.onEvent(
                                    ConnectionUiEvent.OnHostChanged(dev.host)
                                )
                                connectionViewModel.onEvent(
                                    ConnectionUiEvent.OnUsernameChanged(dev.user)
                                )

                                connectionViewModel.connectFromDevice(
                                    context = context,
                                    host = dev.host,
                                    username = dev.user,
                                    onSuccess = {
                                        // cukup update status device,
                                        // redirect di-handle oleh LaunchedEffect di atas
                                        viewModel.setConnected(dev.id, true)
                                    }
                                )
                            },
                            onDisconnect = { dev ->
                                Log.d("DEVICE_SCREEN", "Disconnecting device: ${dev.id}")
                                shouldRedirectAfterConnect = false  // kalau sempat connect → batalin niat redirect
                                viewModel.setConnected(dev.id, false)
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