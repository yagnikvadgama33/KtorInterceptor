package com.example.ktorinterceptor.interceptor

import android.content.Context
import android.util.Log
import com.example.ktorinterceptor.R
import com.example.ktorinterceptor.utils.EncryptionUtils
import com.example.ktorinterceptor.utils.NoInternetException
import com.example.ktorinterceptor.utils.isInternetAvailable
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.HttpResponseValidator
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.Parameters
import io.ktor.http.contentType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody.Companion.toResponseBody

class KtorClient(private val context: Context, private val secretKey: String) {

    private val client = HttpClient(OkHttp) {
        install(Logging) {
            level = LogLevel.ALL
        }

        engine {
            addInterceptor { chain ->
                if (!isInternetAvailable(context)) {
                    throw NoInternetException(context.getString(R.string.no_internet_connection))
                }

                val originalRequest = chain.request()

                val encryptedBody = originalRequest.body?.toString()?.let { EncryptionUtils.encrypt(it, secretKey) }

                Log.d("KtorClient", "Encrypted Response: $encryptedBody")

                val request = originalRequest.newBuilder()
                    .method(originalRequest.method, encryptedBody?.toRequestBody())
                    .build()
                var response = chain.proceed(request)
                var attempt = 0
                val maxRetries = 3
                while (attempt < maxRetries && !response.isSuccessful && response.code in listOf(500, 503)) {
                    CoroutineScope(Dispatchers.IO).launch{
                        delay(2000L) // Delay between retries
                        response = chain.proceed(request)
                        attempt++
                    }
                }

                // Decrypt response
                val decryptedResponseBody = response.body?.string()?.let { EncryptionUtils.decrypt(it, secretKey) }

               Log.d("KtorClient", "Decrypted Response: ${decryptedResponseBody?.toResponseBody()}")

                // Return modified response
                response.newBuilder()
                    .body(decryptedResponseBody?.toResponseBody())
                    .build()
            }
        }

        HttpResponseValidator {
            validateResponse { response ->
                if (response.status.value !in 200..299) {
                    throw Exception("HTTP Error: ${response.status.value}")
                }
            }
            handleResponseExceptionWithRequest { cause,httpReq ->
                throw Exception("Network Error: ${httpReq.url} -> ${cause.localizedMessage}")
            }
        }
    }

    // Example API call
    suspend fun getMoviesData(): HttpResponse {
        return client.get("https://dummyjson.com/posts/1")
    }

    suspend fun postRequest(
        url: String,
        formData: Map<String, String>
    ): String {
        return try {
            println("Request URL: $url")
            println("Request Body: $formData")

            // Send the POST request with form-data
            val response: HttpResponse = client.post(url) {
                contentType(ContentType.Application.FormUrlEncoded)
                setBody(Parameters.build {
                    formData.forEach { (key, value) ->
                        append(key, value)
                    }
                })
            }

            // Log and return the response
            val responseBody = response.bodyAsText()
            println("Response: $responseBody")
            responseBody
        } catch (e: Exception) {
            e.printStackTrace()
            "Network Error: ${e.message}"
        }
    }
}

