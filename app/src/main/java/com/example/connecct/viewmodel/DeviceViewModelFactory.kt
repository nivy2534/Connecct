// com/example/connecct/ui/viewmodel/DeviceViewModelFactory.kt
package com.example.connecct.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.connecct.storage.DeviceRepository
import com.example.connecct.storage.LoadStoredDevice

class DeviceViewModelFactory(
    private val appContext: Context
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DeviceViewModel::class.java)) {
            val loader = LoadStoredDevice(appContext)
            val repo = DeviceRepository(loader)
            return DeviceViewModel(repo) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
