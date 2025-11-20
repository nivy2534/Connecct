package com.example.connecct.ui.viewmodel

data class SSHKeys(
    val name: String,
    //val type: String,
    val privateFile: String,
    val publicFile: String,
    //val addedAt: String,
    //val privateKeyContent: String,
    val publicKeyContent: String
)