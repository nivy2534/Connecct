package com.example.connecct.ui.viewmodel

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.connecct.Conn.Connection
import com.example.connecct.Conn.HttpProbing
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
import com.example.connecct.Conn.UDPProbing
import com.example.connecct.storage.LoadStorageKey
import com.example.connecct.ui.state.RemoteFile
import org.json.JSONObject
import com.example.connecct.util.QrEndpoint
import kotlinx.coroutines.flow.asSharedFlow
import java.io.InputStream

class ConnectionViewModel : ViewModel() {

    private val udpProbing = UDPProbing()
    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val connection = Connection()
    private val transport = Transport(connection)

    private val _deviceFromQr = kotlinx.coroutines.flow.MutableSharedFlow<QrEndpoint>(
        replay = 1,
        extraBufferCapacity = 1
    )

    val deviceFromQr = _deviceFromQr.asSharedFlow()

    fun onEvent(event: ConnectionUiEvent) {
        when (event) {
            is ConnectionUiEvent.OnHostChanged -> updateHost(event.host)
            is ConnectionUiEvent.OnUsernameChanged -> updateUsername(event.username)
            is ConnectionUiEvent.OnPassphraseChanged -> updatePassphrase(event.passphrase)
            is ConnectionUiEvent.OnPrivateKeySelected -> selectKey(event.path, event.filename)
            is ConnectionUiEvent.OpenFile -> openFile(event.fileName, event.context)
            is ConnectionUiEvent.UploadFile -> uploadFile(event.uri, event.context)

            is ConnectionUiEvent.DeleteFile -> {
                val path =
                    if (_uiState.value.currentPath == "/")
                        "/${event.fileName}"
                    else
                        "${_uiState.value.currentPath}/${event.fileName}"

                viewModelScope.launch(Dispatchers.IO) {
                    try {
                        transport.deleteRemoteFile(path)
                        loadDirectory()
                    } catch (e: Exception) {
                        _uiState.update { it.copy(errorMessage = "Delete failed: ${e.message}") }
                    }
                }
            }

            is ConnectionUiEvent.MoveFile -> {
                val oldPath =
                    if (_uiState.value.currentPath == "/")
                        "/${event.fileName}"
                    else
                        "${_uiState.value.currentPath}/${event.fileName}"

                viewModelScope.launch(Dispatchers.IO) {
                    try {
                        transport.moveRemoteFile(oldPath, event.newPath)
                        loadDirectory()
                    } catch (e: Exception) {
                        _uiState.update { it.copy(errorMessage = "Move failed: ${e.message}") }
                    }
                }
            }

            is ConnectionUiEvent.RenameFile -> {
                val basePath = _uiState.value.currentPath

                val oldPath =
                    if (basePath == "/") "/${event.oldName}"
                    else "${basePath.trimEnd('/')}/${event.oldName}"

                val newPath =
                    if (basePath == "/") "/${event.newName}"
                    else "${basePath.trimEnd('/')}/${event.newName}"

                viewModelScope.launch(Dispatchers.IO) {
                    try {
                        transport.moveRemoteFile(oldPath, newPath)
                        loadDirectory()
                    } catch (e: Exception) {
                        _uiState.update {
                            it.copy(errorMessage = "Rename failed: ${e.message}")
                        }
                    }
                }
            }

            is ConnectionUiEvent.DownloadFile -> {
                downloadFilePublic(
                    file = event.file,
                    context = event.context
                )
            }

            is ConnectionUiEvent.ShowMoveDialog -> {
                _uiState.update {
                    it.copy(
                        showMoveDialog = true,
                        moveTargetFile = event.fileName
                    )
                }
            }

            is ConnectionUiEvent.DismissMoveDialog -> {
                _uiState.update {
                    it.copy(
                        showMoveDialog = false,
                        moveTargetFile = null
                    )
                }
            }

            is ConnectionUiEvent.SelectMoveTarget -> {
                val fileName = _uiState.value.moveTargetFile ?: return

                val oldPath =
                    if (_uiState.value.currentPath == "/")
                        "/$fileName"
                    else
                        "${_uiState.value.currentPath}/$fileName"

                val newPath =
                    _uiState.value.currentPath.trimEnd('/') + "/" +
                            event.targetDir + "/" +
                            fileName

                viewModelScope.launch(Dispatchers.IO) {
                    try {
                        transport.moveRemoteFile(oldPath, newPath)
                        loadDirectory()
                    } catch (e: Exception) {
                        _uiState.update {
                            it.copy(errorMessage = "Move failed: ${e.message}")
                        }
                    }
                }

                _uiState.update {
                    it.copy(showMoveDialog = false, moveTargetFile = null)
                }
            }

            ConnectionUiEvent.OnResetClicked -> resetState()
            ConnectionUiEvent.CloseFile -> closeFile()

            // Explorer
            ConnectionUiEvent.LoadDirectory -> loadDirectory()
            ConnectionUiEvent.NavigateUp -> navigateUp()
            is ConnectionUiEvent.NavigateTo -> navigateTo(event.directoryName)
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

    fun clearLastQrDevice() {
        _deviceFromQr.resetReplayCache()
    }

    fun connectToServer(
        context: Context,
        onSuccess: (() -> Unit)? = null
        ) {
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

                onSuccess?.invoke()

            } catch (e: Exception) {
                handleConnectionError(
                    "Failed to connect: ${e::class.simpleName}",
                    ConnectionStatus.FAILED,
                    e
                )
            }
        }
    }
    fun connectFromDevice(
        context: Context,
        host: String,
        username: String,
        onSuccess: (() -> Unit)? = null
    ) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    host = host,
                    username = username,
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
                val key = loadKey(context) ?: run {
                    handleConnectionError(
                        message = "Public key tidak ditemukan. Buat atau pilih key dulu di pengaturan.",
                        status = ConnectionStatus.FAILED,
                        exception = IllegalStateException("No SSH key in storage")
                    )
                    return@launch
                }

                val passphrase = _uiState.value.passphrase

                withContext(Dispatchers.IO) {
                    connection.connect(
                        context = context,
                        host = host,
                        username = username,
                        privateKeyPath = key.privateFile,
                        passphrase = passphrase
                    )
                }

                // ✅ Update status sukses
                _uiState.update {
                    it.copy(
                        isConnecting = false,
                        connectionStatus = ConnectionStatus.CONNECTED,
                        status = "Connected"
                    )
                }

                // Ambil home dir & load file list
                val home = withContext(Dispatchers.IO) {
                    transport.getHomeDirectory()
                }
                _uiState.update { it.copy(currentPath = home) }
                loadDirectory()

                onSuccess?.invoke()

            } catch (e: Exception) {
                handleConnectionError(
                    "Failed to connect: ${e::class.simpleName}",
                    ConnectionStatus.FAILED,
                    e
                )
            }
        }
    }

    private fun fail(message: String) {
        Log.e("FAILED", message)
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

    fun loadRootDirectories(onResult: (List<RemoteFile>) -> Unit) {
        if (!connection.isConnected()) return

        viewModelScope.launch {
            try {
                val rootFiles = withContext(Dispatchers.IO) {
                    transport.listDirectory("/")
                }.filter { it.isDirectory }

                onResult(rootFiles)
            } catch (e: Exception) {
                Log.e("ROOT_DIR", "Failed load root dirs", e)
            }
        }
    }

    fun loadDirectoryAt(
        path: String,
        onResult: (List<RemoteFile>) -> Unit
    ) {
        if (!connection.isConnected()) return

        viewModelScope.launch {
            try {
                val files = withContext(Dispatchers.IO) {
                    transport.listDirectory(path)
                }.filter { it.isDirectory }

                onResult(files)
            } catch (e: Exception) {
                Log.e("DIR_PICKER", "Gagal load: $path", e)
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

    fun handleQrConnection(context: Context, qrData: String) {
        Log.d("QR_CONNECTION", "Raw QR data: $qrData")
        val endpoint = parseQR(qrData) ?: run{
            updatedStatus(ConnectionStatus.FAILED)
            return
        }

        _uiState.update {
            it.copy(
                host = endpoint.ip,
                username = endpoint.username
            )
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

                _deviceFromQr.tryEmit(endpoint)

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

    fun uploadFile(uri: Uri, context: Context) {
        if (!connection.isConnected()) {
            _uiState.update { it.copy(errorMessage = "Not connected") }
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                _uiState.update { it.copy(isUploading = true, uploadProgress = 0) }

                val resolver = context.contentResolver
                val inputStream = resolver.openInputStream(uri)
                    ?: throw Exception("Cannot open InputStream")

                val fileName = resolver.getFileName(uri) ?: "uploaded_file"

                val basePath = _uiState.value.currentPath.trimEnd('/')
                val remotePath = if (basePath.isEmpty())
                    "/$fileName"
                else
                    "$basePath/$fileName"

                // ✅ TOTAL SIZE UNTUK HITUNG PERSEN
                val totalSize = resolver.openFileDescriptor(uri, "r")?.statSize ?: -1

                transport.sftpPutWithProgress(
                    inputStream = inputStream,
                    remotePath = remotePath
                ) { sentBytes ->
                    if (totalSize > 0) {
                        val percent = ((sentBytes * 100) / totalSize).toInt()
                        _uiState.update { it.copy(uploadProgress = percent.coerceIn(0, 100)) }
                    }
                }

                inputStream.close()

                loadDirectory()

                _uiState.update {
                    it.copy(
                        isUploading = false,
                        uploadProgress = 100
                    )
                }

            } catch (e: Exception) {
                Log.e("UPLOAD", "UPLOAD FAILED", e)
                _uiState.update {
                    it.copy(
                        isUploading = false,
                        errorMessage = "Upload failed: ${e.localizedMessage}"
                    )
                }
            }
        }
    }

    fun downloadFile(file: RemoteFile, context: Context) {
        if (!connection.isConnected()) return

        val basePath = _uiState.value.currentPath
        val remotePath =
            if (basePath == "/") "/${file.name}"
            else "${basePath.trimEnd('/')}/${file.name}"

        val downloadDir = File(
            context.getExternalFilesDir(null),
            "downloads"
        )
        if (!downloadDir.exists()) downloadDir.mkdirs()

        val localFile = File(downloadDir, file.name)

        viewModelScope.launch(Dispatchers.IO) {
            try {
                // 🔥 AUTO SWITCH
                if (file.size <= 5 * 1024 * 1024) {
                    // SMALL FILE
                    val bytes = transport.readFileBytes(remotePath)
                    localFile.writeBytes(bytes)

                } else {
                    // LARGE FILE
                    _uiState.update { it.copy(isDownloading = true, downloadProgress = 0) }

                    transport.downloadFile(remotePath, localFile) { percent ->
                        _uiState.update {
                            it.copy(downloadProgress = percent.toInt())
                        }
                    }

                    _uiState.update { it.copy(isDownloading = false) }
                }

                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        context,
                        "Downloaded: ${localFile.absolutePath}",
                        Toast.LENGTH_LONG
                    ).show()
                }

            } catch (e: Exception) {
                _uiState.update { it.copy(isDownloading = false) }

                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        context,
                        "Download failed: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    fun downloadFilePublic(
        file: RemoteFile,
        context: Context
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val resolver = context.contentResolver

                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, file.name)
                    put(MediaStore.MediaColumns.MIME_TYPE, getMimeType(file.name))

                    // 🔥 ini kunci agar masuk folder Download
                    put(
                        MediaStore.MediaColumns.RELATIVE_PATH,
                        Environment.DIRECTORY_DOWNLOADS
                    )

                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }

                val uri = resolver.insert(
                    MediaStore.Files.getContentUri("external"),
                    values
                ) ?: throw IllegalStateException("Failed to create MediaStore entry")

                resolver.openOutputStream(uri)?.use { output ->
                    transport.downloadFileToStream(
                        remotePath = "${_uiState.value.currentPath}/${file.name}",
                        outputStream = output
                    )
                }

                values.clear()
                values.put(MediaStore.MediaColumns.IS_PENDING, 0)
                resolver.update(uri, values, null, null)

                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        context,
                        "Downloaded to Downloads/${file.name}",
                        Toast.LENGTH_LONG
                    ).show()
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        context,
                        "Download failed: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    fun getMimeType(name: String): String {
        val ext = name.substringAfterLast('.', "").lowercase()
        return when (ext) {
            "mp4" -> "video/mp4"
            "m4a" -> "audio/mp4"
            "mp3" -> "audio/mpeg"
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "pdf" -> "application/pdf"
            else -> "application/octet-stream"
        }
    }

    private fun ContentResolver.getFileName(uri: Uri): String? {
        val cursor = query(uri, null, null, null, null) ?: return null
        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        cursor.moveToFirst()
        val name = cursor.getString(nameIndex)
        cursor.close()
        return name
    }

    private fun ContentResolver.getFileSize(uri: Uri): Long? {
        val cursor = query(uri, null, null, null, null) ?: return null
        val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
        if (sizeIndex == -1) {
            cursor.close()
            return null
        }
        cursor.moveToFirst()
        val size = cursor.getLong(sizeIndex)
        cursor.close()
        return size
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