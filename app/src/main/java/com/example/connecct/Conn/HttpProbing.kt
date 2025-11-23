package com.example.connecct.Conn

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import com.example.connecct.util.DesktopInfo

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
}