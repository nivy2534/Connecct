package com.example.connecct.ui.state

import android.content.Context
import android.net.Uri

data class UiState(
    val host: String = "",
    val username: String = "",
    val privateKeyPath: String = "",
    val passphrase: String = "",
    val status: String = "Idle",
    val connectionStatus: ConnectionStatus = ConnectionStatus.IDLE,
    val isConnecting: Boolean = false,

    // --- File metadata (local key) ---
    val filename: String = "",
    val filesize: String = "",
    val errorMessage: String = "",
    val isKeySelected: Boolean = false,

    // --- File Explorer (remote SSH) ---
    val currentPath: String = "/",
    val isLoadingDirectory: Boolean = false,
    val remoteFiles: List<RemoteFile> = emptyList(),

    // --- Opened file (ANY TYPE: text, image, pdf, etc) ---
    val openedFile: OpenedFile? = null,

    // Upload File
    val isUploading: Boolean = false,
    val uploadProgress: Int = 0,

    // Move File
    val showMoveDialog: Boolean = false,
    val moveTargetFile: String? = null,

    // Download File
    val isDownloading: Boolean = false,
    val downloadProgress: Int = 0,
    val downloadingFileName: String? = null
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

sealed class ConnectionUiEvent {
    data class OnHostChanged(val host: String) : ConnectionUiEvent()
    data class OnUsernameChanged(val username: String) : ConnectionUiEvent()
    data class OnPassphraseChanged(val passphrase: String) : ConnectionUiEvent()
    data class OnPrivateKeySelected(val path: String, val filename: String) : ConnectionUiEvent()
    data class OpenFile(val fileName: String, val context: Context) : ConnectionUiEvent()
    data class UploadFile(val uri: Uri, val context: Context) : ConnectionUiEvent()
    data class DeleteFile(val fileName: String) : ConnectionUiEvent()
    data class MoveFile(val fileName: String, val newPath: String) : ConnectionUiEvent()
    data class ShowMoveDialog(val fileName: String) : ConnectionUiEvent()
    data class SelectMoveTarget(val targetDir: String) : ConnectionUiEvent()
    data class DownloadFile(val file: RemoteFile, val context: Context) : ConnectionUiEvent()
    data class DownloadProgress(val progress: Int) : ConnectionUiEvent()
    data class RenameFile(val oldName: String, val newName: String) : ConnectionUiEvent()
    data class CreateFolder(val name: String) : ConnectionUiEvent()

    data class LoadDirectoryAt(val path: String) : ConnectionUiEvent()
    data class UploadStarted(val fileName: String) : ConnectionUiEvent()

    object OnConnectClicked : ConnectionUiEvent()
    object OnResetClicked : ConnectionUiEvent()
    object OnChooseKeyClicked : ConnectionUiEvent()
    object CloseFile : ConnectionUiEvent()
    object DismissMoveDialog : ConnectionUiEvent()
    object UploadFinished : ConnectionUiEvent()
    object DownloadFinished : ConnectionUiEvent()

    // Explorer events
    object LoadDirectory : ConnectionUiEvent()
    object NavigateUp : ConnectionUiEvent()
    data class NavigateTo(val directoryName: String) : ConnectionUiEvent()
}

// --- NEW: Multi-type opened file support ---
sealed class OpenedFile {

    data class Text(val name: String, val content: String) : OpenedFile()

    data class Image(val name: String, val bytes: ByteArray) : OpenedFile()

    data class Pdf(val name: String, val bytes: ByteArray) : OpenedFile()

    data class Video(val name: String, val bytes: ByteArray) : OpenedFile()

    data class External(val name: String, val bytes: ByteArray, val mime: String) : OpenedFile()

    data class Unsupported(val name: String, val bytes: ByteArray) : OpenedFile()
}