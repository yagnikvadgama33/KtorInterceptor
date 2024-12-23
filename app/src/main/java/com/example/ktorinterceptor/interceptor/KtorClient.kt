package com.example.ktorinterceptor.interceptor

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.example.ktorinterceptor.utils.EncryptionUtils
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpResponseValidator
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.get
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.formUrlEncode
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody.Companion.toResponseBody

class KtorClient(private val context: Context, private val secretKey: String) {

    private val client = HttpClient(OkHttp) {
        install(Logging) {
            level = LogLevel.ALL
        }

        engine {
            addInterceptor { chain ->
                val originalRequest = chain.request()

                // Encrypt
                val encryptedBody = when {
                    originalRequest.method.equals(io.ktor.http.HttpMethod.Post) ||
                            originalRequest.method.equals(io.ktor.http.HttpMethod.Put) -> {
                        val bodyString = originalRequest.body?.let {
                            it.toString()
                        } ?: ""
                        EncryptionUtils.encrypt(bodyString, secretKey)
                    }

                    else -> null
                }

                Log.d("KtorClient", "Encrypted Request: $encryptedBody")

                // If the body is encrypted
                val request = if (encryptedBody != null) {
                    originalRequest.newBuilder()
                        .method(
                            originalRequest.method,
                            encryptedBody.toRequestBody(originalRequest.body?.contentType())
                        )
                        .build()
                } else {
                    originalRequest
                }

                // Retry
                var attempt = 0
                var response = chain.proceed(request)
                val maxRetries = 3
                while (attempt < maxRetries && response.code in 201..503) {
                    Thread.sleep(2000L)
                    response = chain.proceed(request)
                    attempt++
                    Toast.makeText(context, "API Retrying...", Toast.LENGTH_SHORT).show()
                }

                // Decrypt
                val decryptedResponseBody = response.body?.let { body ->
                    val bodyString = body.string()
                    val decrypted = EncryptionUtils.decrypt(bodyString, secretKey)
                    decrypted
                }

                Log.d("KtorClient", "Decrypted Response: $decryptedResponseBody")

                response.newBuilder()
                    .body(decryptedResponseBody?.toResponseBody(response.body?.contentType()))
                    .build()
            }
        }

        HttpResponseValidator {
            validateResponse { response ->
                if (response.status.value !in 200..299) {
                    throw Exception("HTTP Error: ${response.status.value}")
                }
            }
            handleResponseExceptionWithRequest { cause, httpReq ->
                throw Exception("Network Error: ${httpReq.url} -> ${cause.localizedMessage}")
            }
        }
    }

    suspend fun getMoviesData(): HttpResponse {
        return client.get("https://dummyjson.com/posts/1")
    }

    suspend fun postRequest(
        formData: Map<String, String>
    ): String {
        return try {
            val formDataList = formData.map { it.toPair() }

            val encryptedData = EncryptionUtils.encrypt(formDataList.formUrlEncode(), secretKey)

            val response: HttpResponse = client.put("https://dummyjson.com/posts/1") {
                contentType(ContentType.Application.FormUrlEncoded)
                setBody(encryptedData)
            }

            val responseBody = response.bodyAsText()
            println("POST Response: $responseBody")
            responseBody
        } catch (e: Exception) {
            e.printStackTrace()
            "Network Error: ${e.message}"
        }
    }
}



