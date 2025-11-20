package com.example.connecct.storage

import android.content.Context
import com.example.connecct.ui.viewmodel.SSHKeys
import com.example.connecct.util.SshKeyUtils
import java.io.File
import java.security.KeyPair
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

class LoadStorageKey(private val context: Context) {

    // ----------------------------------------------------------
    // LOAD STORED KEYS (read content too)
    // ----------------------------------------------------------
    suspend fun loadKeys(): List<SSHKeys> {
        val dir = File(context.filesDir, "ssh_keys")
        if (!dir.exists()) return emptyList()

        return dir.listFiles()?.mapNotNull { file ->
            if (file.extension == "pub") {

                val privateFile = File(dir, file.nameWithoutExtension)

                val pubText = runCatching { file.readText() }.getOrElse { "" }
                val privText = runCatching { privateFile.readText() }.getOrElse { "" }

                SSHKeys(
                    name = file.nameWithoutExtension,
                    type = "RSA",
                    privateFile = privateFile.absolutePath,
                    publicFile = file.absolutePath,
                    addedAt = file.lastModified().toString(),
                    privateKeyContent = privText,
                    publicKeyContent = pubText
                )
            } else null
        } ?: emptyList()
    }

    // ----------------------------------------------------------
    fun generateAutoKey(): SSHKeys? = generateAndStoreKey(null)

    fun generateManualKey(passphrase: String): SSHKeys? =
        generateAndStoreKey(passphrase)

    // ----------------------------------------------------------
    // Generate + return content
    // ----------------------------------------------------------
    private fun generateAndStoreKey(passphrase: String?): SSHKeys? {
        return try {
            val dir = File(context.filesDir, "ssh_keys")
            if (!dir.exists()) dir.mkdirs()

            val keyName = "id_rsa_${System.currentTimeMillis()}"

            val privateKeyFile = File(dir, keyName)
            val publicKeyFile = File(dir, "$keyName.pub")

            // --- RSA KEYPAIR ---
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

            // ---- read content for UI ----
            val privateContent = privateKeyFile.readText()
            val publicContent = publicKeyFile.readText()

            SSHKeys(
                name = keyName,
                type = "RSA",
                privateFile = privateKeyFile.absolutePath,
                publicFile = publicKeyFile.absolutePath,
                addedAt = System.currentTimeMillis().toString(),
                privateKeyContent = privateContent,
                publicKeyContent = publicContent
            )

        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // ----------------------------------------------------------
    fun deleteKeys() {
        try {
            val dir = File(context.filesDir, "ssh_keys")
            if (!dir.exists()) return

            dir.listFiles()?.forEach { it.delete() }

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // ----------------------------------------------------------
    // AES ENCRYPTION
    // ----------------------------------------------------------
    private fun encryptPrivateKey(bytes: ByteArray, passphrase: String): ByteArray {
        val key = MessageDigest.getInstance("SHA-256").digest(passphrase.toByteArray())
        val secretKey = SecretKeySpec(key, "AES")

        val cipher = Cipher.getInstance("AES")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)

        return cipher.doFinal(bytes)
    }

    // Optional
    fun readPublicKeyContent(path: String): String {
        return try { File(path).readText() }
        catch (e: Exception) { "Error reading public key: ${e.message}" }
    }
}