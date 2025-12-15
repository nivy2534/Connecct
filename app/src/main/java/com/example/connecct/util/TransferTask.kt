package com.example.connecct.util

import android.net.Uri
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.UUID

enum class TransferType{
    UPLOAD, DOWNLOAD
}

enum class TransferStatus{
    QUEUED,
    IN_PROGRESS,
    COMPLETED,
    FAILED,
    CANCELED
}

data class TransferTask (
    val id: String = UUID.randomUUID().toString(),
    val type: TransferType,
    val fileName: String,
    val remotePath: String? = null,
    val localUri: Uri? = null,
    val localFile: File? = null,
    val progress: Long = 0L,
    val status: TransferStatus = TransferStatus.QUEUED,
    val error: String? = null
)