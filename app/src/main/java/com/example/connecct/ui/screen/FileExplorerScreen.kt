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
import androidx.compose.material.icons.filled.ArrowBack


@Composable
fun FileExplorerScreen(viewModel: ConnectionViewModel) {

    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // ================= MOVE STATE =================
    var showMoveDialog by remember { mutableStateOf(false) }
    var moveTargetFile by remember { mutableStateOf<RemoteFile?>(null) }
    var selectedTargetDir by remember { mutableStateOf<FolderNode?>(null) }

    // ================= TREE ROOT =================
    var folderTree by remember {
        mutableStateOf(
            listOf(
                FolderNode("C:/", "C:/"),
                FolderNode("E:/", "E:/")
            )
        )
    }

    // ================= LOAD ROOT =================
    LaunchedEffect(state.connectionStatus) {
        if (state.connectionStatus == ConnectionStatus.CONNECTED) {
            viewModel.onEvent(ConnectionUiEvent.LoadDirectory)
        }
    }

    // ================= FILE PICKER =================
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) viewModel.uploadFile(uri, context)
    }

    // ================= SORTED FILES (🔥 INI INTINYA) =================
    val sortedFiles = remember(state.remoteFiles) {
        state.remoteFiles.sortedWith(
            compareBy<RemoteFile>(
                { !it.isDirectory },
                { it.name.lowercase() }
            )
        )
    }

    // ================= MAIN UI =================
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        // ================= TOP BAR =================
        val rootPaths = listOf("/", "C:/", "E:/")
        val showBackButton = state.currentPath !in rootPaths

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {

            if (showBackButton) {
                IconButton(
                    onClick = {
                        viewModel.onEvent(ConnectionUiEvent.NavigateUp)
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Navigate up"
                    )
                }

                Spacer(Modifier.width(12.dp))
            }

            Text(
                text = "Path: ${state.currentPath}",
                modifier = Modifier.weight(1f),
                maxLines = 1
            )

            IconButton(
                onClick = {
                    viewModel.onEvent(ConnectionUiEvent.LoadDirectory)
                }
            ) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh")
            }

            IconButton(
                onClick = { launcher.launch("*/*") }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Upload")
            }
        }

        Spacer(Modifier.height(16.dp))

        // ================= LOADING =================
        if (state.isLoadingDirectory) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            return
        }

        // ================= FILE LIST =================
        LazyColumn {
            items(sortedFiles, key = { it.name }) { file ->

                RemoteFileItem(
                    file = file,

                    onClick = {
                        if (file.isDirectory) {
                            viewModel.onEvent(
                                ConnectionUiEvent.NavigateTo(file.name)
                            )
                        } else {
                            viewModel.onEvent(
                                ConnectionUiEvent.OpenFile(file.name, context)
                            )
                        }
                    },

                    onDelete = {
                        viewModel.onEvent(
                            ConnectionUiEvent.DeleteFile(file.name)
                        )
                    },

                    onMove = {
                        moveTargetFile = file
                        selectedTargetDir = null
                        showMoveDialog = true
                    },

                    onDownload = { remoteFile ->
                        viewModel.onEvent(
                            ConnectionUiEvent.DownloadFile(
                                file = remoteFile,
                                context = context
                            )
                        )
                    },

                    onRename = { oldName, newName ->
                        viewModel.onEvent(
                            ConnectionUiEvent.RenameFile(
                                oldName = oldName,
                                newName = newName
                            )
                        )
                    }
                )
            }
        }
    }

    // ================= MOVE DIALOG =================
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
                                            .map {
                                                FolderNode(
                                                    name = it.name,
                                                    fullPath = clicked.fullPath.trimEnd('/') + "/" + it.name
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
                ) {
                    Text("Konfirmasi")
                }
            },

            dismissButton = {
                TextButton(
                    onClick = {
                        showMoveDialog = false
                        moveTargetFile = null
                        selectedTargetDir = null
                    }
                ) {
                    Text("Batal")
                }
            }
        )
    }

    // ================= UPLOAD PROGRESS =================
    if (state.isUploading) {
        AlertDialog(
            onDismissRequest = {},
            confirmButton = {},
            title = { Text("Uploading...") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
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
    onMove: () -> Unit,
    onDownload: (RemoteFile) -> Unit,
    onRename: (oldName: String, newName: String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf(file.name) }

    // ================= ROW UTAMA =================
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

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {

                DropdownMenuItem(
                    text = { Text("Delete") },
                    onClick = {
                        expanded = false
                        onDelete()
                    }
                )

                DropdownMenuItem(
                    text = { Text("Download") },
                    onClick = {
                        expanded = false
                        onDownload(file)
                    }
                )

                DropdownMenuItem(
                    text = { Text("Move") },
                    onClick = {
                        expanded = false
                        onMove()
                    }
                )

                DropdownMenuItem(
                    text = { Text("Rename") },
                    onClick = {
                        expanded = false
                        newName = file.name   // 🔥 reset setiap buka dialog
                        showRenameDialog = true
                    }
                )
            }
        }
    }

    // ================= RENAME DIALOG =================
    if (showRenameDialog) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Rename File") },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("New name") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(
                    enabled = newName.isNotBlank() && newName != file.name,
                    onClick = {
                        onRename(file.name, newName)
                        showRenameDialog = false
                    }
                ) {
                    Text("Rename")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) {
                    Text("Cancel")
                }
            }
        )
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
