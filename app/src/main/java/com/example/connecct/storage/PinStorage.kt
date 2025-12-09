package com.example.connecct.storage

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import android.util.Log

class PinStorage(context: Context) {

    private val masterKey = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)

    private val shared = EncryptedSharedPreferences.create(
        "secure_prefs",
        masterKey,
        context,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun savePin(pin: String) {
        shared.edit().putString("pin", pin).apply()
    }

    fun getPin(): String? {
        val exists = shared.contains("pin")
        Log.d("PIN_STORAGE", "getPin() called, exists=$exists")
        val pin = shared.getString("pin", null)
        Log.d("PIN_STORAGE", "getPin() result length=${pin?.length ?: 0}")
        return pin
    }

    fun deletePin(){
        val exists = shared.contains("pin")
        Log.d("PIN_STORAGE", "deletePin() called, exists=$exists")
        val pin = shared.getString("pin", null)
        Log.d("PIN_STORAGE", "deletePin() result length=${pin?.length ?: 0}")
        shared.edit().remove(pin).apply()
    }
}