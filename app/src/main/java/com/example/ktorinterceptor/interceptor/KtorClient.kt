package com.example.ktorinterceptor.interceptor

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import android.widget.Toast
import com.example.ktorinterceptor.R
import com.example.ktorinterceptor.utils.formUrlEncode
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
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody.Companion.toResponseBody
import org.json.JSONException
import org.json.JSONObject

class KtorClient(private val context: Context) {

    private val sharedPreferences: SharedPreferences = context.getSharedPreferences("api_prefs", Context.MODE_PRIVATE)

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
                    notifyRetry(attempt)
                    runBlocking {
                        delay(3000L)
                    }
                    response.close()
                    response = chain.proceed(originalRequest)
                }

                // Save GET response data
                if (originalRequest.method == "GET" && response.isSuccessful) {
                    val responseBody = response.body?.string() ?: ""
                    try {
                        JSONObject(responseBody)
                        val modifiedBody = """{"status_code": ${response.code},"data": $responseBody}""".trimIndent()

                        sharedPreferences.edit().putString("get_response_data", modifiedBody).apply()

                        response = response.newBuilder()
                            .body(modifiedBody.toResponseBody(response.body?.contentType()))
                            .build()
                    } catch (e: Exception) {
                        Log.e("KtorClient", "Invalid JSON in GET response: ${e.message}", e)
                    }
                }

                // Use data for PUT
                if (originalRequest.method == "PUT") {
                    val savedData = sharedPreferences.getString("get_response_data", "") ?: ""

                    try {
                        val removeJsonData = savedData.replace("\"status_code\":\\d+,?", "").trim()

                        val jsonData = JSONObject(removeJsonData)
                        val data = jsonData.optJSONObject("data")

                        val buffer = okio.Buffer()
                        originalRequest.body?.writeTo(buffer)
                        val userProvidedData = buffer.readUtf8()

                        val userData = JSONObject(userProvidedData)
                        val userTitle = userData.optString("title", "")
                        val userBody = userData.optString("body", "")

                        if (userTitle.isNotEmpty()) {
                            data?.put("title", userTitle)
                        }
                        if (userBody.isNotEmpty()) {
                            data?.put("body", userBody)
                        }

                        // Update JSON
                        jsonData.put("data", data)
                        val modifiedBody = jsonData.toString()

                        val mediaType = originalRequest.body?.contentType()?.toString() ?: "application/json"
                        val requestBody = modifiedBody.toRequestBody(mediaType.toMediaTypeOrNull())
                        val newRequest = originalRequest.newBuilder()
                            .put(requestBody)
                            .build()

                        response.close()
                        response = chain.proceed(newRequest)
                    } catch (e: JSONException) {
                        Log.e("KtorClient", "Error JSON: ${e.message}", e)
                    } catch (e: Exception) {
                        Log.e("KtorClient", "Error: ${e.message}", e)
                    }
                }
                response
            }
        }
    }

    suspend fun getMoviesData(id: String): String {
        return client.get("https://dummyjson.com/posts/$id").bodyAsText()
    }

    suspend fun putRequest(
        formData: Map<String, String>
    ): String {
        return try {
            val response: HttpResponse = client.put("https://dummyjson.com/posts/1") {
                contentType(ContentType.Application.FormUrlEncoded)
                setBody(formData.formUrlEncode())
            }

            val responseBody = response.bodyAsText()
//            Log.d("KtorClient", "Response: $responseBody")

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
            ).show()
        }
        Log.d("KtorClient", "API Retrying... $attempt Times")
    }
}

