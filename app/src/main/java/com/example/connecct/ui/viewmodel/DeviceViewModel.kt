package com.example.connecct.ui.viewmodel

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel

class DeviceViewModel : ViewModel() {

    private val _devices = mutableStateListOf<Device>()
    val devices: List<Device> get() = _devices

    init {
        // Dummy contoh
        _devices.addAll(
            listOf(
                Device("Laptop Work", "192.168.0.12", "Online", "1 menit lalu"),
                Device("Server VPS", "45.112.89.12", "Offline", "2 jam lalu")
            )
        )
    }

    fun removeDevice(device: Device) {
        _devices.remove(device)
    }
}