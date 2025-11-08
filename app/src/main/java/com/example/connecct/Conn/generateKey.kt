package com.example.connecct.Conn

import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.openssl.jcajce.JcaPEMWriter
import org.bouncycastle.openssl.jcajce.JcePEMEncryptorBuilder
import java.io.File
import java.io.FileWriter
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.Security
import android.util.*

object generateKey {
    fun generateKey(
        keyname: String = "id_rsa_*",
        outputDir: File = File("."),
        passphrase: String? = null
    ):Pair<File, File>{
        Security.addProvider(BouncyCastleProvider())

        val keygen = KeyPairGenerator.getInstance("RSA", "BC")
        keygen.initialize(2048)
        val keyPair: KeyPair = keygen.generateKeyPair()

        val privateKey = File(outputDir, keyname)
        val publicKey = File(outputDir, "$keyname.pub")

        FileWriter(privateKey).use { fw ->
            JcaPEMWriter(fw).use { pemWriter ->
                if (passphrase != null && passphrase.isNotEmpty()) {
                    val encryptor = JcePEMEncryptorBuilder("AES-256-CBC")
                        .build(passphrase.toCharArray())
                    pemWriter.writeObject(keyPair.private, encryptor)
                } else {
                    pemWriter.writeObject(keyPair.private)
                }
            }
        }

        val publicKeyBytes = keyPair.public.encoded
        val publicKeyBase64 = Base64.encodeToString(publicKeyBytes, Base64.NO_WRAP)
        val publicKeyOpenSSH = "ssh-rsa $publicKeyBase64 generated-by-app"

        publicKey.writeText(publicKeyOpenSSH)

        return Pair(privateKey, publicKey)
    }
}
