package com.example.connecct.ui.screen

import androidx.activity.compose.rememberLauncherForActivityResult
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cable
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.connecct.ui.state.ConnectionStatus
import com.example.connecct.ui.viewmodel.ConnectionViewModel

@Composable
fun ConnectScreen(
    viewModel: ConnectionViewModel,
    navController: NavController
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    // 📌 ZXing QR Scanner Launcher
    val qrLauncher = rememberLauncherForActivityResult(
        contract = ScanContract()
    ) { result ->
        if (result.contents != null) {
            viewModel.handleQrConnection(context, result.contents)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            // 🔌 Icon Connection
            Icon(
                imageVector = Icons.Default.Cable,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(100.dp)
            )

            Text(
                text = "Scan QR untuk memulai koneksi SSH",
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                fontSize = 13.sp
            )

            Spacer(Modifier.height(20.dp))
            val isConnected = uiState.connectionStatus == ConnectionStatus.CONNECTED
            val isConnecting = uiState.connectionStatus == ConnectionStatus.CONNECTING
            // 📷 Tombol Scan QR (tanpa animasi)
            Button(
                onClick = {
                    if (isConnected) {
                        viewModel.disconnect()
                    } else {
                        if (!isConnecting) {
                            val options = ScanOptions().apply {
                                setPrompt("Scan QR Code SSH")
                                setBeepEnabled(true)
                                setOrientationLocked(true)
                            }
                            qrLauncher.launch(options)
                        }
                    }
                },
                enabled = !isConnecting,
                modifier = Modifier
                    .width(220.dp)
                    .height(60.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor =
                        when {
                            isConnected -> Color(0xFFF44336)  // 🔴 merah saat connected
                            isConnecting -> Color(0xFFFFC107) // 🟡 kuning saat connecting
                            else -> MaterialTheme.colorScheme.primary // 🟢 default saat connect
                        }
                )
            ) {
                Text(
                    text = when {
                        isConnected -> "Disconnect"
                        isConnecting -> "Connecting..."
                        else -> "Connect"
                    },
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(30.dp))

            // 📝 Status Text saja (tanpa blok card)
            ConnectionStatusText(uiState.connectionStatus)

            Spacer(Modifier.height(60.dp))
        }
    }
}

@Composable
fun ConnectionStatusText(status: ConnectionStatus) {

    val text = when (status) {
        ConnectionStatus.CONNECTED -> "Connected"
        ConnectionStatus.CONNECTING -> "Connecting..."
        else -> "Not Connected"
    }

    val color = when (status) {
        ConnectionStatus.CONNECTED -> Color(0xFF4CAF50)
        ConnectionStatus.CONNECTING -> Color(0xFFFFC107)
        else -> Color(0xFFF44336)
    }

    Text(
        text = text,
        color = color,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp
    )
}
