package com.example.connecct

import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.example.connecct.Conn.*
import kotlinx.coroutines.withContext
import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.userauth.UserAuthException
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.security.Security


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ConnectScreen()
        }
    }
}

@Composable
fun ConnectScreen(){
    val context = LocalContext.current
    var host by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var privateKey by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("Idle") }
    var isConnecting by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val connection = remember { Connection() }
    var passphrase by remember { mutableStateOf("") }

    var filename by remember {mutableStateOf("")}
    var filesize by remember {mutableStateOf("")}

    var providers by remember {mutableStateOf("")}

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri ->
            if (uri != null){
                privateKey = uri.toString()
                val (name, size) = getFileMetadata(context, Uri.parse(privateKey))
                filename = name
                filesize = size
            }
        }
    )

    Surface(modifier = Modifier.fillMaxSize()){
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ){
            //host input
            OutlinedTextField(
                value = host,
                onValueChange = { host=it },
                label = { Text("Host") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(8.dp))

            //username input
            OutlinedTextField(
                  value = username,
                  onValueChange = { username = it },
                  label = {Text("Username")},
                  modifier = Modifier.fillMaxWidth()
              )

            Spacer(Modifier.height(8.dp))

            //passphrase
            OutlinedTextField(
                value = passphrase,
                onValueChange = { passphrase = it },
                label = {Text("Passphrase")},
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(8.dp))

            //file picker button
            Button(
                onClick = {
                    filePicker.launch("*/*")
                },
                modifier = Modifier.fillMaxWidth()
            ){
                Text("Pilih Private Key")
            }

            if (privateKey.isNotEmpty()) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    Text("URI: $privateKey")
                    Text("Filename : $filename")
                    Text("Filesize : $filesize")

                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = {
                            val uri = Uri.parse(privateKey)
                            try{
                                val inputStream = context.contentResolver.openInputStream(uri)
                                val keyContent = inputStream?.bufferedReader().use { it?.readText() } ?:""
                                status = if(keyContent.isNotEmpty()){
                                    "Isi private key: \n" + keyContent.take(200) + "..."
                                }else{
                                    "File kosong"
                                }
                            }catch (e: Exception){
                                status = "Gagal membaca file: ${e::class.simpleName} - ${e.message}"
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Periksa Private Key")
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = {
                    scope.launch {
                        isConnecting = true
                        status = "Connecting..."
                        try{
                            withContext(Dispatchers.IO){
                                connection.connect(
                                    context = context,
                                    host = host,
                                    username = username,
                                    privateKeyPath = privateKey,
                                    passphrase = passphrase,
                                )
                            }
                        }catch (e: java.io.FileNotFoundException){
                            status = "Error: Private key not found - ${e::class.simpleName} - ${e.message}"
                        }catch (e: java.net.UnknownHostException) {
                            status = "Error: Unknown host '$host'"
                        }catch (e: java.net.SocketTimeoutException){
                            status = "Error: Connection timed out - ${e.message}"
                        }catch (e: IllegalArgumentException) {
                            status = "Error: Invalid input — check host, username, or key path"
                        }catch (e: UserAuthException){
                            status = "Error: Authentication failed - ${e::class.simpleName} - ${e.message}"
                        }catch(e: Exception){
                            status = "Failed to connect : ${e::class.simpleName} - ${e.message}"
                            //status = providers
                            e.printStackTrace()
                        }finally{
                            isConnecting = false
                        }
                    }
                },
                enabled = !isConnecting,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (isConnecting) "Connecting..." else "Connect")
            }

            Spacer(Modifier.height(16.dp))
            Text( text = "Status: $status")
        }
    }

}

@Composable
fun FilePickerContract() {
    TODO("Not yet implemented")
}

fun getFileMetadata(context: Context, uri: Uri): Pair<String, String>{
    val docfile = DocumentFile.fromSingleUri(context, uri)
    val name = docfile?.name ?:"Unknown"
    val sizeByBytes = docfile?.length() ?: 0L
    val sizeInKB = String.format("%.2f KB", sizeByBytes / 1024.0)
    return name to sizeInKB
}
