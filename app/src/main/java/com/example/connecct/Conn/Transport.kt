package com.example.connecct.Conn

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.example.connecct.ui.state.RemoteFile
import net.schmizz.sshj.common.StreamCopier
import net.schmizz.sshj.sftp.OpenMode
import net.schmizz.sshj.sftp.SFTPClient
import net.schmizz.sshj.xfer.TransferListener
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.io.OutputStream

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
    // INTERNAL DOWNLOAD ENGINE (CORE)
    // ----------------------------------------------------------------------
    private fun downloadInternal(
        remotePath: String,
        outputStream: OutputStream,
        onBytesDownloaded: ((downloaded: Long, total: Long) -> Unit)? = null
    ) {
        val ssh = connection.getClient() ?: throw IllegalStateException("Not Connected")

        ssh.newSFTPClient().use { sftp ->
            val totalSize = sftp.stat(remotePath).size ?: 0L

            sftp.open(remotePath).use { remoteFile ->
                val buffer = ByteArray(32 * 1024)
                var offset = 0L
                var downloaded = 0L

                while (true) {
                    val read = remoteFile.read(offset, buffer, 0, buffer.size)
                    if (read <= 0) break

                    outputStream.write(buffer, 0, read)
                    offset += read
                    downloaded += read

                    onBytesDownloaded?.invoke(downloaded, totalSize)
                }

                outputStream.flush()
            }
        }
    }

    // ----------------------------------------------------------------------
    // DOWNLOAD TO FILE
    // ----------------------------------------------------------------------
    fun downloadFile(
        remotePath: String,
        localFile: File,
        onProgress: ((Long) -> Unit)? = null
    ) {
        localFile.outputStream().use { output ->
            downloadInternal(remotePath, output) { downloaded, total ->
                if (total > 0) {
                    val percent = (downloaded * 100) / total
                    onProgress?.invoke(percent)
                }
            }
        }
    }

    // ----------------------------------------------------------------------
    // DOWNLOAD TO STREAM
    // ----------------------------------------------------------------------
    fun downloadFileToStream(
        remotePath: String,
        outputStream: OutputStream,
        onProgress: ((Long, Long) -> Unit)? = null
    ) {
        val ssh = connection.getClient()
            ?: throw IllegalStateException("Not Connected")

        val sftp = ssh.newSFTPClient()
        val remoteFile = sftp.open(remotePath)

        try {
            val totalSize = sftp.stat(remotePath).size ?: 0L
            val buffer = ByteArray(32 * 1024)

            var offset = 0L
            var downloaded = 0L

            while (true) {
                val read = remoteFile.read(offset, buffer, 0, buffer.size)
                if (read <= 0) break

                outputStream.write(buffer, 0, read)
                offset += read
                downloaded += read

                onProgress?.invoke(downloaded, totalSize)
            }

            outputStream.flush()

        } finally {
            remoteFile.close()
            sftp.close()
        }
    }

    fun createRemoteDirectory(parentPath: String, folderName: String) {
        val ssh = connection.getClient()

        if (ssh == null || !ssh.isConnected) {
            Log.e("CREATE_FOLDER", "SSH not connected, abort create folder")
            return
        }

        val sftp = ssh.newSFTPClient()

        try {
            val fullPath = parentPath.trimEnd('/') + "/" + folderName
            sftp.mkdir(fullPath)
        } catch (e: Exception) {
            Log.e("CREATE_FOLDER", "Failed to create folder", e)
        } finally {
            sftp.close()
        }
    }

    // ----------------------------------------------------------------------
    // READ FILE AS STRING
    // ----------------------------------------------------------------------
    fun readFile(path: String, context: Context): String {
        val tempFile = File.createTempFile("ssh_read_", ".tmp", context.cacheDir)
        downloadFile(path, tempFile)
        return tempFile.readText(Charsets.UTF_8)
    }

    // ----------------------------------------------------------------------
    // READ FILE AS BYTES
    // ----------------------------------------------------------------------
    fun readFileBytes(remotePath: String): ByteArray {
        val output = ByteArrayOutputStream()
        downloadInternal(remotePath, output)
        return output.toByteArray()
    }

    // ----------------------------------------------------------------------
    // SFTP UPLOAD WITH PROGRESS (BYTES)
    // ----------------------------------------------------------------------
    fun sftpPutWithProgress(
        inputStream: InputStream,
        remotePath: String,
        onBytesSent: (sent: Long, total: Long) -> Unit
    ) {
        val ssh = connection.getClient() ?: throw IllegalStateException("Not Connected")

        ssh.newSFTPClient().use { sftp ->
            sftp.open(
                remotePath,
                setOf(OpenMode.WRITE, OpenMode.CREAT, OpenMode.TRUNC)
            ).use { remoteFile ->

                val buffer = ByteArray(16 * 1024)
                var totalSent = 0L
                val totalSize = try {
                    inputStream.available().toLong()
                } catch (_: Exception) {
                    -1L
                }

                while (true) {
                    val read = inputStream.read(buffer)
                    if (read <= 0) break

                    remoteFile.write(totalSent, buffer, 0, read)
                    totalSent += read

                    onBytesSent(totalSent, totalSize)
                }
            }
        }
    }

    // ----------------------------------------------------------------------
    // SCP UPLOAD
    // ----------------------------------------------------------------------
    fun uploadFile(localPath: String, remotePath: String, onProgress: (Float) -> Unit = {}) {
        val ssh = connection.getClient() ?: throw IllegalStateException("Not Connected")
        val scp = ssh.newSCPFileTransfer()
        val file = File(localPath)

        if (!file.exists()) throw IllegalArgumentException("File not found: $localPath")

        onProgress(0f)

        scp.setTransferListener(object : TransferListener {
            override fun directory(name: String?) = this
            override fun file(name: String?, size: Long) =
                StreamCopier.Listener { transferred ->
                    if (size > 0) {
                        onProgress((transferred.toFloat() / size).coerceIn(0f, 1f))
                    }
                }
        })

        scp.upload(localPath, remotePath)
        onProgress(1f)
    }

    // ----------------------------------------------------------------------
    // SCP DOWNLOAD
    // ----------------------------------------------------------------------
    fun downloadFile(remotePath: String, localPath: String, onProgress: (Float) -> Unit = {}) {
        val ssh = connection.getClient() ?: throw IllegalStateException("Not Connected")
        val scp = ssh.newSCPFileTransfer()

        onProgress(0f)

        scp.setTransferListener(object : TransferListener {
            override fun directory(name: String?) = this
            override fun file(name: String?, size: Long) =
                StreamCopier.Listener { transferred ->
                    if (size > 0) {
                        onProgress((transferred.toFloat() / size).coerceIn(0f, 1f))
                    }
                }
        })

        scp.download(remotePath, localPath)
        onProgress(1f)
    }

    // ----------------------------------------------------------------------
    // DIRECTORY LIST
    // ----------------------------------------------------------------------
    fun listDirectory(path: String): List<RemoteFile> {
        val ssh = connection.getClient() ?: throw IllegalStateException("Not Connected")

        ssh.newSFTPClient().use { sftp ->
            return sftp.ls(path)
                .filter { it.name != "." && it.name != ".." }
                .map {
                    RemoteFile(
                        name = it.name,
                        size = it.attributes.size ?: 0L,
                        isDirectory = it.attributes.type ==
                                net.schmizz.sshj.sftp.FileMode.Type.DIRECTORY
                    )
                }
        }
    }

    // ----------------------------------------------------------------------
    // DELETE & MOVE
    // ----------------------------------------------------------------------
    fun deleteRemoteFile(path: String) {
        val ssh = connection.getClient() ?: return
        ssh.newSFTPClient().use { it.rm(path) }
    }

    fun moveRemoteFile(oldPath: String, newPath: String) {
        val ssh = connection.getClient() ?: return
        ssh.newSFTPClient().use { it.rename(oldPath, newPath) }
    }

    // ----------------------------------------------------------------------
    // HOME DIRECTORY
    // ----------------------------------------------------------------------
    fun getHomeDirectory(): String {
        val ssh = connection.getClient() ?: throw IllegalStateException("Not Connected")
        ssh.newSFTPClient().use { return it.canonicalize(".") }
    }
}