package com.example.connecct.ui.viewmodel

data class Device(
    val id: String,           // unik, misal "username@ip"
    val host: String,         // IP / hostname
    val user: String,         // username SSH
    val deviceName: String,   // nama buat tampil di UI ("Laptop Work", "Server VPS", dll)
    val os: String? = null,   // opsional, kalau nanti bisa detect OS
    val isOnline: Boolean,    // status online sekarang
    val lastSeen: String,  // timestamp millis, nanti bisa di-format jadi "1 menit lalu"
    val isConnected: Boolean  // apakah saat ini session aktif
)
