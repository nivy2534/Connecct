package com.example.connecct.ui.viewmodel

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.connecct.storage.loadStorageKey
import kotlinx.coroutines.launch

class KeysViewModel : ViewModel() {
    private val _keys = mutableStateListOf<SSHKeys>()
    val keys: List<SSHKeys> get() = _keys

    fun addKey(name: String, type: String, privateFile: String, publicFile: String) {
        val newKey = SSHKeys(
            name = name,
            type = type,
            privateFile = privateFile,
            publicFile = publicFile,
            addedAt = "Today"
        )
        _keys.add(newKey)
    }

    fun deleteKey(key: SSHKeys) {
        _keys.remove(key)
    }

    fun loadStoredKeys(context: Context){
        viewModelScope.launch {
            val loader = loadStorageKey(context)
            val loadedKeys = loader.loadKeys()
            _keys.clear()
            _keys.addAll(loadedKeys)
        }
    }
}