package com.example.connecct.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.SettingsBrightness
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.connecct.ui.state.ConnectionUiEvent
import com.example.connecct.ui.viewmodel.ConnectionViewModel
import com.example.connecct.viewmodel.ThemeViewModel
import com.example.connecct.ui.viewmodel.KeysViewModel
import com.example.connecct.ui.viewmodel.SSHKeys

@Composable
fun SettingsScreen(
    themeViewModel: ThemeViewModel,
    keyViewModel: KeysViewModel,
    connectionViewModel: ConnectionViewModel,
    navController: NavHostController
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current

    val key = keyViewModel.key        // ← key aktif
    var showGenerateDialog by remember { mutableStateOf(false) }
    var showManualDialog by remember { mutableStateOf(false) }
    var passphrase by remember { mutableStateOf("") }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var keyToDelete by remember { mutableStateOf<SSHKeys?>(null) }

    // Load key dari storage
    LaunchedEffect(Unit) {
        keyViewModel.loadStoredKeys(context)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.Top
    ) {

        Text(
            "Settings",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        ExpandableSection(
            title = "KEYS",
            initiallyExpanded = true
        ) {

            // -------------------------------------------------------------
            // 1. JIKA BELUM ADA KEY → Tampilkan tombol Generate
            // -------------------------------------------------------------
            if (key == null) {
                SettingItemClickable(
                    title = "Generate SSH Key",
                    subtitle = "Buat pasangan kunci baru untuk koneksi SSH",
                    icon = Icons.Default.VpnKey
                ) {
                    showGenerateDialog = true
                }
            }

            // -------------------------------------------------------------
            // 2. JIKA SUDAH ADA KEY → Tampilkan isi key
            // -------------------------------------------------------------
            else {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable{
                            connectionViewModel.onEvent(
                                ConnectionUiEvent.OnPrivateKeySelected(
                                    path = key.privateFile,
                                    filename = key.name
                                )
                            )
                            navController.popBackStack()
                        },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {

                        Text("Public Key:", style = MaterialTheme.typography.titleMedium)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 80.dp, max = 200.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            Text(
                                text = key.publicKeyContent ?: "(Failed to load public key)",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        Spacer(Modifier.height(12.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {

                            OutlinedButton(onClick = {
                                clipboard.setText(
                                    AnnotatedString(key.publicKeyContent ?: "")
                                )
                            }) {
                                Text("Copy Public Key")
                            }

                            OutlinedButton(
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = Color.Red
                                ),
                                onClick = {
                                    keyToDelete = key
                                    showDeleteDialog = true
                                }
                            ) {
                                Text("Delete Key")
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        SettingItemSwitch(
            title = "Dark Mode",
            icon = Icons.Default.SettingsBrightness,
            checked = themeViewModel.isDarkMode,
            onCheckedChange = { themeViewModel.toggleTheme() }
        )
    }

    // ------------------------------------------------------------
    // 3. DIALOG: PILIH AUTO / MANUAL
    // ------------------------------------------------------------
    if (showGenerateDialog) {
        AlertDialog(
            onDismissRequest = { showGenerateDialog = false },
            title = { Text("Generate SSH Key") },
            text = { Text("Pilih metode pembuatan SSH Key:") },
            confirmButton = {
                TextButton(onClick = {
                    showGenerateDialog = false
                    keyViewModel.generateKeyAuto(context)
                }) { Text("Auto Generate") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showGenerateDialog = false
                    showManualDialog = true
                }) { Text("Manual (Dengan Passphrase)") }
            }
        )
    }

    // ------------------------------------------------------------
    // 4. DIALOG INPUT PASSPHRASE (MANUAL MODE)
    // ------------------------------------------------------------
    if (showManualDialog) {
        AlertDialog(
            onDismissRequest = { showManualDialog = false },
            title = { Text("Manual Key Generation") },
            text = {
                Column {
                    Text("Masukkan passphrase untuk key Anda:")
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = passphrase,
                        onValueChange = { passphrase = it },
                        placeholder = { Text("Passphrase") }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showManualDialog = false
                    keyViewModel.generateKeyManual(context, passphrase)
                    passphrase = ""
                }) { Text("Generate") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showManualDialog = false
                    passphrase = ""
                }) { Text("Cancel") }
            }
        )
    }

    if (showDeleteDialog && keyToDelete != null) {
        AlertDialog(
            onDismissRequest = {
                showDeleteDialog = false
                keyToDelete = null
            },
            title = { Text("Delete SSH Key") },
            text = {
                Text("Apakah Anda yakin ingin menghapus SSH key ini?")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        keyViewModel.deleteKey(context)
                        showDeleteDialog = false
                        keyToDelete = null
                    }
                ) {
                    Text("Delete", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        keyToDelete = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun SettingItemSwitch(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {

        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(14.dp))
            Text(title, style = MaterialTheme.typography.bodyLarge)
        }

        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
fun SettingItemClickable(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(16.dp))

            Column {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun ExpandableSection(
    title: String,
    initiallyExpanded: Boolean = true,
    content: @Composable () -> Unit
) {
    var expanded by remember { mutableStateOf(initiallyExpanded) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )

            Icon(
                imageVector = if (expanded) Icons.Default.KeyboardArrowDown
                else Icons.Default.KeyboardArrowRight,
                contentDescription = null
            )
        }

        if (expanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp)
            ) {
                content()
            }
        }
    }
}