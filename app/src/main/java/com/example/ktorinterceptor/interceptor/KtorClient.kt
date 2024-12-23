package com.example.ktorinterceptor.interceptor

import android.content.Context
import android.util.Log
import com.example.ktorinterceptor.R
import com.example.ktorinterceptor.utils.EncryptionUtils
import com.example.ktorinterceptor.utils.NoInternetException
import com.example.ktorinterceptor.utils.isInternetAvailable
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
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

                // Encrypt request body for methods like POST, PUT, etc.
//                val encryptedBody = if (originalRequest.method.equals(io.ktor.http.HttpMethod.Get)) {
//                    val bodyString = originalRequest.body?.let {
//                        // Assuming the body is JSON or string, convert accordingly if not
//                        it.toString() // Modify this if the body needs to be serialized
//                    } ?: ""
//                    EncryptionUtils.encrypt(bodyString, secretKey)
//                } else {
//                    null
//                }
//
//                Log.d("KtorClient", "Encrypted Request: $encryptedBody")

                val request =
//                    if (encryptedBody != null) {
                    originalRequest.newBuilder()
                        .method(originalRequest.method, originalRequest.body)
                        .build()
//                } else {
//                    originalRequest
//                }

                // Retry logic for response (synchronous retry without coroutines)
                var attempt = 0
                var response = chain.proceed(request)
                val maxRetries = 3
                while (attempt < maxRetries && response.code in 500..599) {
                    Thread.sleep(2000L) // Delay between retries
                    response = chain.proceed(request)
                    attempt++
                }

                // Decrypt response body if it's encrypted
//                val decryptedResponseBody = response.body?.let { body ->
//                    val bodyString = body.string()
//                    // Assuming the body is encrypted, decrypt it
//                    val decrypted = EncryptionUtils.decrypt(bodyString, secretKey)
//                    decrypted
//                }
//
//                Log.d("KtorClient", "Decrypted Response: $decryptedResponseBody")

                // Return modified response with decrypted body
                response.newBuilder()
                    .body(response.body)
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

            val response: HttpResponse = client.put("https://dummyjson.com/posts/1") {
                contentType(ContentType.Application.FormUrlEncoded)
                setBody(formDataList.formUrlEncode())
            }

            val responseBody = response.bodyAsText()
            println("Response: $responseBody")
            responseBody
        } catch (e: Exception) {
            e.printStackTrace()
            "Network Error: ${e.message}"
        }
    }

}


