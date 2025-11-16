package com.example.connecct.Conn

import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.openssl.jcajce.JcaPEMWriter
import org.bouncycastle.openssl.jcajce.JcaPKCS8Generator
import org.bouncycastle.openssl.jcajce.JceOpenSSLPKCS8EncryptorBuilder
import org.bouncycastle.openssl.PKCS8Generator
import java.io.File
import java.io.FileWriter
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.SecureRandom
import java.security.Security
import android.util.Base64
import kotlin.Pair

object generateKey {

    fun generateKeyPair(
        keyname: String = "id_rsa_*",
        outputDir: File = File("."),
        passphrase: String? = null
    ): Pair<File, File> {

        // tapi tidak salah kalau kita pastikan lagi di sini.
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.insertProviderAt(BouncyCastleProvider(), 1)
        }

        // Pakai RSA dari provider BC biar konsisten
        val keygen: KeyPairGenerator = KeyPairGenerator.getInstance("RSA", "BC")
        keygen.initialize(2048, SecureRandom())
        val keyPair: KeyPair = keygen.generateKeyPair()

        val privateKeyFile = File(outputDir, keyname)
        val publicKeyFile = File(outputDir, "$keyname.pub")

        // ================================
        // PRIVATE KEY (PKCS#8 + optional passphrase)
        // ================================
        FileWriter(privateKeyFile).use { fw ->
            JcaPEMWriter(fw).use { pemWriter ->

                val pemObject = if (!passphrase.isNullOrEmpty()) {
                    // Encrypted PKCS#8 (BEGIN ENCRYPTED PRIVATE KEY)
                    val encryptor = JceOpenSSLPKCS8EncryptorBuilder(PKCS8Generator.AES_256_CBC)
                        .setRandom(SecureRandom())
                        .setPassword(passphrase.toCharArray())
                        .build()

                    JcaPKCS8Generator(keyPair.private, encryptor)
                } else {
                    // Unencrypted PKCS#8 (BEGIN PRIVATE KEY)
                    JcaPKCS8Generator(keyPair.private, null)
                }

                pemWriter.writeObject(pemObject)
            }
        }

        // ================================
        // PUBLIC KEY (OpenSSH-like)
        // ================================
        val publicKeyBytes = keyPair.public.encoded
        val publicKeyBase64 = Base64.encodeToString(publicKeyBytes, Base64.NO_WRAP)
        val publicKeyOpenSSH = "ssh-rsa $publicKeyBase64 generated-by-app"
        publicKeyFile.writeText(publicKeyOpenSSH)

        return Pair(privateKeyFile, publicKeyFile)
    }
}
