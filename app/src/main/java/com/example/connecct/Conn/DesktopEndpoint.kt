package com.example.connecct.Conn

import org.json.JSONObject

data class DesktopEndpoint(
    val ip : String,
    val username: String,
    val devicename: String,
    val sshPort: Int,
    val httpPort: Int,
    val pairingToken: String?
) {
    companion object{
        fun fromQRPayload(raw: String): DesktopEndpoint?{
            return try{
                val json = JSONObject(raw)

                if(json.optString("type") != "desktop") return null

                DesktopEndpoint(
                    ip = json.optString("ip"),
                    username = json.optString("username", null),
                    devicename = json.optString("deviceName", null),
                    sshPort = json.optInt("sshPort"),
                    httpPort = json.optInt("httpPort"),
                    pairingToken = json.optString("pairingToken", null)
                )
            }catch(e: Exception){
                null
            }
        }
    }
}