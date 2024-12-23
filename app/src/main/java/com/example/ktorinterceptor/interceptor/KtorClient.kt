package com.example.ktorinterceptor.interceptor

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.example.ktorinterceptor.R
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class KtorClient(private val context: Context) {

    private val client = HttpClient(OkHttp) {
        install(Logging) {
            level = LogLevel.ALL
        }

        engine {
            addInterceptor { chain ->
                val originalRequest = chain.request()

                // Retry logic
                var attempt = 0
                var response = chain.proceed(originalRequest)
                val maxRetries = 3
                while (attempt < maxRetries && response.code in 201..503) {
                    attempt++
                    notifyRetry(attempt) // Notify UI about the retry
                    runBlocking {
                        delay(3000L)
                    }
                    response.close()
                    response = chain.proceed(originalRequest)
                    Log.d("KtorClient", "API Retrying... $attempt Times")
                }

                response
            }
        }
    }

    suspend fun getMoviesData(id: String): String {
        val response = client.get("https://dummyjson.com/posts/$id")
        val responseBody = response.bodyAsText()
        var modifiedResponse = ""

        if (response.status.value !in 201..503) {
            modifiedResponse =
                "{" + "status_code" + ":" + "${response.status.value} " + "," + responseBody + "}"
            Log.d("KtorClient", "Modified Response: $modifiedResponse")
        }

        Log.d("KtorClient", "Original Response: $responseBody")

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
            Log.d("KtorClient", "Response: $responseBody")

            responseBody
        } catch (e: Exception) {
            e.printStackTrace()
            "Network Error: ${e.message}"
        }
    }

    private fun notifyRetry(attempt: Int) {
        CoroutineScope(Dispatchers.Main).launch {
            Toast.makeText(
                context,
                context.getString(R.string.retrying_api_call_attempt, attempt.toString()),
                Toast.LENGTH_SHORT
            )
                .show()
        }
    }
}

fun Map<String, String>.formUrlEncode(): String {
    return this.entries.joinToString("&") { (key, value) ->
        "${key.urlEncode()}=${value.urlEncode()}"
    }
}

fun String.urlEncode(): String = java.net.URLEncoder.encode(this, "UTF-8")
