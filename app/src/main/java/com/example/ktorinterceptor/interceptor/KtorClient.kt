package com.example.ktorinterceptor.interceptor

import android.content.Context
import android.util.Log
import android.widget.Toast
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.get
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType

class KtorClient(private val context: Context) {

    private val client = HttpClient(OkHttp) {
        install(Logging) {
            level = LogLevel.ALL
        }

        engine {
            addInterceptor { chain ->
                val originalRequest = chain.request()

                // Retry
                var attempt = 0
                var response = chain.proceed(originalRequest)
                val maxRetries = 3
                while (attempt < maxRetries && response.code in 201..503) {
                    Thread.sleep(2000L)
                    response = chain.proceed(originalRequest)
                    attempt++
                    Toast.makeText(context, "API Retrying...", Toast.LENGTH_SHORT).show()
                }

                response
            }
        }

//        HttpResponseValidator {
//            validateResponse { response ->
//                if (response.status.value !in 200..299) {
//                    throw Exception("HTTP Error: ${response.status.value}")
//                }
//            }
//            handleResponseExceptionWithRequest { cause, httpReq ->
//                throw Exception("Network Error: ${httpReq.url} -> ${cause.localizedMessage}")
//            }
//        }
    }

    suspend fun getMoviesData(): String {
        val response = client.get("https://dummyjson.com/posts/1")
        val responseBody = response.bodyAsText()

        val modifiedResponse =
            "{" + "status_code" + ":" + "${response.status.value} " + "," + responseBody + "}"
        Log.d("KtorClient", "Modified Response: $modifiedResponse")

        return modifiedResponse
    }

    suspend fun postRequest(
        formData: Map<String, String>
    ): String {
        return try {
            val filteredFormData = formData.filterKeys { it != "status_code" }

            val response: HttpResponse = client.put("https://dummyjson.com/posts/1") {
                contentType(ContentType.Application.FormUrlEncoded)
                setBody(filteredFormData.formUrlEncode())
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

fun Map<String, String>.formUrlEncode(): String {
    return this.entries.joinToString("&") { (key, value) ->
        "${key.urlEncode()}=${value.urlEncode()}"
    }
}

fun String.urlEncode(): String = java.net.URLEncoder.encode(this, "UTF-8")