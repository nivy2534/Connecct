package com.example.connecct.storage

import android.content.Context
import com.example.connecct.ui.viewmodel.Device
import org.json.JSONArray
import org.json.JSONObject

class LoadStoredDevice(private val context:Context) {
    private val prefs = context.getSharedPreferences("saved_devices", Context.MODE_PRIVATE)
    private val KEY_DEVICES =  "devices_json"

    fun loadDevices(): List<Device> {
        val json = prefs.getString(KEY_DEVICES, null) ?: return emptyList()
        val arr = JSONArray(json)
        val result = mutableListOf<Device>()

        for (i in 0 until arr.length()){
            val obj = arr.getJSONObject(i)
            result.add(
                Device(
                    id = obj.getString("id"),
                    host = obj.getString("host"),
                    user = obj.getString("user"),
                    deviceName = obj.getString("deviceName"),
                    os = if(obj.has("os") && !obj.isNull("os")) obj.getString("os") else null,
                    isOnline = obj.getBoolean("isOnline"),
                    lastSeen = obj.getString("lastSeen"),
                    isConnected = obj.getBoolean("isConnected")
                )
            )
        }

        return result
    }

    fun saveDevices(device: List<Device>){
        val arr = JSONArray()

        device.forEach { device ->
            val obj = JSONObject().apply {
                put("id", device.id)
                put("host", device.host)
                put("user", device.user)
                put("deviceName", device.deviceName)
                put("os", device.os)
                put("isOnline", device.isOnline)
                put("lastSeen", device.lastSeen)
                put("isConnected", device.isConnected)
            }
            arr.put(obj)
        }
        prefs.edit().putString(KEY_DEVICES, arr.toString()).apply()
    }
}