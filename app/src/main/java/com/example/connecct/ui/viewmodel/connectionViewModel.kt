package com.example.connecct.ui.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.connecct.Conn.Connection
import com.example.connecct.Conn.HttpProbing
import com.example.connecct.ui.state.ConnectionStatus
import com.example.connecct.ui.state.ConnectionUiEvent
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
import com.example.connecct.Conn.UDPProbing
import com.example.connecct.storage.LoadStorageKey
import org.json.JSONObject
import com.example.connecct.util.QrEndpoint
import com.example.connecct.util.udpResult

class ConnectionViewModel : ViewModel() {

    private val udpProbing = UDPProbing()
    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val connection = Connection()

    fun onEvent(event: ConnectionUiEvent) {
        when (event) {
            is ConnectionUiEvent.OnHostChanged -> updateHost(event.host)
            is ConnectionUiEvent.OnUsernameChanged -> updateUsername(event.username)
            is ConnectionUiEvent.OnPassphraseChanged -> updatePassphrase(event.passphrase)
            is ConnectionUiEvent.OnPrivateKeySelected -> selectKeyFromSettings(event.path, event.filename)
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

    // ⛔ HAPUS filesystem
    private fun updatePrivateKey(path: String, filename: String) {
        _uiState.update {
            it.copy(
                privateKeyPath = path,
                filename = filename,
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
                        context = context,
                        host = currentState.host,
                        username = currentState.username,
                        passphrase = currentState.passphrase,
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
        Log.d("QR_CONNECTION", "Raw QR data: $qrData")
        val endpoint = parseQR(qrData) ?: run{
            updatedStatus(ConnectionStatus.FAILED)
            return
        }

        viewModelScope.launch {
            val currentState = uiState.value
            updatedStatus(ConnectionStatus.CONNECTING)
            val udpOk = udpProbing.udpPing(endpoint.ip, endpoint.secret)
            Log.d("JSON_PAYLOAD", "JSON payload: $endpoint.ip, ${endpoint.httpPort}, ${endpoint.secret}")
            Log.d("UDP_OK", "UDP OK: $udpOk")

            val key = loadKey(context) ?: run{
                handleConnectionError(
                    message = "Public key tidak ditemukan. Buat atau pilih key terlebih di pengaturan.",
                    status = ConnectionStatus.FAILED,
                    exception = IllegalStateException("No SSH key in storage")
                )
                return@launch
            }
            updatedStatus(ConnectionStatus.CONNECTING)
            if(!udpOk.result){
                updatedStatus(ConnectionStatus.FAILED)
                return@launch
            }
            /*
            endpoint: QrEndpoint,
            publicKey: String,
            comment: String? = null,
            secret: String
            * */

            Log.d("KEY_PUBLIC", "Key: ${key.publicKeyContent}")
            val httpOk = HttpProbing.sendPublicKey(endpoint, key.publicKeyContent, null, udpOk.newSecret)
            Log.d("HTTP_RESPONSE", "HTTP OK: $httpOk")
            if(!httpOk){
                updatedStatus(ConnectionStatus.FAILED)
                return@launch
            }
            Log.d("KEY_PRIVATE", "Private key: ${key.privateFile}")
            Log.d("KEY_PASSPHRASE", "Passphrase: ${currentState.passphrase}")
            try {
                withContext(Dispatchers.IO) {
                    connection.connect(
                        context = context,
                        host = endpoint.ip,
                        username = endpoint.username,
                        passphrase = currentState.passphrase,
                        privateKeyPath = key.privateFile
                    )

                }

                updatedStatus(ConnectionStatus.CONNECTED)

            } catch (e: Exception) {
                handleConnectionError(
                    "Failed to connect: ${e::class.simpleName}",
                    ConnectionStatus.FAILED,
                    e
                )
            }
        }
    }

    private fun updatedStatus(status: ConnectionStatus){
        _uiState.update { state ->
            state.copy(
                connectionStatus = status,
                isConnecting = status == ConnectionStatus.CONNECTING,
                status = when (status) {
                    ConnectionStatus.CONNECTING -> "Connecting..."
                    ConnectionStatus.CONNECTED  -> "Connected"
                    ConnectionStatus.FAILED     -> "Failed to connect"
                    ConnectionStatus.DISCONNECTED -> "Disconnected"
                    else                        -> state.status
                }
            )
        }
    }

    // ⛔ HAPUS filesystem
    fun selectKeyFromSettings(path: String, filename: String) {
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

    private fun parseQR(raw: String): QrEndpoint? = try{
        val json = JSONObject(raw)
        QrEndpoint(
            ip = json.getString("hostname"),
            username = json.getString("username"),
            httpPort = json.optInt("port"),
            secret = json.optString("session")
        )
    }catch(e: Exception){
        null
    }

    private fun resetState() {
        _uiState.update { UiState() }
    }

    private suspend fun loadKey(context: Context): SSHKeys?{
        return withContext(Dispatchers.IO){
            LoadStorageKey(context).loadKeys().firstOrNull()
        }
    }

    fun disconnect(){
        viewModelScope.launch(Dispatchers.IO){
            try{
                connection.disconnect()
                updatedStatus(ConnectionStatus.DISCONNECTED)
            }catch (e: Exception){
                Log.e("DISCONNECT", "Failed to disconnect: ${e::class.simpleName} - ${e.message}")
                e.printStackTrace()
            }
        }
    }

}