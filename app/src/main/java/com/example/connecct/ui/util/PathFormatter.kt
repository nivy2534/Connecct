package com.example.connecct.ui.util

fun formatPath(
    fullPath: String,
    maxSegments: Int = 3
): String {

    if (fullPath.isBlank()) return fullPath

    val normalized = fullPath.replace("\\", "/")
    val parts = normalized.split("/").filter { it.isNotBlank() }

    // Kalau pendek, tampilkan full
    if (parts.size <= maxSegments) {
        return "/" + parts.joinToString("/")
    }

    val tail = parts.takeLast(maxSegments)

    return ".../" + tail.joinToString("/")
}