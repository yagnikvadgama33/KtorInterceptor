package com.example.ktorinterceptor.interceptor

import android.util.Base64
import com.example.ktorinterceptor.utils.stringify
import com.example.ktorinterceptor.utils.toBase64
import com.example.ktorinterceptor.utils.toResponseBody
import okhttp3.Interceptor
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.spec.SecretKeySpec

class ApiInterceptor : Interceptor {
    private val secretKey = generateSecretKey()

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        // Encrypt request body if POST
        val encryptedRequest = if (originalRequest.method == "POST") {
            val originalBody = originalRequest.body?.stringify() ?: ""
            val encryptedBody = encryptData(originalBody, secretKey)
            originalRequest.newBuilder()
                .method(
                    originalRequest.method,
                    encryptedBody.toRequestBody(originalRequest.body?.contentType())
                ).build()
        } else {
            originalRequest
        }

        // Proceed with the request
        val response = chain.proceed(encryptedRequest)

        // Decrypt the response body
        val decryptedBody = decryptData(response.body?.string() ?: "", secretKey)

        return response.newBuilder()
            .body(decryptedBody.toResponseBody(response.body?.contentType()))
            .build()
    }

//    fun encryptData(data: String, key: SecretKeySpec): String {
//        val cipher = Cipher.getInstance("AES")
//        cipher.init(Cipher.ENCRYPT_MODE, key)
//        val encryptedBytes = cipher.doFinal(data.toByteArray(Charsets.UTF_8))
//        return Base64.encodeToString(encryptedBytes, Base64.DEFAULT)
//    }
//
//    fun decryptData(data: String, key: SecretKeySpec): String {
//        val cipher = Cipher.getInstance("AES")
//        cipher.init(Cipher.DECRYPT_MODE, key)
//        val decryptedBytes = Base64.decode(data, Base64.DEFAULT)
//        return String(decryptedBytes, Charsets.UTF_8)
//    }

    private fun encryptData(data: String, key: SecretKeySpec): String {
        val cipher = Cipher.getInstance("AES")
        cipher.init(Cipher.ENCRYPT_MODE, key)
        return cipher.doFinal(data.toByteArray()).toBase64()
    }

    private fun decryptData(data: String, key: SecretKeySpec): String {
        val cipher = Cipher.getInstance("AES")
        cipher.init(Cipher.DECRYPT_MODE, key)
        return Base64.decode(data, Base64.URL_SAFE).toString(Charsets.UTF_8)
    }

    private fun generateSecretKey(): SecretKeySpec {
        val keyGen = KeyGenerator.getInstance("AES")
        keyGen.init(128)
        return SecretKeySpec(keyGen.generateKey().encoded, "AES")
    }
}