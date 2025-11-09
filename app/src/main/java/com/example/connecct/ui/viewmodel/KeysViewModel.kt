package com.example.connecct.ui.viewmodel

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel

data class SSHKey(
    val name: String,
    val type: String,
    val addedAt: String
)

class KeysViewModel : ViewModel() {
    private val _keys = mutableStateListOf<SSHKey>()
    val keys: List<SSHKey> get() = _keys

    fun addKey(name: String, type: String) {
        val newKey = SSHKey(
            name = name,
            type = type,
            addedAt = "Today"
        )
        _keys.add(newKey)
    }

    fun deleteKey(key: SSHKey) {
        _keys.remove(key)
    }
}