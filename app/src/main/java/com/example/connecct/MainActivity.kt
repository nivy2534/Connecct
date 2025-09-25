package com.example.connecct

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
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


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SSH()
        }
    }
}

@Composable
fun SSH() {
    var host by remember { mutableStateOf("") }
    var uname by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("idle") }
    var scope = rememberCoroutineScope()
    val ssh = SSHClient()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ){
        OutlinedTextField(
            value = host,
            onValueChange = { host = it },
            label = { Text("Host IP")}
        )

        OutlinedTextField(
            value = uname,
            onValueChange = {uname = it},
            label = { Text("PC Username")}
        )

        Button(onClick = {
            scope.launch(Dispatchers.IO){
                try{
                    status = "Connecting..."
                    val ssh = SSHClient()
                    ssh.addHostKeyVerifier(PromiscuousVerifier())
                    ssh.connect(host)
                    val keyProvider = ssh.loadKeys("/storage/emulated/0/ssh/id_rsa")
                    ssh.authPublickey(uname, keyProvider)
                    status = "Connected!"
                    ssh.disconnect()
                }catch (e: Exception){
                    status = "Error: ${e.message}"
                }
            }
        }){
            Text("Connect")
        }

        Text("Status: $status")
    }
}

fun generateSSHKey(privateKeyPath: String, publicKeyPath: String){
    Security.addProvider(BouncyCastleProvider())

    val keygen = KeyPairGenerator.getInstance("RSA", "BC")
    keygen.initialize(4096)
    val pair: KeyPair = keygen.generateKeyPair()

    val privateKeyFile = File(privateKeyPath)
    privateKeyFile.writeBytes(pair.private.encoded)

    val publicKeyFile = File(publicKeyPath)
    publicKeyFile.writeBytes(pair.public.encoded)

}

fun savePrivateKeyOpenSSH(keyPair: KeyPair, path: String){
    val writer = JcaPEMWriter(FileWriter(path))
    writer.writeObject(keyPair.private)
    writer.close()
}