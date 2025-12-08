package com.example.connecct.Conn

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import com.example.connecct.util.DesktopInfo
import com.example.connecct.util.QrEndpoint
import java.io.DataOutputStream
import android.util.Log

object HttpProbing {
    private const val CONNECT_TIMEOUT = 3000
    private const val READ_TIMEOUT = 3000
    private const val PAIR_HEADER = "X-Pair-Token"

    suspend fun probeInfo(endpoint: DesktopEndpoint): DesktopInfo? =
        withContext(Dispatchers.IO) {
            val url = URL("http://${endpoint.ip}:${endpoint.httpPort}/info")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = CONNECT_TIMEOUT
                readTimeout = READ_TIMEOUT
                requestMethod = "GET"
                endpoint.pairingToken?.let { setRequestProperty(PAIR_HEADER, it) }
            }

            return@withContext try {
                val code = conn.responseCode
                if (code in 200..299) {
                    val body = conn.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(body)
                    DesktopInfo(
                        os = json.optString("os", null),
                        user = json.optString("user", endpoint.username),
                        host = endpoint.ip,
                        deviceName = json.optString("deviceName", endpoint.devicename)
                    )
                } else {
                    null
                }
            } catch (_: Exception) {
                null
            } finally {
                conn.disconnect()
            }
        }

    suspend fun sendPublicKey(
        endpoint: QrEndpoint,
        publicKey: String,
        comment: String? = null,
        secret: String
    ):Boolean = withContext(Dispatchers.IO){
        val url = URL("http://${endpoint.ip}:${endpoint.httpPort}/keys/import")
        Log.d("HTTP_REQUEST", "Sending public key to ${endpoint.ip}:${endpoint.httpPort}")
        val conn = (url.openConnection() as HttpURLConnection).apply{
            connectTimeout = CONNECT_TIMEOUT
            readTimeout = READ_TIMEOUT
            requestMethod = "POST"
            doOutput = true
            setRequestProperty("Content-Type","application/json")
            setRequestProperty(PAIR_HEADER, secret)
        }
        Log.d("HTTP_REQUEST", "sending secret: $secret")
        return@withContext try{
            val json = JSONObject().apply {
                put("public_key", publicKey)
                comment?.let { put("comment", it) }
            }

            DataOutputStream(conn.outputStream).use{out ->
                out.write(json.toString().toByteArray(Charsets.UTF_8))
                out.flush()
            }

            val code = conn.responseCode
            Log.d("HTTP_RESPONSE", "Response code: $code $conn.responseMessage")
            code in 200..299
        }catch(e: Exception){
            Log.e("HTTP_ERROR", "Error sending public key", e)
            false
        }finally {
            conn.disconnect()
        }
    }
}