package com.example.connecct.viewmodel

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.example.connecct.storage.PinStorage

class PinViewModel(private val storage: PinStorage) : ViewModel() {

    var pinInput = mutableStateOf("")
    var authenticated = mutableStateOf(false)

    // Apakah PIN sudah ada sebelumnya
    var pinExists = storage.getPin() != null

    // Menambah digit PIN
    fun addDigit(digit: String) {
        if (pinInput.value.length < 4) {
            pinInput.value += digit
        }
    }

    // Hapus digit terakhir
    fun deleteDigit() {
        if (pinInput.value.isNotEmpty()) {
            pinInput.value = pinInput.value.dropLast(1)
        }
    }

    // Submit PIN
    fun submitPin() {
        val savedPin = storage.getPin()

        // Jika belum ada PIN → kita simpan PIN baru
        if (savedPin == null) {
            storage.savePin(pinInput.value)
            authenticated.value = true
        }
        // Jika PIN sudah ada → cek kecocokan
        else if (pinInput.value == savedPin) {
            authenticated.value = true
        }

        // Reset input setelah submit
        pinInput.value = ""
    }

    // Jika fingerprint sukses
    fun authenticateWithBiometrics() {
        authenticated.value = true
    }
}