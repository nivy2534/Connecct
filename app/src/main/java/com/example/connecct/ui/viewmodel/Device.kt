package com.example.connecct.ui.viewmodel

data class Device(
    val name: String,
    val ip: String,
    val status: String, // Online / Offline
    val lastSeen: String
)