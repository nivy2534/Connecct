package com.example.connecct.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.SettingsBrightness
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.connecct.viewmodel.ThemeViewModel
import com.example.connecct.ui.viewmodel.KeysViewModel  // jika pakai ViewModel berbeda
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.ui.graphics.Color

@Composable
fun SettingsScreen(
    themeViewModel: ThemeViewModel,
    keyViewModel: KeysViewModel
) {
    val context = LocalContext.current

    // 👇 STATE BARU UNTUK POPUP DIALOG
    var showGenerateDialog by remember { mutableStateOf(false) }
    var showManualDialog by remember { mutableStateOf(false) }
    var passphrase by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
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
            SettingItemClickable(
                title = "Generate SSH Key",
                subtitle = "Buat pasangan kunci baru untuk koneksi SSH",
                icon = Icons.Default.VpnKey
            ) {
                showGenerateDialog = true    // 👈 buka dialog pilihan
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
    // 🔥 DIALOG 1 — PILIH AUTO ATAU MANUAL
    // ------------------------------------------------------------
    if (showGenerateDialog) {
        AlertDialog(
            onDismissRequest = { showGenerateDialog = false },
            title = { Text("Generate SSH Key") },
            text = { Text("Pilih metode pembuatan SSH Key:") },
            confirmButton = {
                TextButton(onClick = {
                    showGenerateDialog = false
                    keyViewModel.generateKeyAuto(context)   // 👈 Auto Generate
                }) {
                    Text("Auto Generate")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showGenerateDialog = false
                    showManualDialog = true                // 👈 lanjut ke dialog manual
                }) {
                    Text("Manual (Dengan Passphrase)")
                }
            }
        )
    }

    // ------------------------------------------------------------
    // 🔥 DIALOG 2 — INPUT PASSPHRASE (MANUAL MODE)
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
                    keyViewModel.generateKeyManual(context, passphrase) // 👈 Manual generate
                    passphrase = ""
                }) {
                    Text("Generate")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showManualDialog = false
                    passphrase = ""
                }) {
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

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
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
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        )
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp),
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

        // Header (klik untuk expand/collapse)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(vertical = 8.dp), // NO background
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )

            Icon(
                imageVector = if (expanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowRight,
                contentDescription = null
            )
        }

        // Body
        if (expanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp) // ONLY padding, no background
            ) {
                content()
            }
        }
    }
}