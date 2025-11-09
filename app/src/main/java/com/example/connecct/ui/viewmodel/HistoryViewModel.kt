package com.example.connecct.ui.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class SSHConnectionHistory(
    val username: String,
    val host: String,
    val port: Int,
    val keyName: String,
    val status: String, // success / failed / unknown
    val error: String = "",
    val timestamp: Long = System.currentTimeMillis()
) {
    fun formattedTime(): String {
        val date = java.util.Date(timestamp)
        val fmt = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
        return "Today ${fmt.format(date)}"
    }
}

class HistoryViewModel : ViewModel() {
    private val _connectionHistory = MutableStateFlow<List<SSHConnectionHistory>>(emptyList())
    val connectionHistory = _connectionHistory.asStateFlow()

    fun addConnection(connection: SSHConnectionHistory) {
        _connectionHistory.value = listOf(connection) + _connectionHistory.value
    }

    fun deleteConnection(connection: SSHConnectionHistory) {
        _connectionHistory.value = _connectionHistory.value.filterNot { it == connection }
    }

    fun clearAll() {
        _connectionHistory.value = emptyList()
    }

    fun getReadableDate(dateKey: String): String {
        val today = java.text.SimpleDateFormat("yyyy-MM-dd").format(java.util.Date())
        val yesterday = java.text.SimpleDateFormat("yyyy-MM-dd")
            .format(java.util.Date(System.currentTimeMillis() - 86400000))
        return when (dateKey) {
            today -> "Today"
            yesterday -> "Yesterday"
            else -> dateKey
        }
    }
}