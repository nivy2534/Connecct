package com.example.connecct.Conn

import android.content.Context
import android.os.Build
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
        val ssh = connection.getClient() ?: throw IllegalStateException("Not Connected")
        val sftp = ssh.newSFTPClient()

        // Simpan inputstream ke file temporary
        val tempFile = File.createTempFile("upload_", ".tmp", context.cacheDir)
        tempFile.outputStream().use { output ->
            input.copyTo(output)
        }

        try {
            sftp.put(tempFile.absolutePath, remotePath)
        } finally {
            tempFile.delete()
            sftp.close()
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
