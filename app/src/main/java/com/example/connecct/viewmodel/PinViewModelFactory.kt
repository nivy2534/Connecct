// com/example/connecct/viewmodel/PinViewModelFactory.kt
package com.example.connecct.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.connecct.storage.PinStorage

class PinViewModelFactory(
    private val appContext: Context
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PinViewModel::class.java)) {
            val storage = PinStorage(appContext)
            return PinViewModel(storage) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
