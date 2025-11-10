package com.example.connecct.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.text.AnnotatedString
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import android.content.ClipData
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.connecct.ui.components.KeyCard
import com.example.connecct.ui.viewmodel.KeysViewModel
import com.example.connecct.Conn.generateKey
import com.example.connecct.storage.loadStorageKey
import com.example.connecct.ui.viewmodel.SSHKeys
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KeysScreen(viewModel: KeysViewModel = viewModel()) {
    val keys by remember { derivedStateOf { viewModel.keys } }

    var selectedKey by remember { mutableStateOf<SSHKeys?>(null) }
    var showDialog by remember {mutableStateOf(false)}

    val context = LocalContext.current

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    val keyName = "id_rsa_${keys.size + 1}"
                    val outputDir = File("/data/data/com.example.connecct/files") // lokasi internal app
                    val (privateFile, publicFile) = generateKey.generateKeyPair(
                        keyname = keyName,
                        outputDir = outputDir
                    )

                    // tambahkan ke ViewModel (menyimpan info key)
                    viewModel.addKey(
                        keyName,
                        "RSA",
                        privateFile.absolutePath,
                        publicFile.absolutePath
                    )
                },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Outlined.Add, contentDescription = "Add Key")
            }
        },
        topBar = {
            TopAppBar(
                title = { Text("SSH Keys", fontWeight = FontWeight.Bold) }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            if (keys.isEmpty()) {
                EmptyState()
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    items(keys) { key ->
                        KeyCard(
                            key = key,
                            onDelete = { viewModel.deleteKey(it) },
                            onClick = {
                                selectedKey = key
                                showDialog = true
                            }
                        )
                    }
                }
            }

            if (showDialog && selectedKey != null) {
                selectedKey?.let { key ->
                    val keyContent = remember(key) {
                        loadStorageKey(context).readPublicKeyContent(key.publicFile)
                    }

                    val clipboard = LocalClipboard.current
                    val scope = rememberCoroutineScope()
                    var copied by remember{mutableStateOf(false)}

                    AlertDialog(
                        onDismissRequest = { showDialog = false },
                        confirmButton = {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ){
                                TextButton(onClick = {
                                    scope.launch {
                                        val clipData = ClipData.newPlainText("SSH PUBLIC KEY", keyContent)
                                        clipboard.setClipEntry(ClipEntry(clipData))
                                        copied = true
                                    }
                                }) {
                                    Text(if (copied) "Copied" else "Copy")
                                }
                                TextButton(onClick = {showDialog = false}) { Text("Close") }
                            }
                        },
                        title = { Text("Key Details", fontWeight = FontWeight.Bold) },
                        text = {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text("Name: ${key.name}")
                                Text("Type: ${key.type}")
                                Text("Added At: ${key.addedAt}")
                                Spacer(modifier = Modifier.height(12.dp))
                                Divider()
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("🔑 Public Key:")
                                Text(
                                    keyContent,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    )
                }
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