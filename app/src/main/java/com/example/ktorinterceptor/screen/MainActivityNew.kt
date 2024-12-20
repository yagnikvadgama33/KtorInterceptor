package com.example.ktorinterceptor.screen

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.ktorinterceptor.viewmodel.KtorViewModel

class MainActivityNew : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyApp()
        }
    }
}

@Composable
fun MyApp() {
    val viewModel = KtorViewModel()

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "Jetpack Compose MVVM App", style = MaterialTheme.typography.headlineMedium)

            Button(onClick = { viewModel.performGetApiCall() }) {
                Text(text = "Fetch Data (GET)")
            }

            Button(onClick = { viewModel.performPostApiCall() }) {
                Text(text = "Send POST Request")
            }
//            if (viewModel.apiResponse.isNotEmpty()) {
            Text(text = viewModel.apiResponse, modifier = Modifier.padding(top = 16.dp))
//            }
        }
    }
}
