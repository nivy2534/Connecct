package com.example.connecct.viewmodel

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.example.connecct.storage.PinStorage
import android.util.Log

class PinViewModel(private val storage: PinStorage) : ViewModel() {

    var pinInput = mutableStateOf("")
    var authenticated = mutableStateOf(false)

    // Apakah PIN sudah ada sebelumnya
    var pinExists = storage.getPin() != null

    init {
        Log.d("PIN_VM", "PinViewModel created, pinExists=$pinExists")
    }

    // Menambah digit PIN
    fun addDigit(digit: String) {
        if (pinInput.value.length < 4) {
            pinInput.value += digit
            Log.d("PIN_VM", "addDigit('$digit') -> now='${pinInput.value}'")
        } else {
            Log.d("PIN_VM", "addDigit('$digit') ignored, already 4 digits")
        }
    }

    // Hapus digit terakhir
    fun deleteDigit() {
        if (pinInput.value.isNotEmpty()) {
            pinInput.value = pinInput.value.dropLast(1)
            Log.d("PIN_VM", "deleteDigit() -> now='${pinInput.value}'")
        } else {
            Log.d("PIN_VM", "deleteDigit() ignored, input empty")
        }
    }

    // Submit PIN
    fun submitPin() {
        val input = pinInput.value
        val savedPin = storage.getPin()
        Log.d(
            "PIN_VM",
            "submitPin() called, input='$input', savedExists=${savedPin != null}"
        )

        if (savedPin == null) {
            Log.d("PIN_VM", "No saved PIN, saving new one and authenticating")
            storage.savePin(input)
            authenticated.value = true
        } else if (input == savedPin) {
            Log.d("PIN_VM", "PIN match -> authenticated")
            authenticated.value = true
        } else {
            Log.d("PIN_VM", "PIN mismatch -> NOT authenticated")
        }

        pinInput.value = ""
        Log.d("PIN_VM", "submitPin() done, authenticated=${authenticated.value}")
    }

    // Jika fingerprint sukses
    fun authenticateWithBiometrics() {
        Log.d("PIN_VM", "authenticateWithBiometrics() -> authenticated=true")
        authenticated.value = true
    }
}