    package com.example.connecct.Conn

    import android.content.Context
    import android.os.Build
    import android.util.Log
    import androidx.annotation.RequiresApi
    import com.example.connecct.ui.state.RemoteFile
    import net.schmizz.sshj.common.StreamCopier
    import net.schmizz.sshj.sftp.SFTPClient
    import net.schmizz.sshj.xfer.TransferListener
    import java.io.ByteArrayOutputStream
    import java.io.File
    import java.io.InputStream

    class Transport(private val connection: Connection) {

        // ----------------------------------------------------------------------
        // COMMAND EXECUTION
        // ----------------------------------------------------------------------
        @RequiresApi(Build.VERSION_CODES.TIRAMISU)
        fun executeCommand(command: String): String {
            val ssh = connection.getClient() ?: throw IllegalStateException("Not Connected")

            ssh.startSession().use { session ->
                val cmd = session.exec(command)
                val output = ByteArrayOutputStream()
                cmd.inputStream.copyTo(output)
                cmd.join()
                return output.toString(Charsets.UTF_8)
            }
        }

        // ----------------------------------------------------------------------
        // SFTP PUT (upload via SFTP)
        // ----------------------------------------------------------------------
        fun sftpPut(input: InputStream, remotePath: String, context: Context) {
            val ssh = connection.getClient()

            if (ssh == null) {
                Log.e("SFTP_PUT", "❌ getClient() returned null. Cannot upload to $remotePath")
                throw IllegalStateException("Not Connected (SSH client null)")
            }

            Log.d(
                "SFTP_PUT",
                "=== sftpPut() start === remotePath=$remotePath, " +
                        "socketConnected=${ssh.isConnected}, authenticated=${ssh.isAuthenticated}"
            )

            val sftp: SFTPClient = try {
                ssh.newSFTPClient().also {
                    Log.d("SFTP_PUT", "✅ newSFTPClient() created.")
                }
            } catch (e: Exception) {
                Log.e("SFTP_PUT", "❌ Failed to create SFTP client: ${e.message}", e)
                throw e
            }

            // Simpan inputstream ke file temporary
            val tempFile = File.createTempFile("upload_", ".tmp", context.cacheDir)
            Log.d("SFTP_PUT", "📄 Temp file created at: ${tempFile.absolutePath}")

            try {
                tempFile.outputStream().use { output ->
                    val copied = input.copyTo(output)
                    Log.d("SFTP_PUT", "📥 Copied $copied bytes from InputStream to temp file")
                }

                Log.d("SFTP_PUT", "⏫ Calling sftp.put(tempFile='${tempFile.absolutePath}', remote='$remotePath')")

                try {
                    sftp.put(tempFile.absolutePath, remotePath)
                    Log.d("SFTP_PUT", "✅ sftp.put() completed for $remotePath")
                } catch (e: Exception) {
                    Log.e("SFTP_PUT", "❌ Error during sftp.put(): ${e.message}", e)
                    throw e
                }

            } finally {
                try {
                    if (tempFile.exists()) {
                        val deleted = tempFile.delete()
                        Log.d("SFTP_PUT", "🧹 Temp file deleted=$deleted")
                    }
                } catch (e: Exception) {
                    Log.e("SFTP_PUT", "⚠️ Failed to delete temp file: ${e.message}", e)
                }

                try {
                    sftp.close()
                    Log.d("SFTP_PUT", "🔐 SFTP client closed.")
                } catch (e: Exception) {
                    Log.e("SFTP_PUT", "⚠️ Error closing SFTP client: ${e.message}", e)
                }

                Log.d("SFTP_PUT", "=== sftpPut() end ===")
            }
        }


        // ----------------------------------------------------------------------
        // SCP UPLOAD (WITH PROGRESS)
        // ----------------------------------------------------------------------
        fun uploadFile(localPath: String, remotePath: String, onProgress: (Float) -> Unit = {}) {
            val ssh = connection.getClient() ?: throw IllegalStateException("Not Connected")
            val scp = ssh.newSCPFileTransfer()
            val file = File(localPath)

            if (!file.exists()) throw IllegalArgumentException("Local file does not exist: $localPath")

            onProgress(0f)

            scp.setTransferListener(object : TransferListener {
                override fun directory(name: String?) = this

                override fun file(name: String?, size: Long) = StreamCopier.Listener { transferred ->
                    if (size > 0) {
                        val progress = transferred.toFloat() / size.toFloat()
                        onProgress(progress.coerceIn(0f, 1f))
                    }
                }
            })

            scp.upload(localPath, remotePath)
            onProgress(1f)
        }

        // ----------------------------------------------------------------------
        // SCP DOWNLOAD (WITH PROGRESS)
        // ----------------------------------------------------------------------
        fun downloadFile(remotePath: String, localPath: String, onProgress: (Float) -> Unit = {}) {
            val ssh = connection.getClient() ?: throw IllegalStateException("Not Connected")
            val scp = ssh.newSCPFileTransfer()

            onProgress(0f)

            scp.setTransferListener(object : TransferListener {
                override fun directory(name: String?) = this

                override fun file(name: String?, size: Long) = StreamCopier.Listener { transferred ->
                    if (size > 0) {
                        val progress = transferred.toFloat() / size.toFloat()
                        onProgress(progress.coerceIn(0f, 1f))
                    }
                }
            })

            scp.download(remotePath, localPath)
            onProgress(1f)
        }

        // ----------------------------------------------------------------------
        // REMOTE DIRECTORY LISTING
        // ----------------------------------------------------------------------
        fun listDirectory(path: String): List<RemoteFile> {
            val ssh = connection.getClient() ?: throw IllegalStateException("Not Connected")
            val sftp = ssh.newSFTPClient()

            val list = sftp.ls(path)
            sftp.close()

            return list
                .filter { it.name != "." && it.name != ".." }
                .map { f ->
                    RemoteFile(
                        name = f.name,
                        size = f.attributes.size ?: 0L,
                        isDirectory = f.attributes.type == net.schmizz.sshj.sftp.FileMode.Type.DIRECTORY
                    )
                }
        }

        // ----------------------------------------------------------------------
        // READ TEXT FILE
        // ----------------------------------------------------------------------
        fun readFile(path: String, context: Context): String {
            val tempFile = File.createTempFile("ssh_read_", ".tmp", context.cacheDir)
            downloadFile(path, tempFile.absolutePath)
            return tempFile.readText(Charsets.UTF_8)
        }

        // ----------------------------------------------------------------------
        // READ ANY FILE AS BYTES
        // ----------------------------------------------------------------------
        fun readFileBytes(remotePath: String): ByteArray {
            val ssh = connection.getClient() ?: throw IllegalStateException("Not Connected")
            val sftp = ssh.newSFTPClient()
            val file = sftp.open(remotePath)

            return try {
                val buffer = ByteArray(32 * 1024)
                val output = ByteArrayOutputStream()
                var offset = 0L

                while (true) {
                    val read = file.read(offset, buffer, 0, buffer.size)
                    if (read <= 0) break
                    output.write(buffer, 0, read)
                    offset += read
                }

                output.toByteArray()
            } finally {
                file.close()
                sftp.close()
            }
        }

        // ----------------------------------------------------------------------
        // GET HOME DIRECTORY
        // ----------------------------------------------------------------------
        fun getHomeDirectory(): String {
            val ssh = connection.getClient() ?: throw IllegalStateException("Not Connected")
            val sftp = ssh.newSFTPClient()
            val home = sftp.canonicalize(".")
            sftp.close()
            return home
        }
    }
