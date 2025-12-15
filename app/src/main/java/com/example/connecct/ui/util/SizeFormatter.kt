package com.example.connecct.ui.util

fun formatFileSize(bytes: Long): String {
    if (bytes < 1000) return "$bytes B"

    val units = arrayOf("KB", "MB", "GB", "TB")
    var size = bytes.toDouble()
    var unitIndex = -1

    while (size >= 1000 && unitIndex < units.lastIndex) {
        size /= 1000
        unitIndex++
    }

    return String.format("%.2f %s", size, units[unitIndex])
}