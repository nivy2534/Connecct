package com.example.connecct.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.connecct.ui.components.KeyCard
import com.example.connecct.ui.viewmodel.KeysViewModel
import com.example.connecct.ui.viewmodel.SSHKey

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KeysScreen(viewModel: KeysViewModel = viewModel()) {
    val keys by remember { derivedStateOf { viewModel.keys } }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.addKey("My Key ${keys.size + 1}", if (keys.size % 2 == 0) "RSA" else "ED25519") },
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
                        KeyCard(key = key, onDelete = { viewModel.deleteKey(it) })
                    }
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