package com.example.ktorinterceptor.interceptor

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkInfo
import android.os.Build
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
import java.util.LinkedList

class KtorClient(private val context: Context) {

    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("api_prefs", Context.MODE_PRIVATE)
    private val failedRequestQueue = LinkedList<String>()

    private val client = HttpClient(OkHttp) {
        install(Logging) {
            level = LogLevel.ALL
        }

        engine {
            addInterceptor { chain ->
                val originalRequest = chain.request()

                // Retry logic for failed requests
                var attempt = 0
                var response = chain.proceed(originalRequest)
                val maxRetries = 3

                Log.d("KtorClient", "Type - ${originalRequest.method} && ${response.isSuccessful}")


                // Retry on network errors (status codes 500-503 or no internet)
                while (attempt < maxRetries && response.code in 201..503) {
                    runBlocking {
                        attempt++
                        notifyRetry(attempt)
                        delay(3000)
                        response.close()
                        response = chain.proceed(originalRequest)
                    }
                }

                // If no internet, save the request in a queue
//                if (response.code == 0) {
//                    Log.d("KtorClient", "No internet saving request for retry.")
//                    if (!failedRequestQueue.contains(originalRequest.method)) {
//                        failedRequestQueue.add(originalRequest.method)
//                    }
//                    Log.d("KtorClient", "failedRequestQueue: ${failedRequestQueue.poll()}")
//                }

                // Handle GET response
                if (originalRequest.method == "GET" && response.isSuccessful) {
                    val responseBody = response.body?.string() ?: ""
                    try {
                        JSONObject(responseBody)
                        val modifiedBody = """{"status_code": ${response.code},"data": $responseBody}""".trimIndent()

                        sharedPreferences.edit().putString("get_response_data", modifiedBody)
                            .apply()

                        response = response.newBuilder()
                            .body(modifiedBody.toResponseBody(response.body?.contentType()))
                            .build()

                        Log.d("KtorClient", "GET Response: $modifiedBody")
                    } catch (e: Exception) {
                        Log.e("KtorClient", "Invalid JSON in GET response: ${e.message}", e)
                    }
                }

                // Handle PUT request
                if (originalRequest.method == "PUT") {
                    val savedData = sharedPreferences.getString("get_response_data", "") ?: ""
                    try {

                        Log.d("KtorClient", "PUT Response Before Modification: $savedData")

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

                        val mediaType =
                            originalRequest.body?.contentType()?.toString() ?: "application/json"
                        val requestBody = modifiedBody.toRequestBody(mediaType.toMediaTypeOrNull())
                        val newRequest = originalRequest.newBuilder()
                            .put(requestBody)
                            .build()

                        Log.d("KtorClient", "PUT Response After Modify: $modifiedBody")

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
        return if (isNetworkAvailable()) {
            try {
                client.get("https://dummyjson.com/posts/$id").bodyAsText()
            } catch (e: Exception) {
                e.printStackTrace()
                "Network Error: ${e.message}"
            }
        } else {
            if (!failedRequestQueue.contains("GET")) {
                failedRequestQueue.add("GET")
            }
            "No internet connection available"
        }
    }

    suspend fun putRequest(
        formData: Map<String, String>
    ): String {
        return if (isNetworkAvailable()){
            try {
                val response: HttpResponse = client.put("https://dummyjson.com/posts/1") {
                    contentType(ContentType.Application.FormUrlEncoded)
                    setBody(formData.formUrlEncode())
                }

                val responseBody = response.bodyAsText()
                responseBody
            } catch (e: Exception) {
                e.printStackTrace()
                "Network Error: ${e.message}"
            }
        }else{
            if (!failedRequestQueue.contains("PUT")) {
                failedRequestQueue.add("PUT")
            }
            "No internet connection available"
        }

    }

    fun retryFailedRequests(
        id: String? = null,
        title: String? = null,
        body: String? = null
    ) {
        if (isNetworkAvailable()) {
            while (failedRequestQueue.isNotEmpty()) {
                val method = failedRequestQueue.poll()
                Log.d("KtorClient", "Retrying failed request: $method")

                CoroutineScope(Dispatchers.IO).launch {
                    when (method) {
                        "GET" -> {
                            getMoviesData("1")
                        }

                        "PUT" -> {
                            putRequest(mapOf("title" to "John Smith", "body" to "Hello 123 this is description"))
                        }
                    }
                }
            }
        } else {
            Log.d("KtorClient", "No network available, cannot retry requests.")
        }
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val networkCapabilities = connectivityManager.activeNetwork
            val activeNetwork = connectivityManager.getNetworkCapabilities(networkCapabilities)
            activeNetwork != null && activeNetwork.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } else {
            val networkInfo: NetworkInfo? = connectivityManager.activeNetworkInfo
            networkInfo != null && networkInfo.isConnected
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

    fun registerNetworkCallback(
        id: String?,
        title: String?,
        body: String?
    ) {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                super.onAvailable(network)
                retryFailedRequests(id, title, body)
            }
        }
        connectivityManager.registerDefaultNetworkCallback(networkCallback)
    }
}

/*class KtorClient(private val context: Context) {

    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("api_prefs", Context.MODE_PRIVATE)
    private val failedRequestQueue = LinkedList<String>()

    private val client = HttpClient(OkHttp) {
        install(Logging) {
            level = LogLevel.ALL
        }

        engine {
            addInterceptor { chain ->
                val originalRequest = chain.request()

                // Retry logic for failed requests
                var attempt = 0
                var response = chain.proceed(originalRequest)
                val maxRetries = 3

                // Retry on network errors (status codes 500-503 or no internet)
                while (attempt < maxRetries && response.code in 201..503) {
                    attempt++
                    notifyRetry(attempt)
                    runBlocking { delay(3000L) }
                    response.close()
                    response = chain.proceed(originalRequest)
                }

                // If no internet, save the request in a queue
                if (response.code == 0) {
                    Log.d("KtorClient", "No internet saving request for retry.")
                    if (!failedRequestQueue.contains(originalRequest.method)) {
                        failedRequestQueue.add(originalRequest.method)
                    }
                    Log.d("KtorClient", "failedRequestQueue: ${failedRequestQueue.poll()}")
                }

                // Handle GET response
                if (originalRequest.method == "GET" && response.isSuccessful) {
                    val responseBody = response.body?.string() ?: ""

                    try {
                        JSONObject(responseBody)
                        val modifiedBody =
                            """{"status_code": ${response.code},"data": $responseBody}""".trimIndent()

                        sharedPreferences.edit().putString("get_response_data", modifiedBody)
                            .apply()

                        response = response.newBuilder()
                            .body(modifiedBody.toResponseBody(response.body?.contentType()))
                            .build()
                    }
                    catch (e: Exception) {
                        Log.e("KtorClient", "Invalid JSON in GET response: ${e.message}", e)
                    }
                }

                // Handle PUT request
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

                        val mediaType =
                            originalRequest.body?.contentType()?.toString() ?: "application/json"
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
            responseBody
        } catch (e: Exception) {
            e.printStackTrace()
            "Network Error: ${e.message}"
        }
    }

    fun retryFailedRequests(id: String? = null, title: String? = null, body: String? = null) {
        if (isNetworkAvailable()) {
            while (failedRequestQueue.isNotEmpty()) {
                val method = failedRequestQueue.poll()
                Log.d("KtorClient", "Retrying failed request: $method")

                CoroutineScope(Dispatchers.IO).launch {
                    when(method) {
                        "GET" -> {
                            getMoviesData(id!!)
                        }
                        "PUT" -> {
                            putRequest(mapOf("title" to title!!, "body" to body!!))
                        }
                    }
                }
            }
        } else {
            Log.d("KtorClient", "No network available, cannot retry requests.")
        }
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val networkCapabilities = connectivityManager.activeNetwork
            val activeNetwork = connectivityManager.getNetworkCapabilities(networkCapabilities)
            activeNetwork != null && activeNetwork.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } else {
            val networkInfo: NetworkInfo? = connectivityManager.activeNetworkInfo
            networkInfo != null && networkInfo.isConnected
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
}*/



