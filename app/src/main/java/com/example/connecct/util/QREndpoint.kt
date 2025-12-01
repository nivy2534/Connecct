package com.example.connecct.util

data class QrEndpoint(
    val ip: String,
    val username: String,
    val httpPort: Int,
    val secret: String,
)