package com.example.ktorinterceptor.utils

import android.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec
import android.content.Context
import javax.crypto.KeyGenerator

object EncryptionUtils {
    private const val ALGORITHM = "AES"
    private const val SECRET_KEY = "SECRET_KEY"
    private const val PREF_NAME = "secure_shared_pref"

    fun generateSecretKey(context: Context): String {
        val sharedPrefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val existingKey = sharedPrefs.getString(SECRET_KEY, null)
        if (existingKey != null) {
            return existingKey
        }

        val keyGenerator = KeyGenerator.getInstance(ALGORITHM)
        keyGenerator.init(128) // Generate 128-bit key
        val secretKey = keyGenerator.generateKey()
        val encodedKey = Base64.encodeToString(secretKey.encoded, Base64.DEFAULT)

        // Store the key securely in SharedPreferences
        sharedPrefs.edit().putString(SECRET_KEY, encodedKey).apply()

        return encodedKey
    }


    fun encrypt(data: String, key: String): String {
        val cipher = Cipher.getInstance(ALGORITHM)
        val secretKey = SecretKeySpec(Base64.decode(key, Base64.DEFAULT), ALGORITHM)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        val encryptedBytes = cipher.doFinal(data.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(encryptedBytes, Base64.DEFAULT)
    }

    fun decrypt(data: String, key: String): String {
        val cipher = Cipher.getInstance(ALGORITHM)
        val secretKey = SecretKeySpec(Base64.decode(key, Base64.DEFAULT), ALGORITHM)
        cipher.init(Cipher.DECRYPT_MODE, secretKey)
        val decryptedBytes = Base64.decode(data, Base64.DEFAULT)
        return String(decryptedBytes, Charsets.UTF_8)
    }
}


