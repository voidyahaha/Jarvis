package com.aether.assistant.core.memory

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Real local encryption backed by the Android Keystore.
 * The AES key never leaves secure hardware (when available). Used for memory
 * entries marked sensitive and for user-supplied API tokens.
 */
class CryptoBox(context: Context) {
    private val keyStore: KeyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }

    init {
        if (!keyStore.containsAlias(KEY_ALIAS)) {
            val keyGen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
            val spec = KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .setUserAuthenticationRequired(false)
                .build()
            keyGen.init(spec)
            keyGen.generateKey()
        }
    }

    private fun key(): SecretKey = keyStore.getKey(KEY_ALIAS, null) as SecretKey

    fun encrypt(plain: String): EncryptedBlob {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val iv = cipher.iv
        val cipherText = cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
        return EncryptedBlob(
            iv = Base64.getEncoder().encodeToString(iv),
            data = Base64.getEncoder().encodeToString(cipherText)
        )
    }

    fun decrypt(blob: EncryptedBlob): String {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val iv = Base64.getDecoder().decode(blob.iv)
        cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, iv))
        return String(cipher.doFinal(Base64.getDecoder().decode(blob.data)), Charsets.UTF_8)
    }

    data class EncryptedBlob(val iv: String, val data: String) {
        fun serialize(): String = "$iv|:|$data"
        companion object {
            fun deserialize(raw: String): EncryptedBlob {
                val parts = raw.split("|:|", limit = 2)
                require(parts.size == 2) { "Corrupt encrypted blob" }
                return EncryptedBlob(parts[0], parts[1])
            }
        }
    }

    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "aether_master_key_v1"
    }
}
