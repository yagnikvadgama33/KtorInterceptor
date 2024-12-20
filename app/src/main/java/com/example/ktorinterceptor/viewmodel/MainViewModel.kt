package com.example.ktorinterceptor.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ktorinterceptor.interceptor.KtorClient
import com.example.ktorinterceptor.utils.NoInternetException
import kotlinx.coroutines.launch

class MainViewModel(private val ktorClient: KtorClient) : ViewModel() {
    fun fetchData() {
        viewModelScope.launch {
            try {
                val data = ktorClient.getMoviesData()
                Log.w("KtorClient", "VM: Decrypted Data: $data")
            } catch (e: NoInternetException) {
                Log.e("KtorClient", "No Internet${e.message}")
            } catch (e: Exception) {
                Log.e("KtorClient", "VM: Error -> ${e.message}")
            }
        }
    }

    fun makePostRequest() {

        val formData = mapOf(
            "title" to "John Abram",
            "body" to "description..."
        )

        viewModelScope.launch {
            val response = ktorClient.postRequest(
                url = "https://dummyjson.com/posts/1",
                formData = formData
            )
            println("POST Response: ${response}")
        }
    }
}