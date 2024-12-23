package com.example.ktorinterceptor.screen

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.example.ktorinterceptor.interceptor.KtorClient
import com.example.ktorinterceptor.ui.theme.KtorInterceptorTheme
import com.example.ktorinterceptor.utils.EncryptionUtils
import com.example.ktorinterceptor.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {

    var isForPost = false

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

    @Composable
    fun MainUI(innerPadding: PaddingValues, viewModel: MainViewModel) {
        Column(modifier = Modifier.padding(innerPadding)) {

            //GET
            Button(onClick = { viewModel.fetchData() }) {
                Text(text = "Fetch Data (GET)")
            }
            Spacer(modifier = Modifier.height(16.dp))

            Text(text = "GET Response:", style = MaterialTheme.typography.headlineMedium)
            Text(text = viewModel.getResponse.collectAsState().value)

            HorizontalDivider(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 18.dp)
            )

            //POST
            Button(onClick = { viewModel.makePostRequest() }) {
                Text(text = "Send Data (POST)")
            }

            Text(text = "POST Response:", style = MaterialTheme.typography.headlineMedium)
            Text(text = viewModel.postResponse.collectAsState().value)
        }
    }
}

