package com.example.ktorinterceptor.screen

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.ktorinterceptor.interceptor.KtorClient
import com.example.ktorinterceptor.ui.theme.KtorInterceptorTheme
import com.example.ktorinterceptor.utils.EncryptionUtils
import com.example.ktorinterceptor.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val secretKey = EncryptionUtils.generateSecretKey(applicationContext)
        val viewModel = MainViewModel(KtorClient(this, secretKey))

        enableEdgeToEdge()
        setContent {
            KtorInterceptorTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MainUI(innerPadding, viewModel)
                }
            }
        }
    }
}

@Composable
fun MainUI(innerPadding: PaddingValues, viewModel: MainViewModel) {
    Column(modifier = Modifier.padding(innerPadding)) {
        Text(
            text = "Jetpack Compose MVVM App",
            style = MaterialTheme.typography.headlineMedium
        )

        Button(onClick = { viewModel.fetchData() }) {
            Text(text = "Fetch Data (GET)")
        }

        Button(onClick = { viewModel.makePostRequest() }) {
            Text(text = "Send Data (POST)")
        }

        Text(text = "Response: ${viewModel.response.collectAsState().value}")
    }
}