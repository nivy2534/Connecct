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

fun pathShortener(path: String):String{
    val clean = path.trimEnd('/')

    if(clean.isEmpty() || clean == "/") return "/"

    val parts = clean.split("/").filter{it.isNotBlank()}

    val hasDrive = parts.firstOrNull()?.endsWith(":") == true

    return when{
        parts.size <= 2 -> clean
        hasDrive -> parts.first() + "/../" + parts.takeLast(1).joinToString("/")
        else -> "../" + parts.takeLast(1).joinToString("/")
    }

}