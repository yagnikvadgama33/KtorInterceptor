package com.example.ktorinterceptor

import android.util.Log
import com.example.ktorinterceptor.interceptor.ApiInterceptor
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException

class Repository {
    private val client: OkHttpClient

    init {
        val interceptor = ApiInterceptor()
        client = OkHttpClient.Builder()
            .addInterceptor(interceptor)
            .build()
    }

    fun fetchData(onSuccess: (String) -> Unit, onError: (String) -> Unit) {
        val request = Request.Builder().url("https://dummyapi.online/api/movies").build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                onError("Network error: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    onSuccess(response.body?.string() ?: "Empty response")
                    Log.w("Response: ","${response.body?.toString()}")
                } else {
                    onError("Error: ${response.code}")
                }
            }
        })
    }

    fun postData(data: String, onSuccess: (String) -> Unit, onError: (String) -> Unit) {
        val requestBody = data.toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url("https://new-api-url.com/object")
            .post(requestBody)
            .build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                onError("Network error: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    onSuccess(response.body?.string() ?: "Empty response")
                } else {
                    onError("Error: ${response.code}")
                }
            }
        })
    }
}