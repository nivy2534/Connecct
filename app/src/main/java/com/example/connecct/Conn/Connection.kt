package com.example.connecct.Conn


import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.transport.verification.PromiscuousVerifier

class Connection{
    private var ssh: SSHClient? = null

    suspend fun connect(host: String, username: String, privateKeyPath: String){
        disconnect()

        val newSsh = SSHClient()
        newSsh.addHostKeyVerifier(PromiscuousVerifier())
        newSsh.connect(host)

        try{
            val keyProvider = newSsh.loadKeys(privateKeyPath)
            newSsh.authPublickey(username, keyProvider)

            ssh = newSsh
        }catch(e: Exception){
            newSsh.disconnect()
            throw e
        }
    }

    fun isConnected():Boolean{
        return ssh?.isAuthenticated == true
    }

    fun disconnect(){
        ssh?.disconnect()
        ssh = null
    }

    internal fun getClient() : SSHClient? = ssh
}