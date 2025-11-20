package com.example.connecct.util

import android.util.Base64
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.interfaces.RSAPublicKey
import javax.crypto.Cipher

object SshKeyUtils {

    fun generateRsaKeyPair(): KeyPair {
        val generator = KeyPairGenerator.getInstance("RSA")
        generator.initialize(2048)
        return generator.genKeyPair()
    }

    // Convert public key to SSH format (ssh-rsa AAAAB3...)
    fun convertPublicKeyToOpenSshFormat(publicKey: RSAPublicKey): String {
        val keyType = "ssh-rsa".toByteArray()

        fun lengthPrefix(data: ByteArray): ByteArray {
            val length = data.size
            return byteArrayOf(
                (length shr 24 and 0xFF).toByte(),
                (length shr 16 and 0xFF).toByte(),
                (length shr 8 and 0xFF).toByte(),
                (length and 0xFF).toByte()
            ) + data
        }

        val exponentBytes = publicKey.publicExponent.toByteArray()
        val modulusBytes = publicKey.modulus.toByteArray()

        val keyBlob =
            lengthPrefix(keyType) +
                    lengthPrefix(exponentBytes) +
                    lengthPrefix(modulusBytes)

        val base64 = Base64.encodeToString(keyBlob, Base64.NO_WRAP)

        return "ssh-rsa $base64"
    }
}