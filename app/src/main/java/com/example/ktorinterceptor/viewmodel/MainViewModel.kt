package com.example.ktorinterceptor.viewmodel

import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ktorinterceptor.interceptor.KtorClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MainViewModel(private val ktorClient: KtorClient) : ViewModel()
{

    private val _getResponse = MutableStateFlow("")
    val getResponse: StateFlow<String> = _getResponse

    private val _postResponse = MutableStateFlow("")
    val postResponse: StateFlow<String> = _postResponse

    var vmId = mutableStateOf<String?>(null)
    var vmName = mutableStateOf<String?>(null)
    var vmBody = mutableStateOf<String?>(null)

     val vmTitle = MutableLiveData("")

     val vmB = MutableLiveData("")

    var isGetApiCalled = mutableStateOf(false)
    var isPostApiCalled = mutableStateOf(false)

    fun fetchData() {
        viewModelScope.launch {
            try {
                ktorClient.getPostData {
                    Log.d("KtorClient", "After Modification : $it")
                    _getResponse.value = it
                }
            } catch (e: Exception) {
                Log.e("KtorClient", "VM: Error -> ${e.message}")
                _getResponse.value = "Error: ${e.message}"
            }
        }
    }

    fun makePostRequest() {
        viewModelScope.launch {
            try {
                ktorClient.putPostRequest {
                    Log.d("KtorClient", "After Modification : $it")
                    _postResponse.value = it
                }
            } catch (e: Exception) {
                Log.e("KtorClient", "VM: Error -> ${e.message}")
                _postResponse.value = "Error: ${e.message}"
            }
        }
    }

    fun retryApiLater() {
        ktorClient.registerNetworkCallback(
            id = vmId.value,
            title = vmTitle.value,
            body = vmB.value
        ) {
            Log.d("KtorClient","VM Param: ${vmTitle.value} - ${vmB.value}")
            if (isGetApiCalled.value) {
                _getResponse.value = it
                isGetApiCalled.value = false
            }
            if (isPostApiCalled.value) {
                _postResponse.value = it
                isPostApiCalled.value = false
            }
        }
    }
}
