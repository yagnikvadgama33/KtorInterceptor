package com.example.ktorinterceptor.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ktorinterceptor.Repository
import io.ktor.util.Identity.decode
import kotlinx.coroutines.launch
import okhttp3.RequestBody.Companion.toRequestBody

class KtorViewModel : ViewModel() {
    private val repository = Repository()
    var apiResponse = ""
        private set

    fun performGetApiCall() {
        viewModelScope.launch {
            repository.fetchData(
                onSuccess = { response ->
                    apiResponse = response
                    Log.d("MainViewModel", "GET API Response: ${apiResponse}")
                },
                onError = { error -> apiResponse = error }
            )
        }
    }

    fun performPostApiCall() {
        viewModelScope.launch {
            repository.postData(
                data = "{\"title\":\"abc123\"}",
                onSuccess = { response ->
                    apiResponse = response
                    Log.d("MainViewModel", "POST API Response: ${response.toRequestBody()}")
                },
                onError = { error -> apiResponse = error }
            )
        }
    }
}