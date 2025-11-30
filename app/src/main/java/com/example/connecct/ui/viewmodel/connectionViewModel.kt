package com.example.connecct.ui.viewmodel

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.connecct.Conn.Connection
import com.example.connecct.Conn.Transport
import com.example.connecct.ui.state.ConnectionStatus
import com.example.connecct.ui.state.ConnectionUiEvent
import com.example.connecct.ui.state.OpenedFile
import com.example.connecct.ui.state.UiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.io.File
import java.security.Security

class ConnectionViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val connection = Connection()
    private val transport = Transport(connection)

    fun onEvent(event: ConnectionUiEvent) {
        when (event) {
            is ConnectionUiEvent.OnHostChanged -> updateHost(event.host)
            is ConnectionUiEvent.OnUsernameChanged -> updateUsername(event.username)
            is ConnectionUiEvent.OnPassphraseChanged -> updatePassphrase(event.passphrase)
            is ConnectionUiEvent.OnPrivateKeySelected -> selectKey(event.path, event.filename)
            is ConnectionUiEvent.OpenFile -> openFile(event.fileName, event.context)

            ConnectionUiEvent.OnResetClicked -> resetState()
            ConnectionUiEvent.CloseFile -> closeFile()

            // Explorer
            ConnectionUiEvent.LoadDirectory -> loadDirectory()
            ConnectionUiEvent.NavigateUp -> navigateUp()
            is ConnectionUiEvent.NavigateTo -> navigateTo(event.directoryName)
            else -> {}
        }
    }

    fun connectToServer(context: Context) {
        viewModelScope.launch {

            val state = _uiState.value

            if (state.host.isBlank() ||
                state.username.isBlank() ||
                state.privateKeyPath.isBlank()
            ) {
                fail("Host, Username, dan Private Key harus diisi")
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

                // SSH CONNECT
                withContext(Dispatchers.IO) {
                    connection.connect(
                        context = context,
                        host = state.host,
                        username = state.username,
                        privateKeyPath = state.privateKeyPath,
                        passphrase = state.passphrase
                    )
                }

                _uiState.update {
                    it.copy(
                        isConnecting = false,
                        connectionStatus = ConnectionStatus.CONNECTED,
                        status = "Connected"
                    )
                }

                // 🔥 Ambil HOME directory asli dari server
                val home = withContext(Dispatchers.IO) {
                    transport.getHomeDirectory()
                }

                _uiState.update { it.copy(currentPath = home) }

                loadDirectory()

            } catch (e: Exception) {
                fail("Failed to connect: ${e.message}")
            }
        }
    }

    private fun fail(message: String) {
        _uiState.update {
            it.copy(
                isConnecting = false,
                connectionStatus = ConnectionStatus.FAILED,
                status = "Error",
                errorMessage = message
            )
        }
    }

    fun loadDirectory() {
        val path = _uiState.value.currentPath

        if (!connection.isConnected()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingDirectory = true) }

            try {
                val files = withContext(Dispatchers.IO) {
                    transport.listDirectory(path)
                }

                _uiState.update {
                    it.copy(
                        remoteFiles = files,
                        isLoadingDirectory = false
                    )
                }

            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoadingDirectory = false,
                        errorMessage = e.message ?: "Failed to load directory"
                    )
                }
            }
        }
    }

    fun navigateTo(dir: String) {
        if (dir.isBlank()) return

        val newPath =
            if (_uiState.value.currentPath == "/") "/$dir"
            else _uiState.value.currentPath.trimEnd('/') + "/$dir"

        _uiState.update { it.copy(currentPath = newPath) }
        loadDirectory()
    }

    fun navigateUp() {
        val current = _uiState.value.currentPath
        if (current == "/") return

        val up = current.trimEnd('/').substringBeforeLast("/", "")

        _uiState.update {
            it.copy(
                currentPath = if (up.isEmpty()) "/" else up
            )
        }

        loadDirectory()
    }

    fun openFile(fileName: String, context: Context) {
        if (!connection.isConnected()) {
            _uiState.update { it.copy(errorMessage = "Not Connected") }
            return
        }

        viewModelScope.launch {
            try {
                val path = uiState.value.currentPath
                val remotePath =
                    if (path == "/") "/$fileName" else "$path/$fileName"

                val bytes = withContext(Dispatchers.IO) {
                    transport.readFileBytes(remotePath)
                }

                // TEXT FILES
                if (fileName.endsWith(".txt") ||
                    fileName.endsWith(".log") ||
                    fileName.endsWith(".csv") ||
                    fileName.endsWith(".json") ||
                    fileName.endsWith(".xml")
                ) {
                    _uiState.update {
                        it.copy(
                            openedFile = OpenedFile.Text(
                                name = fileName,
                                content = bytes.toString(Charsets.UTF_8)
                            )
                        )
                    }
                    return@launch
                }

                // IMAGE FILES
                if (fileName.endsWith(".jpg") ||
                    fileName.endsWith(".jpeg") ||
                    fileName.endsWith(".png")
                ) {
                    _uiState.update {
                        it.copy(
                            openedFile = OpenedFile.Image(
                                name = fileName,
                                bytes = bytes
                            )
                        )
                    }
                    return@launch
                }

                // PDF FILES
                if (fileName.endsWith(".pdf")) {
                    _uiState.update {
                        it.copy(openedFile = OpenedFile.Pdf(fileName, bytes))
                    }
                    return@launch
                }

                // VIDEO FILES
                if (fileName.endsWith(".mp4") ||
                    fileName.endsWith(".mov") ||
                    fileName.endsWith(".mkv") ||
                    fileName.endsWith(".avi")
                ) {
                    _uiState.update {
                        it.copy(
                            openedFile = OpenedFile.Video(
                                name = fileName,
                                bytes = bytes
                            )
                        )
                    }
                    return@launch
                }

                // UNSUPPORTED → fallback to external intent viewer
                val localFile = File(context.cacheDir, fileName)
                withContext(Dispatchers.IO) { localFile.writeBytes(bytes) }

                val uri = FileProvider.getUriForFile(
                    context,
                    context.packageName + ".provider",
                    localFile
                )

                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "*/*")
                    flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                }

                context.startActivity(intent)

            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Failed to open file: ${e.message}") }
            }
        }
    }

    fun closeFile() {
        _uiState.update { it.copy(openedFile = null) }
    }
    fun handleQrConnection(context: Context, qrData: String) {
        try {
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

            _uiState.value = _uiState.value.copy(
                host = host,
                username = username,
                passphrase = passphrase
            )

            connectToServer(context)

        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "Gagal membaca QR: ${e.message}"
            )
        }
    }

    // Key selection
    private fun selectKey(path: String, filename: String) {
        _uiState.update {
            it.copy(
                privateKeyPath = path,
                filename = filename,
                isKeySelected = true
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

    private fun updateHost(host: String) {
        _uiState.update { it.copy(host = host) }
    }

    private fun updateUsername(username: String) {
        _uiState.update { it.copy(username = username) }
    }

    private fun updatePassphrase(passphrase: String) {
        _uiState.update { it.copy(passphrase = passphrase) }
    }

    private fun resetState() {
        _uiState.update { UiState() }
    }
}