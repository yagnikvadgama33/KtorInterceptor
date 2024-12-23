package com.example.ktorinterceptor.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ktorinterceptor.interceptor.KtorClient
import com.example.ktorinterceptor.utils.EncryptionUtils
import com.example.ktorinterceptor.utils.EncryptionUtils.PREF_NAME
import com.example.ktorinterceptor.utils.EncryptionUtils.SECRET_KEY
import io.ktor.client.call.body
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MainViewModel(private val ktorClient: KtorClient) :
    ViewModel() {

    private val _getResponse = MutableStateFlow("")
    val getResponse: StateFlow<String> = _getResponse

    private val _postResponse = MutableStateFlow("")
    val postResponse: StateFlow<String> = _postResponse

    fun fetchData() {
        viewModelScope.launch {
            try {
                val result = ktorClient.getMoviesData().body<String>()
                _getResponse.value = result
                Log.w("KtorClient", "VM: Decrypted Data: $result")
            } catch (e: Exception) {
                Log.e("KtorClient", "VM: Error -> ${e.message}")
                _getResponse.value = "Error: ${e.message}"
            }
        }
    }

    fun makePostRequest(name: String, body: String) {
        viewModelScope.launch {
            try {
                val result = ktorClient.postRequest(
                    formData = mapOf("title" to name, "body" to body)
                )
                _postResponse.value = result

                val decryptedResponseBody = result.let { body ->
                    val bodyString = body
                    val decrypted = EncryptionUtils.decrypt(bodyString, "")
                    decrypted
                }

                Log.w("KtorClient", "VM: Decrypted Data: $decryptedResponseBody")
            } catch (e: Exception) {
                Log.e("KtorClient", "VM: Error -> ${e.message}")
                _postResponse.value = "Error: ${e.message}"
            }
        }
    }
}