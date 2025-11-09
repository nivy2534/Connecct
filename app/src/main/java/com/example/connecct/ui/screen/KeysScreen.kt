package com.example.connecct.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import com.example.connecct.Conn.generateKey
import java.io.File
import android.content.Context
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast

data class SSHKeyInfo(
    val username: String,
    val publicKey: String,
    val privateKeyPath: String,
    val hasPassphrase: Boolean
)

@Composable
fun KeysScreen(
    onKeySelected: (SSHKeyInfo) -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var keys by remember { mutableStateOf(listOf<SSHKeyInfo>()) }
    var isGenerating by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    coroutineScope.launch {
                        isGenerating = true
                        try {
                            // Lokasi penyimpanan di direktori app
                            val dir = File(context.filesDir, "ssh_keys")
                            if (!dir.exists()) dir.mkdirs()

                            val keyname = "id_rsa_${System.currentTimeMillis()}"
                            val (privFile, pubFile) = generateKey.generateKeyPair(
                                keyname = keyname,
                                outputDir = dir,
                                passphrase = null // Bisa kamu isi nanti
                            )

                            val pubContent = pubFile.readText()

                            // Tambahkan ke daftar UI
                            keys = keys + SSHKeyInfo(
                                username = "user@device",
                                publicKey = pubContent,
                                privateKeyPath = privFile.absolutePath,
                                hasPassphrase = false
                            )

                            Toast.makeText(context, "SSH key berhasil dibuat", Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            Toast.makeText(context, "Gagal membuat key: ${e.message}", Toast.LENGTH_LONG).show()
                        } finally {
                            isGenerating = false
                        }
                    }
                }
            ) {
                Text(if (isGenerating) "..." else "+")
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            if (keys.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Belum ada key yang dibuat.", style = MaterialTheme.typography.bodyLarge)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(keys) { key ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { onKeySelected(key) }
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = key.username,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = key.publicKey.take(80) + "...",
                                    style = MaterialTheme.typography.bodySmall,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Lokasi: ${key.privateKeyPath}",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
