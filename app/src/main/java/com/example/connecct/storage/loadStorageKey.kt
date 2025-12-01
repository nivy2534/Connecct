package com.example.connecct.storage

import android.content.Context
import android.util.Log
import com.example.connecct.ui.viewmodel.SSHKeys
import java.io.File
import java.security.KeyPair
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec
import com.example.connecct.Conn.generateKey

class LoadStorageKey(private val context: Context) {

    private val keyDir: File
        get() = File(context.filesDir, "ssh_keys").apply { mkdirs() }

    private val privFile get() = File(keyDir, "id_rsa")
    private val pubFile  get() = File(keyDir, "id_rsa.pub")

    fun loadKeys(): List<SSHKeys> {
        if (!privFile.exists() || !pubFile.exists()) return emptyList()

        val pubContent = pubFile.readText()
        return listOf(
            SSHKeys(
                name = "id_rsa",
                privateFile = privFile.absolutePath,
                publicFile = pubFile.absolutePath,
                publicKeyContent = pubContent,
            )
        )
    }

    /**
     * getOrCreateAutoKey:
     * - kalau file sudah ada → load & return
     * - kalau belum ada     → generate baru (tanpa passphrase), lalu return
     */
    fun getOrCreateAutoKey(): SSHKeys? {
        if (privFile.exists() && pubFile.exists()) {
            return loadKeys().firstOrNull()
        }

        val (priv, pub) = generateKey.generateKeyPair(
            keyname   = "id_rsa",
            outputDir = keyDir,
            passphrase = null
        )

        val pubContent = pub.readText()
        return SSHKeys(
            name = "id_rsa",
            privateFile = priv.absolutePath,
            publicFile = pub.absolutePath,
            publicKeyContent = pubContent
        )
    }

    fun generateManualKey(passphrase: String): SSHKeys? {
        val (priv, pub) = generateKey.generateKeyPair(
            keyname   = "id_rsa",
            outputDir = keyDir,
            passphrase = passphrase
        )

        val pubContent = pub.readText()
        return SSHKeys(
            name = "id_rsa",
            privateFile = priv.absolutePath,
            publicFile = pub.absolutePath,
            publicKeyContent = pubContent
        )
    }

    fun deleteKeys() {
        privFile.delete()
        pubFile.delete()
    }
}
