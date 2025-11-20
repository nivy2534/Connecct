package com.example.connecct.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.connecct.Conn.Connection
import com.example.connecct.ui.state.ConnectionStatus
import com.example.connecct.ui.state.UiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.security.Security
import com.example.connecct.ui.state.ConnectionUiEvent

class ConnectionViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val connection = Connection()

    fun onEvent(event: ConnectionUiEvent) {
        when (event) {
            is ConnectionUiEvent.OnHostChanged -> updateHost(event.host)
            is ConnectionUiEvent.OnUsernameChanged -> updateUsername(event.username)
            is ConnectionUiEvent.OnPassphraseChanged -> updatePassphrase(event.passphrase)
            is ConnectionUiEvent.OnPrivateKeySelected -> updatePrivateKey(event.path, event.filename, event.filesize)
            ConnectionUiEvent.OnResetClicked -> resetState()
            else -> {}
        }
    }

    private fun updateHost(host: String) {
        _uiState.update { it.copy(host = host) }
    }

    private fun updateUsername(username: String) {
        _uiState.update { it.copy(username = username) }
    }

    private fun updatePassphrase(passphrase: String) {
        _uiState.update { it.copy(passphrase = passphrase) }
    }

    private fun updatePrivateKey(path: String, filename: String, filesize: String) {
        _uiState.update {
            it.copy(
                privateKeyPath = path,
                filename = filename,
                filesize = filesize,
                isKeySelected = true
            )
        }
    }

    fun connectToServer(context: Context) {
        viewModelScope.launch {
            val currentState = _uiState.value

            if (currentState.host.isEmpty() || currentState.username.isEmpty() || currentState.privateKeyPath.isEmpty()) {
                _uiState.update {
                    it.copy(
                        errorMessage = "Host, Username, dan Private Key harus diisi",
                        connectionStatus = ConnectionStatus.FAILED
                    )
                }
                return@launch
            }

            _uiState.update {
                it.copy(
                    isConnecting = true,
                    connectionStatus = ConnectionStatus.CONNECTING,
                    status = "Connecting...",
                    errorMessage = ""
                )
            }

            try {
                if (Security.getProvider("BC") == null) {
                    Security.addProvider(BouncyCastleProvider())
                }

                withContext(Dispatchers.IO) {
                    connection.connect(
                        context = context, // ✅ kirim dari UI
                        host = currentState.host,
                        username = currentState.username,
                        privateKeyPath = currentState.privateKeyPath
                    )
                }

                _uiState.update {
                    it.copy(
                        isConnecting = false,
                        connectionStatus = ConnectionStatus.CONNECTED,
                        status = "Connected successfully!",
                        errorMessage = ""
                    )
                }

            } catch (e: Exception) {
                handleConnectionError(
                    "Failed to connect: ${e::class.simpleName}",
                    ConnectionStatus.FAILED,
                    e
                )
            }
        }
    }

    fun handleQrConnection(context: Context, qrData: String) {
        try {
            // Format QR misalnya: ssh://username:pass@host:22?key=id_rsa
            // Atau format custom seperti: host|username|passphrase
            val parts = qrData.split("|")

            if (parts.size < 3) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Format QR tidak valid"
                )
                return
            }

            val host = parts[0]
            val username = parts[1]
            val passphrase = parts[2]

            // memperbarui uiState
            _uiState.value = _uiState.value.copy(
                host = host,
                username = username,
                passphrase = passphrase
            )

            // otomatis konek
            connectToServer(context)

        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "Gagal membaca QR: ${e.message}"
            )
        }
    }

    private fun handleConnectionError(message: String, status: ConnectionStatus, exception: Exception) {
        _uiState.update {
            it.copy(
                isConnecting = false,
                connectionStatus = status,
                status = "Error: $message",
                errorMessage = exception.message ?: message
            )
        }
    }

    private fun resetState() {
        _uiState.update { UiState() }
    }
}
