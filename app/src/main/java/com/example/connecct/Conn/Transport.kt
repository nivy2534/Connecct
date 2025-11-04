package com.example.connecct.Conn

import java.io.ByteArrayOutputStream

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

    fun uploadFile(localPath: String, remotePath: String) {
        val ssh = connection.getClient() ?:throw IllegalStateException("Not Connected")
        ssh.newSCPFileTransfer().upload(localPath, remotePath)
    }

    fun downloadFile(remotePath: String, localPath: String){
        val ssh = connection.getClient()?:throw IllegalStateException("Not Connected")
        ssh.newSCPFileTransfer().download(remotePath, localPath)
    }

}