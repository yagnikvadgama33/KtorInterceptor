package com.example.ktorinterceptor.screen

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
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

    @Composable
    fun MainUI(innerPadding: PaddingValues, viewModel: MainViewModel) {
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(horizontal = 12.dp)
        ) {

            //GET
            Row (
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(onClick = { viewModel.fetchData() }, modifier = Modifier.padding(end = 12.dp)) {
                    Text(text = "Fetch Data (GET)")
                }
                Text(text = "GET Response:", style = MaterialTheme.typography.headlineMedium)
            }
            Spacer(Modifier.height(12.dp))

            Text(
                text = viewModel.getResponse.collectAsState().value,
                style = MaterialTheme.typography.bodyLarge
            )

            HorizontalDivider(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 18.dp)
            )

            //POST
            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(
                    onClick = { viewModel.makePostRequest() },
                    modifier = Modifier.padding(end = 12.dp)
                ) {
                    Text(text = "Send Data (POST)")
                }

                Text(text = "POST Response:", style = MaterialTheme.typography.headlineMedium)
            }
            Spacer(Modifier.height(12.dp))

            Text(
                text = viewModel.postResponse.collectAsState().value,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

