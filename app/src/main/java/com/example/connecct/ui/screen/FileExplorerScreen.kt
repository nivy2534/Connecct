package com.example.connecct.ui.screen

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MoreVert
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
import com.example.connecct.ui.state.FolderNode
import com.example.connecct.ui.state.OpenedFile
import com.example.connecct.ui.state.RemoteFile
import com.example.connecct.ui.viewmodel.ConnectionViewModel

@Composable
fun FileExplorerScreen(viewModel: ConnectionViewModel) {

    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // ================= MOVE STATE =================
    var showMoveDialog by remember { mutableStateOf(false) }
    var moveTargetFile by remember { mutableStateOf<RemoteFile?>(null) }
    var selectedTargetDir by remember { mutableStateOf<FolderNode?>(null) }

    // ✅ TREE ROOT
    var folderTree by remember {
        mutableStateOf(
            listOf(
                FolderNode("C:/", "C:/"),
                FolderNode("E:/", "E:/")
            )
        )
    }

    // ================= LOAD =================
    LaunchedEffect(state.connectionStatus) {
        if (state.connectionStatus == ConnectionStatus.CONNECTED) {
            viewModel.onEvent(ConnectionUiEvent.LoadDirectory)
        }
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) viewModel.uploadFile(uri, context)
    }

    // ================= MAIN UI =================
    Column(Modifier.fillMaxSize().padding(16.dp)) {

        Row(verticalAlignment = Alignment.CenterVertically) {

            Button(onClick = {
                viewModel.onEvent(ConnectionUiEvent.NavigateUp)
            }) { Text("Up") }

            Spacer(Modifier.width(12.dp))

            Text("Path: ${state.currentPath}", modifier = Modifier.weight(1f))

            IconButton(onClick = {
                viewModel.onEvent(ConnectionUiEvent.LoadDirectory)
            }) { Icon(Icons.Default.Refresh, null) }

            IconButton(onClick = { launcher.launch("*/*") }) {
                Icon(Icons.Default.Add, null)
            }
        }

        Spacer(Modifier.height(16.dp))

        if (state.isLoadingDirectory) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return
        }

        LazyColumn {
            items(state.remoteFiles, key = { it.name }) { file ->
                RemoteFileItem(
                    file = file,
                    onClick = {
                        if (file.isDirectory)
                            viewModel.onEvent(ConnectionUiEvent.NavigateTo(file.name))
                        else
                            viewModel.onEvent(ConnectionUiEvent.OpenFile(file.name, context))
                    },
                    onDelete = {
                        viewModel.onEvent(ConnectionUiEvent.DeleteFile(file.name))
                    },
                    onMove = {
                        moveTargetFile = file
                        selectedTargetDir = null
                        showMoveDialog = true
                    }
                )
            }
        }
    }

    // ================= MOVE POPUP TREE =================
    if (showMoveDialog && moveTargetFile != null) {
        AlertDialog(
            onDismissRequest = {
                showMoveDialog = false
                moveTargetFile = null
            },
            title = { Text("Pilih Folder Tujuan") },

            text = {
                LazyColumn(Modifier.height(340.dp)) {
                    folderTree.forEach { root ->
                        item {
                            FolderTreeItem(
                                node = root,
                                level = 0,
                                selectedNode = selectedTargetDir,
                                onArrowClick = { clicked ->
                                    viewModel.loadDirectoryAt(clicked.fullPath) { result ->

                                        val newChildren = result
                                            .filter { it.isDirectory }
                                            .map { rf ->
                                                FolderNode(
                                                    name = rf.name,
                                                    fullPath = clicked.fullPath.trimEnd('/') + "/" + rf.name
                                                )
                                            }

                                        folderTree = updateFolderTree(
                                            nodes = folderTree,
                                            targetPath = clicked.fullPath,
                                            newChildren = newChildren
                                        )
                                    }
                                },
                                onSelect = { selectedTargetDir = it }
                            )
                        }
                    }
                }
            },

            confirmButton = {
                Button(
                    enabled = selectedTargetDir != null,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selectedTargetDir != null)
                            Color(0xFF4CAF50) else Color.Gray
                    ),
                    onClick = {
                        val targetPath =
                            selectedTargetDir!!.fullPath.trimEnd('/') + "/" +
                                    moveTargetFile!!.name

                        viewModel.onEvent(
                            ConnectionUiEvent.MoveFile(
                                moveTargetFile!!.name,
                                targetPath
                            )
                        )

                        showMoveDialog = false
                        moveTargetFile = null
                        selectedTargetDir = null
                    }
                ) { Text("Konfirmasi") }
            },

            dismissButton = {
                TextButton(onClick = {
                    showMoveDialog = false
                    moveTargetFile = null
                    selectedTargetDir = null
                }) { Text("Batal") }
            }
        )
    }

    // ================= UPLOAD PROGRESS POPUP =================
    if (state.isUploading) {
        AlertDialog(
            onDismissRequest = {}, // ❌ tidak bisa ditutup manual
            confirmButton = {},
            title = { Text("Uploading...") },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {

                    LinearProgressIndicator(
                        progress = state.uploadProgress / 100f,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(12.dp))

                    Text("${state.uploadProgress}%")
                }
            }
        )
    }
}

fun updateFolderTree(
    nodes: List<FolderNode>,
    targetPath: String,
    newChildren: List<FolderNode>
): List<FolderNode> {
    return nodes.map { node ->
        if (node.fullPath == targetPath) {
            node.copy(
                isExpanded = !node.isExpanded,
                children = newChildren
            )
        } else {
            node.copy(
                children = updateFolderTree(
                    node.children,
                    targetPath,
                    newChildren
                )
            )
        }
    }
}


// ================= ITEM ROW =================
@Composable
fun RemoteFileItem(
    file: RemoteFile,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onMove: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable { onClick() },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {

        Text(
            text = if (file.isDirectory) "📁 ${file.name}" else "📄 ${file.name}",
            modifier = Modifier.weight(1f)
        )

        if (!file.isDirectory) {
            Text("${file.size} bytes", modifier = Modifier.padding(end = 8.dp))
        }

        Box {
            IconButton(onClick = { expanded = true }) {
                Icon(Icons.Default.MoreVert, contentDescription = "Menu")
            }

            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {

                DropdownMenuItem(
                    text = { Text("Delete") },
                    onClick = {
                        expanded = false
                        onDelete()
                    }
                )

                DropdownMenuItem(
                    text = { Text("Move") },
                    onClick = {
                        expanded = false
                        onMove()
                    }
                )
            }
        }
    }
}

@Composable
fun FolderTreeItem(
    node: FolderNode,
    level: Int,
    selectedNode: FolderNode?,
    onArrowClick: (FolderNode) -> Unit,
    onSelect: (FolderNode) -> Unit
) {
    val isSelected = selectedNode?.fullPath == node.fullPath

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = (level * 16).dp)
                .background(
                    if (isSelected) Color(0xFF2E7D32) else Color.Transparent,
                    shape = MaterialTheme.shapes.small
                )
                .padding(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            IconButton(
                onClick = { onArrowClick(node) },
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = if (node.isExpanded)
                        Icons.Default.KeyboardArrowDown
                    else
                        Icons.Default.KeyboardArrowRight,
                    contentDescription = null
                )
            }

            Icon(
                imageVector = Icons.Default.Folder,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(Modifier.width(8.dp))

            Text(
                text = node.name,
                modifier = Modifier.clickable {
                    onSelect(node)
                },
                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
            )
        }

        if (node.isExpanded) {
            node.children.forEach {
                FolderTreeItem(
                    node = it,
                    level = level + 1,
                    selectedNode = selectedNode,
                    onArrowClick = onArrowClick,
                    onSelect = onSelect
                )
            }
        }
    }
}
