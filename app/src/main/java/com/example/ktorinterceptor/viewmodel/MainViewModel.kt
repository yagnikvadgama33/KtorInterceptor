package com.example.ktorinterceptor.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ktorinterceptor.interceptor.KtorClient
import com.example.ktorinterceptor.utils.NoInternetException
import io.ktor.client.call.body
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MainViewModel(private val ktorClient: KtorClient) : ViewModel() {

    private val _response = MutableStateFlow("")
    val response: StateFlow<String> = _response

    fun fetchData() {
        viewModelScope.launch {
            try {
                val result = ktorClient.getMoviesData().body<String>()
                _response.value = result
                Log.w("KtorClient", "VM: Decrypted Data: $result")
            } catch (e: NoInternetException) {
                Log.e("KtorClient", "No Internet: ${e.message}")
                _response.value = "No Internet: ${e.message}"
            } catch (e: Exception) {
                Log.e("KtorClient", "VM: Error -> ${e.message}")
                _response.value = "Error: ${e.message}"
            }
        }
    }

    fun makePostRequest() {
        val formData = mapOf(
            "title" to "John Abram",
            "body" to "description..."
        )

        viewModelScope.launch {
            try {
                val result = ktorClient.postRequest(
                    url = "https://dummyjson.com/posts/1",
                    formData = formData
                )
                Log.d("KtorClient", "POST Response: $result")
            } catch (e: Exception) {
                Log.e("KtorClient", "POST Error: ${e.message}")
            }
        }
    }
}
