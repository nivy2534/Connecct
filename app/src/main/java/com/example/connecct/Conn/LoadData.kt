package com.example.connecct.Conn

import com.example.connecct.util.RemoteOS
import net.schmizz.sshj.connection.channel.Window

class LoadData(private val transport: Transport){

    private var cachedOS: RemoteOS? = null

    fun getSystemInfo(): Map<String, String>{
        val os = detectRemoteOS()

        return when (os){
            RemoteOS.WINDOWS -> {
                val ver = transport.executeCommand("cmd.exe /c ver").trim()
                mapOf(
                    "system" to ver,
                    "uptime" to "N/A (Windows belum di-handle)"
                )
            }

            else -> {
                val uname = transport.executeCommand("uname -a").trim()
                val uptime = transport.executeCommand("uptime -p").trim()
                mapOf(
                    "system" to uname,
                    "uptime" to uptime
                )
            }
        }


    }

    fun listDirectory(path: String): List<String>{
        val safePath = if (path.isBlank()) "." else path
        val os = detectRemoteOS()

        val command = when(os){
            RemoteOS.WINDOWS ->
                """cmd.exe /c dir /b"$safePath""""
            else ->
                """ls -1A "$safePath""""
        }

        val output = transport.executeCommand(command)

        return output
            .lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toList()
    }

    fun detectRemoteOS(): RemoteOS{
        cachedOS?.let{return it}

        val uname = try{
            transport.executeCommand("uname -s").trim()
        }catch(e: Exception){
            ""
        }

        val detected = when{
            uname.contains("Linux", ignoreCase = true) -> RemoteOS.LINUX
            uname.contains("Darwin", ignoreCase = true) -> RemoteOS.MAC
            uname.isNotEmpty() -> RemoteOS.LINUX
            else -> {
                val ver = try{
                    transport.executeCommand("cat /etc/issue").trim()
                }catch (e: Exception){
                    ""
                }

                when{
                    ver.contains("Windows", ignoreCase = true) -> RemoteOS.WINDOWS
                    else -> RemoteOS.UNKNOWN
                }
            }
        }
        cachedOS = detected
        return detected
    }
}