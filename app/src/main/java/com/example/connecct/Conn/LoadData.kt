package com.example.connecct.Conn

class LoadData(private val transport: Transport){
    fun getSystemInfo(): Map<String, String>{
        val uname = transport.executeCommand("uname -a").trim()
        val uptime = transport.executeCommand("uptime -p").trim()
        return mapOf(
            "system" to uname,
            "uptime" to uptime
        )
    }
}