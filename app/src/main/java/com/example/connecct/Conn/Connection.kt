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

        val newSsh = SSHClient()
        newSsh.addHostKeyVerifier(PromiscuousVerifier())
        newSsh.connect(host)

        val keyUri = Uri.parse(privateKeyPath)
        val tempKeyFile = File(context.cacheDir, "temp_id_key.pem")

        try {
            Log.d("SSH_CONNECT", "Key URI: $keyUri")

            // Copy private key dari URI ke file sementara
            context.contentResolver.openInputStream(keyUri)?.use { input ->
                val content = input.bufferedReader().use { it.readText() }
                Log.d("SSH_CONNECT", "Private Key Content (Snippet): ${content.take(200)}...")
                tempKeyFile.writeText(content)
            }

            Log.d("SSH_CONNECT", "Temp key file path: ${tempKeyFile.absolutePath}")
            Log.d("SSH_CONNECT", "Passphrase used: ${if (passphrase.isBlank()) "(none)" else "(provided)"}")


            // =======================================================
            // SOLUSI STABIL: Menggunakan newSsh.loadKeys + PasswordFinder
            // (Membutuhkan Private Key dalam format PEM/PKCS#1)
            // =======================================================

            val passwordFinder = if (passphrase.isNotBlank()) {
                object : PasswordFinder {
                    override fun reqPassword(resource: Resource<*>?): CharArray {
                        return passphrase.toCharArray()
                    }
                    override fun shouldRetry(resource: Resource<*>?): Boolean {
                        return false
                    }
                }
            } else {
                null
            }

            // loadKeys adalah metode yang paling andal untuk file PEM
            val keyProvider = newSsh.loadKeys(tempKeyFile.absolutePath, passwordFinder)

            Log.d("SSH_CONNECT", "Key loaded successfully using loadKeys.")

            // Authenticate
            newSsh.authPublickey(username, keyProvider)

            ssh = newSsh
        } catch (e: Exception) {
            // Log error
            Log.e("SSH_CONNECT", "Connection attempt failed: ${e::class.simpleName} - ${e.message}", e)

            // Bersihkan koneksi
            try { newSsh.disconnect() } catch (ignored: Exception) {}

            // Lempar kembali error ke CoroutineScope
            throw e
        } finally {
            // Hapus file sementara
            tempKeyFile.delete()
        }
    }

    // ... (isConnected dan disconnect tetap sama) ...

    fun isConnected(): Boolean {
        return ssh?.isAuthenticated == true
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