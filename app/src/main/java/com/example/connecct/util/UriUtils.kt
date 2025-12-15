package com.example.connecct.util

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns

fun ContentResolver.getFilename(uri: Uri): String?{
    val cursor = query(uri, null, null, null, null) ?: return null
    return cursor.use{
        val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (nameIndex == -1) null
        else {
            it.moveToFirst()
            it.getString(nameIndex)
        }
    }
}