package com.example.ktorinterceptor.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Base64
import okhttp3.MediaType
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody

fun isInternetAvailable(context: Context): Boolean {
    val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val network = connectivityManager.activeNetwork
    val capabilities = connectivityManager.getNetworkCapabilities(network)
    return capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
}

//New
fun String.toRequestBody(contentType: MediaType?): RequestBody =
    this.toRequestBody(contentType)

fun String.toResponseBody(contentType: MediaType?): ResponseBody =
    ResponseBody.create(contentType, this)

// Extensions for Base64 encoding/decoding
fun ByteArray.toBase64(): String = Base64.encodeToString(this, Base64.DEFAULT)

fun RequestBody.stringify(): String {
    val buffer = okio.Buffer()
    writeTo(buffer)
    return buffer.readUtf8()
}