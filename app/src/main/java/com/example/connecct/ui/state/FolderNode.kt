package com.example.connecct.ui.state

data class FolderNode(
    val name: String,
    val fullPath: String,
    val children: List<FolderNode> = emptyList(),
    val isExpanded: Boolean = false
)
