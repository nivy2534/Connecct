package com.example.connecct.Conn

import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Semaphore
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.Socket
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.math.min

data class DiscoveredHost(
    val ip: String,
    val isSshOpen: Boolean,
    val isTailscaleCandidate: Boolean
)

class IPdiscover (
    private val portToCheck: Int=22,
    private val connTimeouts: Int=400,
    private val paralellism: Int=100
){
    suspend fun discoveredNetworkHosts(): List<DiscoveredHost> = withContext(Dispatchers.IO){
        val prefixes = collectRelevantPrefixes()
        val results = ConcurrentLinkedQueue<DiscoveredHost>()
        val scope = CoroutineScope(Dispatchers.IO)

        val semaphore = Semaphore(paralellism)

        val jobs = prefixes.flatMap{ prefix ->
            (1..254).map {i ->
                scope.async{
                    semaphore.acquire()
                    try{
                        val ip = "$prefix.$i"
                        val isOpen = isPortOpen(ip, portToCheck, connTimeouts)
                        val isTailscaleCandidate = ip.startsWith("100.")
                        if (isOpen){
                            results.add(DiscoveredHost(ip, true, isTailscaleCandidate))
                        }
                    }finally {
                        semaphore.release()
                    }
                }
            }
        }
        return@withContext results.toList()
    }

    private fun collectRelevantPrefixes(): List<String>{
        val prefixes = mutableSetOf<String>()
        try{
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while(interfaces.hasMoreElements()){
                val ni = interfaces.nextElement()
                if(!ni.isUp || ni.isLoopback) continue

                val addrs = ni.inetAddresses
                while (addrs.hasMoreElements()){
                    val addr = addrs.nextElement()
                    val host = addr.hostAddress?: continue

                    if(host.contains(":")) continue

                    val parts = host.split(".")
                    if (parts.size == 4){
                        val prefix = "${parts[0]}.${parts[1]}.${parts[2]}"
                        prefixes.add(prefix)
                    }
                }
            }
        }catch (e: Exception){

        }

        if(prefixes.isEmpty()){
            prefixes.addAll(listOf("192.168.0", "192.168.1", "10.0.0"))
        }
        return prefixes.toList()
    }

    private fun isPortOpen(host: String, port: Int, timeout: Int): Boolean{
        return try{
            Socket().use{ socket ->
                socket.connect(java.net.InetSocketAddress(host, port), timeout)
                true
            }
        }catch (_: Exception) {
            false
        }
    }

    private class Semaphore(private val permits: Int){
        private val channel = kotlinx.coroutines.sync.Semaphore(permits)
        suspend fun acquire() = channel.acquire()
        fun release() = channel.release()
    }
}