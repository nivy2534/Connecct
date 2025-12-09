package com.example.connecct.storage

import android.util.Log
import com.example.connecct.ui.viewmodel.Device
import com.example.connecct.util.QrEndpoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class DeviceRepository(
    private val storageDevice: LoadStoredDevice
) {
    private val _devices = MutableStateFlow<List<Device>>(emptyList())
    val devices: StateFlow<List<Device>> get() = _devices

    init {
        val loaded = storageDevice.loadDevices()
        Log.d("DEVICE_REPO", "Loaded ${loaded.size} devices from storage")
        _devices.value = loaded
    }

    private fun addOrUpdateDeviceInternal(
        id: String,
        host: String,
        user: String,
        deviceName: String,
        connected: Boolean,
    ){
        val now = formatTime()

        val current = _devices.value.toMutableList()
        val idx = current.indexOfFirst { it.id == id }

        val device = Device(
            id = id,
            host = host,
            user = user,
            deviceName = deviceName,
            os = null,
            isOnline = true,
            lastSeen = now,
            isConnected = connected
        )

        if(idx >= 0){
            Log.d("DEVICE_REPO", "Updating existing device $id")
            current[idx] = device
        }else{
            Log.d("DEVICE_REPO", "Adding new device $id")
            current.add(device)
        }

        _devices.value = current
        storageDevice.saveDevices(current)
        Log.d("DEVICE_REPO", "Saved ${current.size} devices to storage")
    }
    fun addOrUpdateFromQr(
        endpoint: QrEndpoint,
        connected: Boolean
    ) {
        val id = "${endpoint.username}@${endpoint.ip}"
        addOrUpdateDeviceInternal(
            id = id,
            host = endpoint.ip,
            user = endpoint.username,
            deviceName = endpoint.username,
            connected = connected
        )
    }

    fun addOrUpdateManual(
        host: String,
        username: String,
        connected: Boolean
    ){
        val id = "${username}@$host"
        addOrUpdateDeviceInternal(
            id = id,
            host = host,
            user = username,
            deviceName = username,
            connected = connected
        )
    }

    fun removeDevice(id: String) {
        Log.d("DEVICE_REPO", "Removing device $id")
        val updated = _devices.value.filterNot { it.id == id }
        _devices.value = updated
        storageDevice.saveDevices(updated)
        Log.d("DEVICE_REPO", "Saved ${updated.size} devices after removal")
    }

    private fun formatTime(): String {
        val formatter = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
        return formatter.format(java.util.Date())
    }

    fun setConnected(id: String, connected: Boolean){
        val current = _devices.value.toMutableList()
        val idx = current.indexOfFirst { it.id == id }

        if(idx < 0){
            Log.e("DEVICE_REPO", "Device with id $id not found")
            return
        }

        val old = current[idx]
        val updated = old.copy(
            isConnected = connected,
            lastSeen = formatTime(),
            isOnline = connected
        )

        current[idx] = updated
        _devices.value = current
        storageDevice.saveDevices(current)

        Log.d("DEVICE_REPO", "Saved ${current.size} devices after setConnected")
    }
}
