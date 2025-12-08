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
        repo.removeDevice(device.id)
    }

    fun addOrUpdateFromQr(endpoint: QrEndpoint, connected: Boolean) {
        android.util.Log.d(
            "DEVICE_VM",
            "addOrUpdateFromQr called for ${endpoint.username}@${endpoint.ip}, connected=$connected"
        )
        // kalau mau central, mending panggil repo.addOrUpdateFromQr di sini juga:
        repo.addOrUpdateFromQr(endpoint, connected)
    }
}
