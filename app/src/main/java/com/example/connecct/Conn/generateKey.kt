package com.example.connecct.Conn

import android.util.Base64
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.openssl.PKCS8Generator
import org.bouncycastle.openssl.jcajce.JcaPEMWriter
import org.bouncycastle.openssl.jcajce.JcaPKCS8Generator
import org.bouncycastle.openssl.jcajce.JceOpenSSLPKCS8EncryptorBuilder
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileWriter
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.SecureRandom
import java.security.Security
import java.security.interfaces.RSAPublicKey

object generateKey {

    fun generateKeyPair(
        keyname: String = "id_rsa_*",
        outputDir: File = File("."),
        passphrase: String? = null
    ): Pair<File, File> {

        // Pastikan BC aktif
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.insertProviderAt(BouncyCastleProvider(), 1)
        }

        // === 1. Generate RSA 2048 key pair ===
        val keygen: KeyPairGenerator = KeyPairGenerator.getInstance("RSA", "BC")
        keygen.initialize(2048, SecureRandom())
        val keyPair: KeyPair = keygen.generateKeyPair()

        val privateKeyFile = File(outputDir, keyname)
        val publicKeyFile = File(outputDir, "$keyname.pub")

        // === 2. PRIVATE KEY (PKCS#8 + optional passphrase) ===
        FileWriter(privateKeyFile).use { fw ->
            JcaPEMWriter(fw).use { pemWriter ->

                val pemObject = if (!passphrase.isNullOrEmpty()) {
                    // Encrypted PKCS#8  (BEGIN ENCRYPTED PRIVATE KEY)
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

        // === 3. PUBLIC KEY (OpenSSH ssh-rsa) ===
        val rsaPub = keyPair.public as RSAPublicKey
        val e = rsaPub.publicExponent  // exponent
        val n = rsaPub.modulus         // modulus

        val keyBlob = buildOpenSSHRsaBlob(e.toByteArray(), n.toByteArray())
        val keyBlobBase64 = Base64.encodeToString(keyBlob, Base64.NO_WRAP)

        val comment = "generated-by-app"  // boleh kamu ganti misalnya "${android.os.Build.MODEL}@android"
        val publicKeyOpenSSH = "ssh-rsa $keyBlobBase64 $comment"

        publicKeyFile.writeText(publicKeyOpenSSH)

        return Pair(privateKeyFile, publicKeyFile)
    }

    /**
     * Build OpenSSH key blob untuk ssh-rsa:
     *  string "ssh-rsa"
     *  mpint e
     *  mpint n
     */
    private fun buildOpenSSHRsaBlob(eBytes: ByteArray, nBytes: ByteArray): ByteArray {
        val baos = ByteArrayOutputStream()
        val dos = DataOutputStream(baos)

        fun writeString(s: String) {
            val b = s.toByteArray(Charsets.US_ASCII)
            dos.writeInt(b.size)
            dos.write(b)
        }

        fun writeMpInt(b: ByteArray) {
            // BigInteger.toByteArray() kadang kasih leading 0, itu masih valid sebagai mpint.
            dos.writeInt(b.size)
            dos.write(b)
        }

        writeString("ssh-rsa")
        writeMpInt(eBytes)
        writeMpInt(nBytes)

        dos.flush()
        return baos.toByteArray()
    }
}
