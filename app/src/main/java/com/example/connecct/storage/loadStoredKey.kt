package com.example.connecct.storage

import android.content.Context
import android.util.Log
import com.example.connecct.ui.viewmodel.SSHKeys
import java.io.File

class loadStorageKey(private val context: Context){
    suspend fun loadKeys(): List<SSHKeys>{
        val dir = File(context.filesDir, "ssh_keys")
        Log.d("SSH_KEYS", "Dir: ${dir.absolutePath}, exists=${dir.exists()}")

        if (!dir.exists()) return emptyList()

        dir.listFiles()?.forEach {
            Log.d("SSH_KEYS", "Found file: ${it.name}")
        }

        return dir.listFiles()?.mapNotNull { file ->
            if (file.extension == "pub") {
                val privateFile = File(dir, file.nameWithoutExtension)
                SSHKeys(
                    name = file.nameWithoutExtension,
                    type = "RSA",
                    privateFile = privateFile.absolutePath,
                    publicFile = file.absolutePath,
                    addedAt = file.lastModified().toString()
                )
            } else null
        } ?: emptyList()
    }

    fun readPublicKeyContent(pubFilePath: String): String {
        return try {
            File(pubFilePath).readText()
        } catch (e: Exception) {
            "Error reading public key: ${e.message}"
        }
    }
}