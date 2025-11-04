package com.example.connecct

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.dp
import com.example.connecct.ui.theme.ConnecctTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.transport.verification.PromiscuousVerifier
import net.schmizz.sshj.xfer.scp.SCPFileTransfer
import net.schmizz.sshj.xfer.FileSystemFile
import org.bouncycastle.jcajce.provider.asymmetric.elgamal.BCElGamalPrivateKey
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.openssl.jcajce.JcaPEMWriter
import java.io.FileWriter
import java.io.File
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.Security
import kotlin.coroutines.CoroutineContext
import com.example.connecct.Conn.*


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
    var host by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var privateKey by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("Idle") }
    var isConnecting by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val connection = remember { Connection() }

    Surface(modifier = Modifier.fillMaxSize()){
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ){
            OutlinedTextField(
                value = host,
                onValueChange = { host=it },
                label = { Text("Host") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                  value = username,
                  onValueChange = { username = it },
                  label = {Text("Username")},
                  modifier = Modifier.fillMaxWidth()
              )

            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = privateKey,
                onValueChange = { privateKey = it },
                label = {Text("Private Key")},
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = {
                    scope.launch {
                        isConnecting = true
                        status = "Connecting..."
                        try{
                            connection.connect(host, username, privateKey)
                        }catch(e: Exception){
                            status = "Failed to connect : ${e.message}"
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
