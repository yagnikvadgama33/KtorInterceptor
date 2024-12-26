package com.example.ktorinterceptor.interceptor

import android.content.Context
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkInfo
import android.os.Build
import android.util.Log
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
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
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

                var response = chain.proceed(originalRequest)

                Log.d(
                    "KtorClient",
                    "Type - ${originalRequest.method} && isSuccessful - ${response.code}"
                )

                if (originalRequest.method == "GET" && response.isSuccessful) {
                    val responseBody = response.body?.string() ?: ""
                    try {
                        Log.d("KtorClient", "Before Modification: $responseBody")


                        JSONObject(responseBody)
                        val modifiedBody =
                            """{"status_code": ${response.code},"data": $responseBody}""".trimIndent()

                        sharedPreferences.edit().putString("get_response_data", modifiedBody)
                            .apply()

                        response = response.newBuilder()
                            .body(modifiedBody.toResponseBody(response.body?.contentType()))
                            .build()

                        Log.d("KtorClient", "After Modification Response: $modifiedBody")
                    } catch (e: Exception) {
                        Log.e("KtorClient", "Invalid JSON in GET response: ${e.message}", e)
                    }
                }

                if (originalRequest.method == "PUT") {
                    val savedData = sharedPreferences.getString("get_response_data", "") ?: ""
                    try {
                        Log.d("KtorClient", "Before Modification: $savedData")

                        /*
                        val removeJsonData = savedData.replace("\"status_code\":\\d+,?", "").trim()
                        val jsonData = JSONObject(removeJsonData)
                        val data = jsonData.optJSONObject("data")

                        val buffer = okio.Buffer()
                        originalRequest.body?.writeTo(buffer)
                        val userProvidedData = buffer.readUtf8()

                        Log.d("KtorClient", "{$userProvidedData}".replaceSpecialChar())

                        val userData = JSONObject("{$userProvidedData}".replaceSpecialChar())
                        val userTitle = userData.getString("title")
                        val userBody = userData.getString("body")

                        Log.d("KtorClient", "title = $userTitle")
                        Log.d("KtorClient", "body = $userBody")
                        if (userTitle.isNotEmpty()) {
                            data?.put("title", userTitle)
                        }
                        if (userBody.isNotEmpty()) {
                            data?.put("body", userBody)
                        }

                        jsonData.put("data", data)
                        val modifiedBody = jsonData.toString()

                        Log.d("KtorClient", "modifiedBody Response: $modifiedBody")*/

                        val buffer = okio.Buffer()
                        originalRequest.body?.writeTo(buffer)
                        val userProvidedData = buffer.readUtf8()

                        val mediaType =
                            originalRequest.body?.contentType()?.toString() ?: "application/json"
                        val requestBody =
                            userProvidedData.toRequestBody(mediaType.toMediaTypeOrNull())
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

    suspend fun getPostData(onResponse: (String) -> Unit) {
        if (isNetworkAvailable()) {
            try {
                val id = sharedPreferences.getString("set_id", "") ?: ""
                val response = client.get("https://dummyjson.com/posts/$id").bodyAsText()
                onResponse(response)
            } catch (e: Exception) {
                e.printStackTrace()
                onResponse("Network Error: ${e.message}")
            }
        } else {
            if (!failedRequestQueue.contains("GET")) {
                failedRequestQueue.add("GET")
            }
            onResponse(context.getString(R.string.no_internet_msg))
        }
    }

    suspend fun putPostRequest(
        onResponse: (String) -> Unit
    ) {
        if (isNetworkAvailable()) {
            try {
                val title = sharedPreferences.getString("set_title", "") ?: ""
                val body = sharedPreferences.getString("set_body", "") ?: ""

                Log.d("KtorClient", "putPostRequest Param: $title - $body")

                val m = mapOf("title" to title, "body" to body)

                val response: HttpResponse = client.put("https://dummyjson.com/posts/1") {
                    contentType(ContentType.Application.FormUrlEncoded)
                    setBody(m.formUrlEncode())
                }

                val responseBody = response.bodyAsText()
                CoroutineScope(Dispatchers.Main).launch {
                    onResponse(responseBody)
                }

            } catch (e: Exception) {
                e.printStackTrace()
                onResponse("Network Error: ${e.message}")
            }
        } else {
            if (!failedRequestQueue.contains("PUT")) {
                failedRequestQueue.add("PUT")
            }
            onResponse(context.getString(R.string.no_internet_msg))
        }
    }

    private val coroutineExceptionHandler = CoroutineExceptionHandler { _, throwable ->
        throwable.printStackTrace()
    }

    fun retryFailedRequests(
        id: String? = null,
        title: String? = null,
        body: String? = null,
        onResponse: (String) -> Unit
    ) {
        if (isNetworkAvailable()) {
            while (failedRequestQueue.isNotEmpty()) {
                val method = failedRequestQueue.poll()
                Log.d("KtorClient", "Retrying failed request: $method")

                CoroutineScope(Dispatchers.IO + coroutineExceptionHandler).launch {
                    when (method) {
                        "GET" -> {
                            getPostData(onResponse)
                        }

                        "PUT" -> {
                            Log.d("KtorClient", "Inside PUT: $title - $body")
                            putPostRequest(
                                onResponse
                            )
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

    fun registerNetworkCallback(
        id: String?,
        title: String?,
        body: String?,
        onResponse: (String) -> Unit
    ) {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                super.onAvailable(network)
                Log.d("KtorClient", "onAvailable Param: $title - $body")
                retryFailedRequests(id, title, body, onResponse)
            }

            override fun onLost(network: Network) {
                super.onLost(network)
                Log.d("KtorClient", "onLost Param: $title - $body")
            }
        }
        connectivityManager.registerDefaultNetworkCallback(networkCallback)
    }
}