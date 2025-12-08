package com.example.connecct.ui.screen

import android.content.Context
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.documentfile.provider.DocumentFile
import androidx.navigation.NavController
import com.example.connecct.ui.state.ConnectionStatus
import com.example.connecct.ui.state.ConnectionUiEvent
import com.example.connecct.ui.state.RemoteFile
import com.example.connecct.ui.viewmodel.ConnectionViewModel
import com.example.connecct.ui.viewmodel.DeviceViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectBetaScreen(
    viewModel: ConnectionViewModel,
    deviceViewModel: DeviceViewModel,
    navController: NavController
) {
    val context = LocalContext.current
    val ui by viewModel.uiState.collectAsState()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(ui.errorMessage) {
        if (ui.errorMessage.isNotEmpty()) {
            snackbar.showSnackbar(ui.errorMessage)
        }
    }

    LaunchedEffect(ui.connectionStatus, ui.host, ui.username, ui.username) {
        if(ui.connectionStatus == ConnectionStatus.CONNECTED &&
            ui.host.isNotBlank() &&
            ui.username.isNotBlank()
            ){
            deviceViewModel.addOrUpdateManual(
                host = ui.host,
                username = ui.username,
                connected = true
            )
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        if (ui.connectionStatus == ConnectionStatus.CONNECTED) {
            FileExplorerScreen(viewModel)
        } else {
            ConnectionScreen(viewModel, context, padding, navController)
        }
    }
}

@Composable
fun ConnectionScreen(
    viewModel: ConnectionViewModel,
    context: Context,
    padding: PaddingValues,
    navController: NavController
) {
    val ui = viewModel.uiState.collectAsState().value
    val scroll = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .verticalScroll(scroll)
            .padding(24.dp)
    ) {

        Text("SSH Connect", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text("Secure shell connection manager", color = Color.Gray)

        Spacer(Modifier.height(24.dp))

        // HOST
        Text("Host")
        OutlinedTextField(
            value = ui.host,
            onValueChange = { viewModel.onEvent(ConnectionUiEvent.OnHostChanged(it)) },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(16.dp))

        // USERNAME
        Text("Username")
        OutlinedTextField(
            value = ui.username,
            onValueChange = { viewModel.onEvent(ConnectionUiEvent.OnUsernameChanged(it)) },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(16.dp))

        // PASSPHRASE
        Text("Passphrase")
        OutlinedTextField(
            value = ui.passphrase,
            onValueChange = { viewModel.onEvent(ConnectionUiEvent.OnPassphraseChanged(it)) },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(20.dp))

        Button(
            onClick = { navController.navigate("settings") },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Pilih Private Key")
        }

        if (ui.isKeySelected) {
            Spacer(Modifier.height(6.dp))
            Text("Filename: ${ui.filename}", fontSize = 14.sp)
        }

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = { viewModel.connectToServer(context) },
            enabled = !ui.isConnecting,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (ui.isConnecting) "Connecting..." else "Connect")
        }

        Spacer(Modifier.height(100.dp))
    }
}

@Composable
fun FileItem(
    file: RemoteFile,
    onFolderClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable {
                if (file.isDirectory) onFolderClick()
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(
                    if (file.isDirectory) Color(0xFF90CAF9) else Color(0xFFF48FB1),
                    shape = CircleShape
                )
        )

        Spacer(Modifier.width(12.dp))

        Column {
            Text(file.name, fontWeight = FontWeight.Medium)
            if (!file.isDirectory) {
                Text("${file.size} bytes", fontSize = 12.sp, color = Color.Gray)
            }
        }
    }
}

fun getFileMetadata(context: Context, uri: Uri): Pair<String, String> {
    val doc = DocumentFile.fromSingleUri(context, uri)
    val name = doc?.name ?: "Unknown"
    val size = doc?.length() ?: 0L
    return name to "${size / 1024} KB"
}
