package com.example.connecct.ui.screen

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.documentfile.provider.DocumentFile
import androidx.navigation.NavController
import com.example.connecct.ui.state.ConnectionStatus
import com.example.connecct.ui.viewmodel.ConnectionViewModel
import com.example.connecct.ui.state.ConnectionUiEvent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectBetaScreen(
    viewModel: ConnectionViewModel,
    navController: NavController
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollState = rememberScrollState()

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri ->
            if (uri != null) {
                val (name, size) = getFileMetadata(context, uri)
                viewModel.onEvent(ConnectionUiEvent.OnPrivateKeySelected(uri.toString(), name))
            }
        }
    )

    // Snackbar otomatis saat error
    LaunchedEffect(uiState.errorMessage) {
        if (uiState.errorMessage.isNotEmpty()) {
            snackbarHostState.showSnackbar(
                message = "Error: ${uiState.errorMessage}",
                withDismissAction = true
            )
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // 🔽 Gunakan verticalScroll agar bisa di-scroll
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState) // ← Ini penting!
                    .padding(horizontal = 24.dp)
                    .padding(top = 32.dp),
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                // Judul
                Text(
                    "SSH Connect",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Start)
                )
                Text(
                    "Secure shell connection manager",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .align(Alignment.Start)
                        .padding(bottom = 24.dp)
                )

                // Host
                Text("Host", fontWeight = FontWeight.Medium, modifier = Modifier.align(Alignment.Start))
                OutlinedTextField(
                    value = uiState.host,
                    onValueChange = { viewModel.onEvent(ConnectionUiEvent.OnHostChanged(it)) },
                    placeholder = { Text("192.168.0.1") },
                    singleLine = true,
                    enabled = !uiState.isConnecting,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(16.dp))

                // Username
                Text("Username", fontWeight = FontWeight.Medium, modifier = Modifier.align(Alignment.Start))
                OutlinedTextField(
                    value = uiState.username,
                    onValueChange = { viewModel.onEvent(ConnectionUiEvent.OnUsernameChanged(it)) },
                    placeholder = { Text("root") },
                    singleLine = true,
                    enabled = !uiState.isConnecting,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(16.dp))

                // Passphrase
                Text("Passphrase", fontWeight = FontWeight.Medium, modifier = Modifier.align(Alignment.Start))
                OutlinedTextField(
                    value = uiState.passphrase,
                    onValueChange = { viewModel.onEvent(ConnectionUiEvent.OnPassphraseChanged(it)) },
                    placeholder = { Text("********") },
                    singleLine = true,
                    enabled = !uiState.isConnecting,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(20.dp))

                // Private Key Button
                Button(
                    onClick = { navController.navigate("settings") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isConnecting,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Pilih Private Key")
                }

                if (uiState.isKeySelected) {
                    Spacer(Modifier.height(8.dp))
                    Column(horizontalAlignment = Alignment.Start) {
                        Text("Filename: ${uiState.filename}", fontSize = 13.sp)
                        Text("Filesize: ${uiState.filesize}", fontSize = 13.sp)
                    }
                }

                Spacer(Modifier.height(24.dp))

                // Connect Button
                Button(
                    onClick = { viewModel.connectToServer(context) },
                    enabled = !uiState.isConnecting,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Text(
                        if (uiState.isConnecting) "Connecting..." else "Connect",
                        fontSize = 16.sp
                    )
                }

                Spacer(Modifier.height(28.dp))

                // Quick Tips
                Text(
                    "💡 Quick Tips",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.align(Alignment.Start)
                )

                Spacer(Modifier.height(8.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(16.dp)
                ) {
                    Text("• Pastikan SSH Server aktif dan dapat diakses melalui jaringan yang sama.", fontSize = 13.sp)
                    Spacer(Modifier.height(4.dp))
                    Text("• Gunakan private key yang cocok dengan server tujuan.", fontSize = 13.sp)
                    Spacer(Modifier.height(4.dp))
                    Text("• Jika koneksi gagal, periksa kembali Host, Username, dan izin file kunci Anda.", fontSize = 13.sp)
                }

                Spacer(Modifier.height(24.dp))

                // Status
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val isConnected = uiState.connectionStatus == ConnectionStatus.CONNECTED

                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .background(
                                    color = if (isConnected)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.error,
                                    shape = RoundedCornerShape(50)
                                )
                        )

                        Text(
                            text = if (isConnected) "Connected" else "Disconnected",
                            color = if (isConnected)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                if (uiState.connectionStatus == ConnectionStatus.CONNECTED) {
                    Spacer(Modifier.height(24.dp))
                    OutlinedButton(
                        onClick = { viewModel.onEvent(ConnectionUiEvent.OnResetClicked) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Reset", fontWeight = FontWeight.Medium)
                    }
                }

                Spacer(Modifier.height(100.dp)) // memberi ruang bawah agar scroll tidak mentok
            }
        }
    }
}

fun getFileMetadata(context: Context, uri: Uri): Pair<String, String> {
    val docfile = DocumentFile.fromSingleUri(context, uri)
    val name = docfile?.name ?: "Unknown"
    val sizeByBytes = docfile?.length() ?: 0L
    val sizeInKB = String.format("%.2f KB", sizeByBytes / 1024.0)
    return name to sizeInKB
}