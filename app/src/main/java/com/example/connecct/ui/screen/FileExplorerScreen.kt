package com.example.connecct.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

    // 🔥 Auto load directory setelah CONNECTED
    LaunchedEffect(state.connectionStatus) {
        if (state.connectionStatus == ConnectionStatus.CONNECTED) {
            viewModel.onEvent(ConnectionUiEvent.LoadDirectory)
        }
    }

    val openedFile = state.openedFile

    if (openedFile != null) {
        when (openedFile) {

            is OpenedFile.Text -> FileViewerTextScreen(
                fileName = openedFile.name,
                content = openedFile.content,
                onClose = { viewModel.onEvent(ConnectionUiEvent.CloseFile) }
            )

            is OpenedFile.Image -> FileViewerImageScreen(
                fileName = openedFile.name,
                bytes = openedFile.bytes,
                onClose = { viewModel.onEvent(ConnectionUiEvent.CloseFile) }
            )

            is OpenedFile.Pdf -> FileViewerPdfScreen(
                fileName = openedFile.name,
                bytes = openedFile.bytes,
                onClose = { viewModel.onEvent(ConnectionUiEvent.CloseFile) }
            )

            // 🔥 Tambahkan ini
            is OpenedFile.Video -> FileViewerVideoScreen(
                fileName = openedFile.name,
                bytes = openedFile.bytes,
                onClose = { viewModel.onEvent(ConnectionUiEvent.CloseFile) }
            )

            // 🔥 Tambahkan ini kalau ingin buka via aplikasi eksternal
            is OpenedFile.External -> OpenExternalFileScreen(
                file = openedFile,
                onClose = { viewModel.onEvent(ConnectionUiEvent.CloseFile) }
            )

            else -> {
                Text("Unsupported file type")
            }
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        // ==== PATH SEKARANG ====
        Text(
            text = "Path: ${state.currentPath}",
            style = MaterialTheme.typography.bodyLarge
        )

        Spacer(modifier = Modifier.height(10.dp))

        // ==== TOMBOL NAVIGASI ====
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Button(onClick = { viewModel.onEvent(ConnectionUiEvent.NavigateUp) }) {
                Text("Up")
            }

            Text(
                text = "Refresh",
                modifier = Modifier
                    .padding(8.dp)
                    .clickable { viewModel.onEvent(ConnectionUiEvent.LoadDirectory) },
                color = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ==== LOADING ====
        if (state.isLoadingDirectory) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return
        }

        // ==== LIST FILE ====
        LazyColumn {
            items(state.remoteFiles) { file ->
                val context = LocalContext.current

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
        horizontalArrangement = Arrangement.SpaceBetween
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