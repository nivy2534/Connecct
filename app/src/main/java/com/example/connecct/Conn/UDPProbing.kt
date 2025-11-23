package com.example.connecct.Conn

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetSocketAddress
import java.net.SocketTimeoutException
import android.util.Log

class UDPProbing(
    private val udpPort: Int = 33220
) {
    suspend fun udpPing(ip: String, secret: String, timeouts: Int = 5000): Boolean = withContext(Dispatchers.IO){
        val payload = "CONNECCT_DISCOVERY:$secret".toByteArray()
        val buf = ByteArray(64)
        Log.d("UDP_PROBING", "Sending UDP packet to $ip:$udpPort with $payload")
        DatagramSocket().use { socket ->
            socket.soTimeout = timeouts
            Log.d("UDP_PROBING", "Sending UDP packet to $ip:$udpPort")
            val packet = DatagramPacket(
                payload,
                payload.size,
                InetSocketAddress(ip, udpPort)
            )
            socket.send(packet)

            val reply = DatagramPacket(buf, buf.size)
            return@withContext try{
                socket.receive(reply)
                val msg = String(reply.data, 0, reply.length)
                msg.trim() == "OK"
            }catch (e: SocketTimeoutException){
                false
            }catch (_: Exception){
                false
            }
        }
    }
}