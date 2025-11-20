package com.example.connecct.ui.viewmodel

import android.content.Context
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.connecct.storage.LoadStorageKey
import kotlinx.coroutines.launch

class KeysViewModel : ViewModel() {

    // 🔑 Hanya 1 key saja
    private val _key = mutableStateOf<SSHKeys?>(null)
    val key: SSHKeys? get() = _key.value

    // ----------------------------------------------------------
    // LOAD KEY dari penyimpanan
    // ----------------------------------------------------------
    fun loadStoredKeys(context: Context) {
        viewModelScope.launch {
            val loader = LoadStorageKey(context)
            _key.value = loader.loadKeys().firstOrNull()
        }
    }

    // ----------------------------------------------------------
    // AUTO GENERATE (hapus yang lama & simpan baru)
    // ----------------------------------------------------------
    fun generateKeyAuto(context: Context, onDone: (() -> Unit)? = null) {
        viewModelScope.launch {
            val loader = LoadStorageKey(context)
            val newKey = loader.getOrCreateAutoKey()
            if (newKey != null) _key.value = newKey
            onDone?.invoke()
        }
    }

    // ----------------------------------------------------------
    // MANUAL GENERATE (pakai passphrase)
    // ----------------------------------------------------------
    fun generateKeyManual(context: Context, passphrase: String, onDone: (() -> Unit)? = null) {
        viewModelScope.launch {
            val loader = LoadStorageKey(context)

            val newKey = loader.generateManualKey(passphrase)

            if (newKey != null) {
                _key.value = newKey
            }

            onDone?.invoke()
        }
    }

    // ----------------------------------------------------------
    // DELETE KEY (dari UI + Storage)
    // ----------------------------------------------------------
    fun deleteKey(context: Context, onDone: (() -> Unit)? = null) {
        viewModelScope.launch {
            val loader = LoadStorageKey(context)
            loader.deleteKeys()

            _key.value = null // bersihkan UI

            onDone?.invoke()
        }
    }
}
