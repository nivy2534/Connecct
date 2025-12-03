package com.example.connecct.ui.screen

import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import java.io.File
import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.ui.Alignment
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.FileProvider
import com.example.connecct.ui.state.OpenedFile
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.common.MediaItem
import androidx.media3.ui.PlayerView

// GENERIC EXPORT FUNCTION (dipakai semua tipe)
fun exportBytes(context: android.content.Context, uri: Uri, data: ByteArray) {
    try {
        context.contentResolver.openOutputStream(uri)?.use { out ->
            out.write(data)
        }
        Toast.makeText(context, "File saved!", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        Toast.makeText(context, "Failed: ${e.message}", Toast.LENGTH_LONG).show()
    }
}

// ROOT VIEWER
@Composable
fun FileViewer(openedFile: OpenedFile, onClose: () -> Unit) {
    when (openedFile) {
        is OpenedFile.Text -> FileViewerTextScreen(openedFile.name, openedFile.content, onClose)
        is OpenedFile.Image -> FileViewerImageScreen(openedFile.name, openedFile.bytes, onClose)
        is OpenedFile.Pdf -> FileViewerPdfScreen(openedFile.name, openedFile.bytes, onClose)
        is OpenedFile.Video -> FileViewerVideoScreen(openedFile.name, openedFile.bytes, onClose)
        is OpenedFile.External -> OpenExternalFileScreen(openedFile, onClose)
        else -> Text("Unsupported file type")
    }
}

// TEXT FILE VIEWER + EXPORT
@Composable
fun FileViewerTextScreen(fileName: String, content: String, onClose: () -> Unit) {

    val context = LocalContext.current

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/plain")
    ) { uri ->
        uri?.let {
            exportBytes(context, it, content.toByteArray())
        }
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {

        Text(fileName, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(12.dp))

        Text(content, modifier = Modifier.weight(1f))
        Spacer(Modifier.height(12.dp))

        Button(onClick = { launcher.launch(fileName) }, modifier = Modifier.fillMaxWidth()) {
            Text("Export")
        }

        Spacer(Modifier.height(8.dp))

        Button(onClick = onClose, modifier = Modifier.fillMaxWidth()) {
            Text("Close")
        }
    }
}

// IMAGE VIEWER + EXPORT
@Composable
fun FileViewerImageScreen(fileName: String, bytes: ByteArray, onClose: () -> Unit) {

    val context = LocalContext.current

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("image/*")
    ) { uri ->
        uri?.let { exportBytes(context, it, bytes) }
    }

    val bitmap = remember(bytes) {
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {

        Text(fileName, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(12.dp))

        bitmap?.let {
            Image(
                bitmap = it.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxWidth().weight(1f)
            )
        }

        Spacer(Modifier.height(12.dp))

        Button(onClick = { launcher.launch(fileName) }, modifier = Modifier.fillMaxWidth()) {
            Text("Export")
        }

        Spacer(Modifier.height(8.dp))

        Button(onClick = onClose, modifier = Modifier.fillMaxWidth()) {
            Text("Close")
        }
    }
}

// VIDEO VIEWER + EXPORT
@Composable
fun FileViewerVideoScreen(fileName: String, bytes: ByteArray, onClose: () -> Unit) {

    val context = LocalContext.current

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("video/mp4")
    ) { uri ->
        uri?.let { exportBytes(context, it, bytes) }
    }

    val videoFile = remember(bytes) {
        val f = File(context.cacheDir, "temp_${System.currentTimeMillis()}.mp4")
        f.writeBytes(bytes)
        f
    }

    val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", videoFile)
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(scrollState).padding(16.dp)
    ) {
        Text(fileName, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(16.dp))

        Box(modifier = Modifier.height(250.dp).fillMaxWidth()) {

            AndroidView(
                modifier = Modifier.matchParentSize(),
                factory = { ctx ->
                    val exo = ExoPlayer.Builder(ctx).build().apply {
                        setMediaItem(MediaItem.fromUri(uri))
                        prepare()
                        playWhenReady = true
                    }

                    PlayerView(ctx).apply {
                        player = exo
                        useController = true
                    }
                }
            )

            IconButton(
                onClick = {
                    val intent = Intent(context, FullscreenVideoActivity::class.java).apply {
                        putExtra("VIDEO_URI", uri.toString())
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(intent)
                },
                modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)
            ) {
                Icon(Icons.Default.Fullscreen, contentDescription = "Fullscreen")
            }
        }

        Spacer(Modifier.height(24.dp))

        Button(onClick = { exportLauncher.launch(fileName) }, modifier = Modifier.fillMaxWidth()) {
            Text("Export")
        }

        Spacer(Modifier.height(12.dp))

        Button(onClick = onClose, modifier = Modifier.fillMaxWidth()) {
            Text("Close")
        }
    }
}

// PDF VIEWER + EXPORT
@Composable
fun FileViewerPdfScreen(fileName: String, bytes: ByteArray, onClose: () -> Unit) {

    val context = LocalContext.current

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        uri?.let { exportBytes(context, it, bytes) }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp)
    ) {

        Text(fileName, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(12.dp))

        Box(
            modifier = Modifier.fillMaxWidth().weight(1f),
            contentAlignment = Alignment.Center
        ) {
            Text("PDF Viewer coming soon...")
        }

        Spacer(Modifier.height(12.dp))

        Button(onClick = { launcher.launch(fileName) }, modifier = Modifier.fillMaxWidth()) {
            Text("Export PDF")
        }

        Spacer(Modifier.height(8.dp))

        Button(onClick = onClose, modifier = Modifier.fillMaxWidth()) {
            Text("Close")
        }
    }
}

//Open File Pakai External App
@Composable
fun OpenExternalFileScreen(file: OpenedFile.External, onClose: () -> Unit) {
    val context = LocalContext.current

    LaunchedEffect(Unit) {

        val downloads = File(context.getExternalFilesDir(android.os.Environment.DIRECTORY_DOWNLOADS), "opened_videos")
        if (!downloads.exists()) downloads.mkdirs()

        val finalName = if (file.name.endsWith(".mp4")) file.name else file.name + ".mp4"
        val outputFile = File(downloads, finalName)
        outputFile.writeBytes(file.bytes)

        val uri = FileProvider.getUriForFile(context, "com.example.connecct.provider", outputFile)

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, file.mime)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        try {
            context.startActivity(intent)
        } catch (_: Exception) {
            Toast.makeText(context, "No app found to open this file", Toast.LENGTH_LONG).show()
        }

        onClose()
    }
}
