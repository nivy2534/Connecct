package com.example.connecct.ui.state

/**
 * Data class yang merepresentasikan UI state untuk connection screen
 */
data class UiState(
    val host: String = "",
    val username: String = "",
    val privateKeyPath: String = "",
    val passphrase: String = "",
    val status: String = "Idle",
    val connectionStatus: ConnectionStatus = ConnectionStatus.IDLE,
    val isConnecting: Boolean = false,
    val filename: String = "",
    val filesize: String = "",
    val errorMessage: String = "",
    val isKeySelected: Boolean = false
)

enum class ConnectionStatus {
    IDLE,
    CONNECTING,
    CONNECTED,
    FAILED,
    TIMEOUT,
    AUTH_ERROR,
    DISCONNECTED
}

/**
 * Event yang dapat di-trigger dari UI
 */
sealed class ConnectionUiEvent {
    data class OnHostChanged(val host: String) : ConnectionUiEvent()
    data class OnUsernameChanged(val username: String) : ConnectionUiEvent()
    data class OnPassphraseChanged(val passphrase: String) : ConnectionUiEvent()
    data class OnPrivateKeySelected(val path: String, val filename: String) : ConnectionUiEvent()
    object OnConnectClicked : ConnectionUiEvent()
    object OnResetClicked : ConnectionUiEvent()

    // ✅ Tambahan baru untuk navigasi ke KeysScreen
    object OnChooseKeyClicked : ConnectionUiEvent()
}
