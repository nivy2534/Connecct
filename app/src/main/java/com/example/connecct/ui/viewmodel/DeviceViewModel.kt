package com.example.connecct.ui.viewmodel

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.connecct.storage.DeviceRepository
import com.example.connecct.util.QrEndpoint
import kotlinx.coroutines.launch

class DeviceViewModel(
    private val repo: DeviceRepository
) : ViewModel() {

    private val _devices = mutableStateListOf<Device>()
    val devices: List<Device> get() = _devices

    init {
        viewModelScope.launch {
            repo.devices.collect { list ->
                android.util.Log.d("DEVICE_VM", "Repo devices updated: size=${list.size}")
                _devices.clear()
                _devices.addAll(list)
            }
        }
    }

    fun removeDevice(device: Device) {
        android.util.Log.d("DEVICE_VM", "Request remove device: ${device.id}")
        _devices.removeAll{it.id == device.id}

        viewModelScope.launch {
            repo.removeDevice(device.id)
        }
    }

    fun addOrUpdateFromQr(endpoint: QrEndpoint, connected: Boolean) {
        android.util.Log.d(
            "DEVICE_VM",
            "addOrUpdateFromQr called for ${endpoint.username}@${endpoint.ip}, connected=$connected"
        )
        repo.addOrUpdateFromQr(endpoint, connected)
    }

    fun addOrUpdateManual(host: String, username: String, connected: Boolean) {
        android.util.Log.d(
            "DEVICE_VM",
            "addOrUpdateManual called for $username@$host, connected=$connected"
        )
        repo.addOrUpdateManual(host, username, connected)
    }

    fun setConnected(id: String, connected: Boolean){
        android.util.Log.d("DEVICE_VM", "setConnected called for $id, connected=$connected")
        val idx = _devices.indexOfFirst { it.id == id }
        if(idx != -1){
            val old = _devices[idx]
            _devices[idx] = old.copy(isConnected = connected)
        }

        viewModelScope.launch{
            android.util.Log.d(
                "DEVICE_VM",
                "setConnected called for $id, connected=$connected"
            )

            val idx = _devices.indexOfFirst { it.id == id }
            if(idx != -1){
                val old = _devices[idx]
                _devices[idx] = old.copy(isConnected = connected)
            }

            viewModelScope.launch {
                repo.setConnected(id, connected)
            }
        }
    }
}
