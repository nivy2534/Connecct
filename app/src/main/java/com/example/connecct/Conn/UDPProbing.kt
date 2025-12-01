package com.example.connecct.Conn

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetSocketAddress
import java.net.SocketTimeoutException
import android.util.Log
import com.example.connecct.util.udpResult

class UDPProbing(
    private val udpPort: Int = 33220
) {
    suspend fun udpPing(ip: String, secret: String, timeouts: Int = 5000): udpResult = withContext(Dispatchers.IO){
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
                val status = msg.substringBefore(":", missingDelimiterValue = msg)
                val secret = msg.substringAfter(":", missingDelimiterValue = "")

                if (status == "OK"){
                    udpResult(true, secret)
                }else{
                    udpResult(false, "")
                }
            }catch (e: Exception){
                udpResult(false, "")
            }
        }
    }
}