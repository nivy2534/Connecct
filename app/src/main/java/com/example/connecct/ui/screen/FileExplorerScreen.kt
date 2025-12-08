package com.example.connecct.ui.screen

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.connecct.ui.state.ConnectionStatus
import com.example.connecct.ui.state.ConnectionUiEvent
import com.example.connecct.ui.state.OpenedFile
import com.example.connecct.ui.state.RemoteFile
import com.example.connecct.ui.viewmodel.ConnectionViewModel

@Composable
fun FileExplorerScreen(viewModel: ConnectionViewModel) {

    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // Load directory when connected
    LaunchedEffect(state.connectionStatus) {
        if (state.connectionStatus == ConnectionStatus.CONNECTED) {
            viewModel.onEvent(ConnectionUiEvent.LoadDirectory)
        }
    }

    // =============== FILE VIEWER ===============
    state.openedFile?.let { opened ->
        when (opened) {
            is OpenedFile.Text -> FileViewerTextScreen(
                fileName = opened.name,
                content = opened.content,
                onClose = { viewModel.onEvent(ConnectionUiEvent.CloseFile) }
            )

            is OpenedFile.Image -> FileViewerImageScreen(
                fileName = opened.name,
                bytes = opened.bytes,
                onClose = { viewModel.onEvent(ConnectionUiEvent.CloseFile) }
            )

            is OpenedFile.Pdf -> FileViewerPdfScreen(
                fileName = opened.name,
                bytes = opened.bytes,
                onClose = { viewModel.onEvent(ConnectionUiEvent.CloseFile) }
            )

            is OpenedFile.Video -> FileViewerVideoScreen(
                fileName = opened.name,
                bytes = opened.bytes,
                onClose = { viewModel.onEvent(ConnectionUiEvent.CloseFile) }
            )

            is OpenedFile.External -> OpenExternalFileScreen(
                file = opened,
                onClose = { viewModel.onEvent(ConnectionUiEvent.CloseFile) }
            )

            else -> return
        }
        return
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            viewModel.uploadFileFromAndroid(uri, context)
        }
    }

    // =============== MAIN FILE EXPLORER UI ===============
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            // UP BUTTON
            Button(
                onClick = { viewModel.onEvent(ConnectionUiEvent.NavigateUp) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
            ) {
                Text("Up")
            }

            Spacer(modifier = Modifier.width(12.dp))

            // PATH
            Text(
                text = "Path: ${state.currentPath}",
                color = Color.White,
                modifier = Modifier.weight(1f)
            )

            // REFRESH BUTTON
            IconButton(onClick = {
                viewModel.onEvent(ConnectionUiEvent.LoadDirectory)
            }) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh",
                    tint = Color.White
                )
            }

            // UPLOAD BUTTON
            IconButton(onClick = {
                launcher.launch("*/*")
            }) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Upload file",
                    tint = Color.White
                )
            }
        }


        Spacer(Modifier.height(16.dp))

        // ---- Loading State ----
        if (state.isLoadingDirectory) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return
        }

        // ---- File List ----
        LazyColumn(
            modifier = Modifier.fillMaxSize()
        ) {
            items(
                items = state.remoteFiles,
                key = { it.name } // better list stability
            ) { file ->
                RemoteFileItem(file = file) {
                    if (file.isDirectory) {
                        viewModel.onEvent(ConnectionUiEvent.NavigateTo(file.name))
                    } else {
                        viewModel.onEvent(ConnectionUiEvent.OpenFile(file.name, context))
                    }
                }
            }
        }
    }
}

@Composable
fun RemoteFileItem(file: RemoteFile, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable { onClick() },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = if (file.isDirectory) "📁 ${file.name}" else "📄 ${file.name}")
        if (!file.isDirectory) {
            Text("${file.size} bytes")
        }
    }
}

@Composable
fun FileViewerScreen(fileName: String, content: String, onClose: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = fileName,
            style = MaterialTheme.typography.titleLarge
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Konten file
        Text(
            text = content,
            modifier = Modifier.weight(1f)
        )

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = onClose,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Close")
        }
    }
}