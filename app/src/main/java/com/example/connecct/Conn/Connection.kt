package com.example.connecct.Conn

import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.transport.verification.PromiscuousVerifier
import java.security.Security
import android.content.Context
import android.net.Uri
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.io.File
import java.io.FileOutputStream

class Connection {
    private var ssh: SSHClient? = null

    suspend fun connect(
        context: Context,
        host: String,
        username: String,
        privateKeyPath: String,
        passphrase: String = ""
    ) {
        disconnect()

        val newSsh = SSHClient()
        newSsh.addHostKeyVerifier(PromiscuousVerifier())
        newSsh.connect(host)

        val keyUri = Uri.parse(privateKeyPath)
        val tempKeyFile = File(context.cacheDir, "temp_id_key.pem")

        try {
            // Copy private key dari URI ke file sementara
            context.contentResolver.openInputStream(keyUri)?.use { input ->
                FileOutputStream(tempKeyFile).use { output ->
                    input.copyTo(output)
                }
            }

            // Tambahkan BouncyCastle jika belum ada
            if (Security.getProvider("BC") == null) {
                Security.addProvider(BouncyCastleProvider())
            }

            // Load RSA key dengan passphrase
            val keyProvider = newSsh.loadKeys(
                tempKeyFile.absolutePath,
                passphrase.ifBlank { null },
                null
            )

            // Authenticate
            newSsh.authPublickey(username, keyProvider)

            ssh = newSsh
        } catch (e: Exception) {
            newSsh.disconnect()
            throw e
        } finally {
            // Hapus file sementara
            tempKeyFile.delete()
        }
    }

    fun isConnected(): Boolean {
        return ssh?.isAuthenticated == true
    }

    fun disconnect() {
        ssh?.disconnect()
        ssh = null
    }

    internal fun getClient(): SSHClient? = ssh
}
