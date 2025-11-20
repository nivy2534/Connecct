package com.example.connecct.Conn

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

}