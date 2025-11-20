package com.example.connecct.storage

import android.content.Context
import android.util.Log
import com.example.connecct.ui.viewmodel.SSHKeys
import com.example.connecct.util.SshKeyUtils
import java.io.File
import java.security.KeyPair
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

class LoadStorageKey(private val context: Context) {

    // ----------------------------------------------------------
    // LOAD STORED KEYS
    // ----------------------------------------------------------
    suspend fun loadKeys(): List<SSHKeys> {
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

    // ----------------------------------------------------------
    // AUTO GENERATE SSH KEY
    // ----------------------------------------------------------
    fun generateAutoKey(): SSHKeys? = generateAndStoreKey(null)

    // ----------------------------------------------------------
    // MANUAL (WITH PASSPHRASE)
    // ----------------------------------------------------------
    fun generateManualKey(passphrase: String): SSHKeys? =
        generateAndStoreKey(passphrase)

    // ----------------------------------------------------------
    // CORE GENERATOR + SAVE
    // ----------------------------------------------------------
    private fun generateAndStoreKey(passphrase: String?): SSHKeys? {
        return try {
            val dir = File(context.filesDir, "ssh_keys")
            if (!dir.exists()) dir.mkdirs()

            val keyName = "id_rsa_${System.currentTimeMillis()}"

            val privateKeyFile = File(dir, keyName)
            val publicKeyFile = File(dir, "$keyName.pub")

            // --- GENERATE RSA KEYPAIR 2048 ---
            val keyPairGenerator = java.security.KeyPairGenerator.getInstance("RSA")
            keyPairGenerator.initialize(2048)
            val keyPair: KeyPair = keyPairGenerator.genKeyPair()

            // --- PRIVATE KEY ---
            var privateKeyBytes = keyPair.private.encoded

            if (passphrase != null) {
                privateKeyBytes = encryptPrivateKey(privateKeyBytes, passphrase)
            }

            privateKeyFile.writeBytes(privateKeyBytes)

            // --- PUBLIC KEY (OpenSSH format) ---
            val pubSsh = SshKeyUtils.convertPublicKeyToOpenSshFormat(
                keyPair.public as java.security.interfaces.RSAPublicKey
            )

            publicKeyFile.writeText("$pubSsh $keyName")

            return SSHKeys(
                name = keyName,
                type = "RSA",
                privateFile = privateKeyFile.absolutePath,
                publicFile = publicKeyFile.absolutePath,
                addedAt = System.currentTimeMillis().toString()
            )

        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // ----------------------------------------------------------
    // DELETE ALL KEYS (PRIVATE + PUBLIC)
    // ----------------------------------------------------------
    fun deleteKeys() {
        try {
            val dir = File(context.filesDir, "ssh_keys")
            if (!dir.exists()) return

            dir.listFiles()?.forEach { file ->
                file.delete()
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // ----------------------------------------------------------
    // AES ENCRYPTION FOR PRIVATE KEY
    // ----------------------------------------------------------
    private fun encryptPrivateKey(bytes: ByteArray, passphrase: String): ByteArray {
        val key = MessageDigest.getInstance("SHA-256").digest(passphrase.toByteArray())
        val secretKey = SecretKeySpec(key, "AES")

        val cipher = Cipher.getInstance("AES")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)

        return cipher.doFinal(bytes)
    }

    // ----------------------------------------------------------
    fun readPublicKeyContent(path: String): String {
        return try {
            File(path).readText()
        } catch (e: Exception) {
            "Error reading public key: ${e.message}"
        }
    }
}
