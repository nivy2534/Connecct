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

enum class AddKeyMode { GENERATE, IMPORT}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KeysScreen(viewModel: KeysViewModel = viewModel()) {
    val keys by remember { derivedStateOf { viewModel.keys } }

    var selectedKey by remember { mutableStateOf<SSHKeys?>(null) }
    var showDialog by remember { mutableStateOf(false) }

    var showAddKeyDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.loadStoredKeys(context)
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    showAddKeyDialog = true // buka dialog, bukan langsung generate
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
            // ======== LIST KEYS / EMPTY STATE ========
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
                            onDelete = { keyToDelete ->
                                viewModel.deleteKey(context, keyToDelete)
                            },
                            onClick = {
                                selectedKey = key
                                showDialog = true
                            }
                        )
                    }
                }
            }

            // ======== DIALOG DETAIL KEY (PUBLIC KEY) ========
            if (showDialog && selectedKey != null) {
                selectedKey?.let { key ->
                    val keyContent = remember(key) {
                        loadStorageKey(context).readPublicKeyContent(key.publicFile)
                    }

                    val clipboard = LocalClipboard.current
                    val scope = rememberCoroutineScope()
                    var copied by remember { mutableStateOf(false) }

                    AlertDialog(
                        onDismissRequest = { showDialog = false },
                        confirmButton = {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                TextButton(
                                    onClick = {
                                        scope.launch {
                                            val clipData = ClipData.newPlainText(
                                                "SSH PUBLIC KEY",
                                                keyContent
                                            )
                                            clipboard.setClipEntry(ClipEntry(clipData))
                                            copied = true
                                        }
                                    }
                                ) {
                                    Text(if (copied) "Copied" else "Copy")
                                }
                                TextButton(onClick = { showDialog = false }) {
                                    Text("Close")
                                }
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

            // ======== DIALOG ADD / IMPORT KEY ========
            if (showAddKeyDialog) {
                AddKeyDialog(
                    onDismiss = { showAddKeyDialog = false },
                    onGenerate = { keyName, passphrase ->
                        val sshDir = File(context.filesDir, "ssh_keys").apply {
                            if (!exists()) mkdirs()
                        }

                        val (privateFile, publicFile) = generateKey.generateKeyPair(
                            keyname = keyName,
                            outputDir = sshDir,
                            passphrase = passphrase.ifBlank { null }
                        )

                        viewModel.addKey(
                            keyName,
                            "RSA",
                            privateFile.absolutePath,
                            publicFile.absolutePath
                        )
                        showAddKeyDialog = false
                    },
                    onImport = { keyName, privPath, pubPath ->
                        val sshDir = File(context.filesDir, "ssh_keys").apply {
                            if (!exists()) mkdirs()
                        }

                        val srcPriv = File(privPath)
                        val destPriv = File(sshDir, keyName)
                        srcPriv.copyTo(destPriv, overwrite = true)

                        val destPub = if (pubPath.isNotBlank()) {
                            val srcPub = File(pubPath)
                            val file = File(sshDir, "$keyName.pub")
                            srcPub.copyTo(file, overwrite = true)
                            file
                        } else {
                            null
                        }

                        viewModel.addKey(
                            keyName,
                            "RSA",
                            destPriv.absolutePath,
                            destPub?.absolutePath ?: ""
                        )

                        showAddKeyDialog = false
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

@Composable
fun AddKeyDialog(
    onDismiss: () -> Unit,
    onGenerate: (keyName: String, passphrase: String) -> Unit,
    onImport: (keyName: String, privPath: String, pubPath: String) -> Unit
){
    var mode by remember { mutableStateOf(AddKeyMode.GENERATE) }
    var keyName by remember { mutableStateOf("") }
    var passphrase by remember { mutableStateOf("") }

    var importPrivatePath by remember { mutableStateOf("") }
    var importPublicPath by remember { mutableStateOf("") }

    val isGenerateValid = keyName.isNotBlank()
    val isImportValid = keyName.isNotBlank() && importPrivatePath.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add SSH Key", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Mode selector
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = mode == AddKeyMode.GENERATE,
                        onClick = { mode = AddKeyMode.GENERATE },
                        label = { Text("Generate new") }
                    )
                    FilterChip(
                        selected = mode == AddKeyMode.IMPORT,
                        onClick = { mode = AddKeyMode.IMPORT },
                        label = { Text("Import existing") }
                    )
                }

                OutlinedTextField(
                    value = keyName,
                    onValueChange = { keyName = it },
                    label = { Text("Key name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                when (mode) {
                    AddKeyMode.GENERATE -> {
                        OutlinedTextField(
                            value = passphrase,
                            onValueChange = { passphrase = it },
                            label = { Text("Passphrase (optional)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    AddKeyMode.IMPORT -> {
                        OutlinedTextField(
                            value = importPrivatePath,
                            onValueChange = { importPrivatePath = it },
                            label = { Text("Private key path") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = importPublicPath,
                            onValueChange = { importPublicPath = it },
                            label = { Text("Public key path (optional)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Text(
                            "Nanti bisa kamu ganti jadi file picker (SAF) biar user pilih file, " +
                                    "sekarang manual path dulu.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    when (mode) {
                        AddKeyMode.GENERATE -> onGenerate(keyName, passphrase)
                        AddKeyMode.IMPORT -> onImport(
                            keyName,
                            importPrivatePath,
                            importPublicPath
                        )
                    }
                },
                enabled = when (mode) {
                    AddKeyMode.GENERATE -> isGenerateValid
                    AddKeyMode.IMPORT -> isImportValid
                }
            ) {
                Text(
                    when (mode) {
                        AddKeyMode.GENERATE -> "Generate"
                        AddKeyMode.IMPORT -> "Import"
                    }
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}