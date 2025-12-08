package com.example.connecct.Conn

import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.transport.verification.PromiscuousVerifier
import java.security.Security
import android.content.Context
import android.net.Uri
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.io.File
import java.io.FileOutputStream
import android.util.Log
import net.schmizz.sshj.userauth.password.PasswordFinder
import net.schmizz.sshj.userauth.keyprovider.KeyProvider
import net.schmizz.sshj.userauth.password.Resource
import net.schmizz.sshj.userauth.keyprovider.PEMKey

class Connection {
    private var ssh: SSHClient? = null

    suspend fun connect(
        context: Context,
        host: String,
        username: String,
        privateKeyPath: String,
        passphrase: String,
    ) {
        disconnect()

        val keyFile = File(privateKeyPath)

        if (!keyFile.exists()) {
            Log.e("SSH_CONNECT", "Private key not found at: ${keyFile.absolutePath}")
            throw java.io.FileNotFoundException("Private key not found at: ${keyFile.absolutePath}")
        }

        val newSsh = SSHClient().apply {
            addHostKeyVerifier(PromiscuousVerifier())
            connect(host)
        }

        try {
            Log.d("SSH_CONNECT", "Using key file: ${keyFile.absolutePath}")
            Log.d(
                "SSH_CONNECT",
                "Passphrase used: ${if (passphrase.isBlank()) "(none)" else "(provided)"}"
            )

            val passwordFinder =
                if (passphrase.isNotBlank()) {
                    object : PasswordFinder {
                        override fun reqPassword(resource: Resource<*>?): CharArray {
                            return passphrase.toCharArray()
                        }

                        override fun shouldRetry(resource: Resource<*>?): Boolean = false
                    }
                } else {
                    null
                }

            // Kalau key tanpa passphrase
            val keyProvider: KeyProvider = if (passwordFinder == null) {
                newSsh.loadKeys(keyFile.absolutePath)
            } else {
                newSsh.loadKeys(keyFile.absolutePath, passwordFinder)
            }

            Log.d("SSH_CONNECT", "Key loaded successfully using loadKeys.")

            newSsh.authPublickey(username, keyProvider)

            ssh = newSsh
        } catch (e: Exception) {
            Log.e(
                "SSH_CONNECT",
                "Connection attempt failed: ${e::class.simpleName} - ${e.message}",
                e
            )
            try {
                newSsh.disconnect()
            } catch (_: Exception) {
            }
            throw e
        }
    }


    // ... (isConnected dan disconnect tetap sama) ...

    fun isConnected(): Boolean {
        val client = ssh ?: return false

        val connected = client.isConnected
        val authed = client.isAuthenticated

        Log.d(
            "SSH_CONNECT",
            "isConnected() check -> socketConnected=$connected, authenticated=$authed"
        )

        return connected && authed
    }

    fun disconnect() {
        ssh?.disconnect()
        ssh = null
    }

    // =======================================================
    // PERBAIKAN fun inspectPrivateKey (Kembali ke loadKeys)
    // =======================================================
    fun inspectPrivateKey(context: Context, privateKeyPath: String, passphrase: String = ""):String{

        if (Security.getProvider("BC") == null) {
            Security.addProvider(BouncyCastleProvider())
        }

        val uri = Uri.parse(privateKeyPath)
        val keyFile = File(context.cacheDir, "inspect_key.pem")

        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(keyFile).use { output ->
                input.copyTo(output)
            }
        }

        val ssh = SSHClient() // Objek SSHClient sementara untuk loading
        return try{
            // Gunakan loadKeys karena ini lebih stabil untuk inspeksi kunci umum
            val provider = ssh.loadKeys(keyFile.absolutePath, passphrase.ifBlank { null })

            val info = buildString {
                appendLine("=== Private Key Info ===")
                appendLine("Type: ${provider.type}")
                appendLine("File Size: ${String.format("%.2f KB", keyFile.length() / 1024.0)}")
                appendLine("Path: ${keyFile.absolutePath}")
            }
            info
        }catch (e: Exception){
            "Failed to read key: ${e::class.simpleName} - ${e.message}. Kemungkinan format kunci atau passphrase salah."
        }finally {
            keyFile.delete()
        }
    }

    internal fun getClient(): SSHClient? = ssh
}