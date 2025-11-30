package com.example.connecct.Conn

import android.content.Context
import com.example.connecct.ui.state.RemoteFile
import net.schmizz.sshj.common.StreamCopier
import net.schmizz.sshj.xfer.TransferListener
import java.io.ByteArrayOutputStream
import java.io.File


class Transport(private val connection: Connection){
    fun executeCommand(command: String): String{
        val ssh = connection.getClient() ?:throw IllegalStateException("Not Connected")
        ssh.startSession().use{ session ->
            val cmd = session.exec(command)
            val output = ByteArrayOutputStream()
            cmd.inputStream.copyTo(output)
            cmd.join()
            return String(output.toByteArray(), Charsets.UTF_8)
        }
    }

    fun uploadFile(localPath: String, remotePath: String, onProgress: (Float) -> Unit = {}) {
        val ssh = connection.getClient() ?:throw IllegalStateException("Not Connected")
        val scp = ssh.newSCPFileTransfer()

        val localFile = File(localPath)
        if (!localFile.exists()) {
            throw IllegalArgumentException("Local file does not exist: $localPath")
        }

        onProgress(0f)

        scp.setTransferListener(object: TransferListener{
            override fun directory(name: String?): TransferListener? {
                return this
            }

            override fun file(name: String?, size: Long): StreamCopier.Listener? {
                return object: StreamCopier.Listener{
                    override fun reportProgress(transferred: Long) {
                        if (size > 0){
                            val progress = (transferred.toFloat() / size.toFloat())
                            onProgress(progress.coerceIn(0f, 1f))
                        }
                    }
                }
            }
        })

        scp.upload(localPath, remotePath)

        onProgress(1f)
    }

    fun downloadFile(remotePath: String, localPath: String, onProgress: (Float) -> Unit = {}){
        val ssh = connection.getClient()?:throw IllegalStateException("Not Connected")
        val scp = ssh.newSCPFileTransfer()

        onProgress(0f)

        scp.setTransferListener(object: TransferListener{
            override fun directory(name: String?): TransferListener? {
                return this
            }

            override fun file(name: String?, size: Long): StreamCopier.Listener? {
                return object: StreamCopier.Listener{
                    override fun reportProgress(transferred: Long) {
                        if (size > 0) {
                            val progress = transferred.toFloat() / size.toFloat()
                            onProgress(progress.coerceIn(0f, 1f))
                        }
                    }
                }
            }
        })

        scp.download(remotePath, localPath)

        onProgress(1f)
    }

    fun listDirectory(path: String): List<RemoteFile> {
        val ssh = connection.getClient() ?: throw IllegalStateException("Not Connected")

        val sftp = ssh.newSFTPClient()
        val list = sftp.ls(path)
        sftp.close()

        return list
            .filter { it.name != "." && it.name != ".." }
            .map { file ->
                RemoteFile(
                    name = file.name,
                    size = file.attributes.size ?: 0L,
                    isDirectory = file.attributes.type == net.schmizz.sshj.sftp.FileMode.Type.DIRECTORY
                )
            }
    }

    fun readFile(path: String, context: Context): String {
        val tempFile = File.createTempFile("ssh_read_", ".tmp", context.cacheDir)
        downloadFile(path, tempFile.absolutePath) // pakai fungsi downloadFile yang sudah ada
        return tempFile.readText(Charsets.UTF_8)
    }

    fun readFileBytes(remotePath: String): ByteArray {
        val ssh = connection.getClient() ?: throw IllegalStateException("Not Connected")
        val sftp = ssh.newSFTPClient()
        val remoteFile = sftp.open(remotePath)

        return try {
            val buffer = ByteArray(32 * 1024) // 32 KB buffer
            val output = ByteArrayOutputStream()
            var offset = 0L

            while (true) {
                val bytesRead = remoteFile.read(offset, buffer, 0, buffer.size)
                if (bytesRead <= 0) break

                output.write(buffer, 0, bytesRead)
                offset += bytesRead
            }

            output.toByteArray()
        } finally {
            remoteFile.close()
            sftp.close()
        }
    }


    fun getHomeDirectory(): String {
        val ssh = connection.getClient() ?: throw IllegalStateException("Not Connected")
        val sftp = ssh.newSFTPClient()
        val home = sftp.canonicalize(".")
        sftp.close()
        return home
    }
}