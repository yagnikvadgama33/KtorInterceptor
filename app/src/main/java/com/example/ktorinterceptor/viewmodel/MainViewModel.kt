package com.example.ktorinterceptor.viewmodel

import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ktorinterceptor.interceptor.KtorClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MainViewModel(private val ktorClient: KtorClient) :
    ViewModel() {

    private val _getResponse = MutableStateFlow("")
    val getResponse: StateFlow<String> = _getResponse

    private val _postResponse = MutableStateFlow("")
    val postResponse: StateFlow<String> = _postResponse

    var id = mutableStateOf<String?>(null)
    var name = mutableStateOf<String?>(null)
    var body = mutableStateOf<String?>(null)

    var isGetApiCalled = mutableStateOf(false)
    var isPostApiCalled = mutableStateOf(false)

    fun fetchData(id: String?) {
        viewModelScope.launch {
            try {
                val result = ktorClient.getMoviesData(id!!, {
                    _getResponse.value = it
                })
//                _getResponse.value = result
                Log.w("KtorClient", "GET Response: $result")
            } catch (e: Exception) {
                Log.e("KtorClient", "VM: Error -> ${e.message}")
                _getResponse.value = "Error: ${e.message}"
            }
        }
    }

    fun makePostRequest(name: String?, body: String?) {
        viewModelScope.launch {
            try {
                val result =
                    ktorClient.putRequest(formData = mapOf("title" to name!!, "body" to body!!), {
                        _postResponse.value = it
                    })
//                _postResponse.value = result
                Log.w("KtorClient", "POST Response: $result")
            } catch (e: Exception) {
                Log.e("KtorClient", "VM: Error -> ${e.message}")
                _postResponse.value = "Error: ${e.message}"
            }
        }
    }

    fun retryApiLater() {
        ktorClient.registerNetworkCallback(
            id = id.value,
            title = name.value,
            body = body.value, {

                if (isGetApiCalled.value) {
                    _getResponse.value = it
                    isGetApiCalled.value = false
                }
                if (isPostApiCalled.value) {
                    _postResponse.value = it
                    isPostApiCalled.value = false
                }
            }
        )
    }
}