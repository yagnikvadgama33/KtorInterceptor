package com.example.ktorinterceptor.screen

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.example.ktorinterceptor.R
import com.example.ktorinterceptor.interceptor.KtorClient
import com.example.ktorinterceptor.ui.theme.KtorInterceptorTheme
import com.example.ktorinterceptor.utils.ConnectionState
import com.example.ktorinterceptor.utils.connectivityState
import com.example.ktorinterceptor.viewmodel.MainViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val viewModel = MainViewModel(KtorClient(this))

        enableEdgeToEdge()

        setContent {
            KtorInterceptorTheme {
                val snackbarHostState = remember { SnackbarHostState() }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    snackbarHost = { SnackbarHost(snackbarHostState) }
                ) { innerPadding ->
                    ConnectivityStatus(snackbarHostState)
                    MainUI(innerPadding, viewModel = viewModel)
                }
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Composable
    fun ConnectivityStatus(snackbarHostState: SnackbarHostState) {
        var isFirstTime = true
        val connection by connectivityState()
        val isConnected = connection === ConnectionState.Available

        LaunchedEffect(isConnected) {
            when {
                isConnected && !isFirstTime -> {
                    snackbarHostState.showSnackbar(
                        message = getString(R.string.internet_connected),
                        duration = SnackbarDuration.Short
                    )
                    isFirstTime = false
                }

                !isConnected -> {
                    snackbarHostState.showSnackbar(
                        message = getString(R.string.please_connect_to_internet),
                        duration = SnackbarDuration.Indefinite
                    )
                }
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Composable
    fun MainUI(
        innerPadding: PaddingValues,
        viewModel: MainViewModel
    ) {
        val connection by connectivityState()
        var name by remember { mutableStateOf(TextFieldValue("")) }
        var body by remember { mutableStateOf(TextFieldValue("")) }
        var id by remember { mutableStateOf(TextFieldValue("")) }

        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(horizontal = 12.dp)
        ) {
            //GET
            Text(
                text = stringResource(R.string.get_response),
                style = MaterialTheme.typography.headlineMedium
            )

            TextField(
                value = id,
                onValueChange = { newText ->
                    id = newText
                },
                placeholder = {
                    Text(stringResource(R.string.enter_post_id))
                },
                maxLines = 1,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
            )

            Button(
                onClick = {
                    if (id.text.isBlank()) {
                        Toast.makeText(
                            this@MainActivity,
                            getString(R.string.enter_post_id),
                            Toast.LENGTH_LONG
                        ).show()
                    } else {
                        viewModel.fetchData(id.text.trim())
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                enabled = connection == ConnectionState.Available
            ) {
                Text(text = stringResource(R.string.fetch_data_get))
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
            Text(
                text = stringResource(R.string.post_response),
                style = MaterialTheme.typography.headlineMedium
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.padding(vertical = 12.dp)
            ) {
                TextField(
                    value = name,
                    onValueChange = { newText ->
                        name = newText
                    },
                    placeholder = {
                        Text(stringResource(R.string.title))
                    },
                    maxLines = 1,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.5f)
                        .padding(end = 3.dp)
                )

                TextField(
                    value = body,
                    onValueChange = { newText ->
                        body = newText
                    },
                    maxLines = 1,
                    placeholder = {
                        Text(stringResource(R.string.body))
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.5f)
                        .padding(start = 3.dp)
                )
            }
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = {
                    if (name.text.isBlank() && body.text.isBlank()) {
                        Toast.makeText(
                            this@MainActivity,
                            getString(R.string.enter_title_or_description),
                            Toast.LENGTH_LONG
                        ).show()
                    } else {
                        viewModel.makePostRequest(
                            name = name.text.trim(),
                            body = body.text.trim()
                        )
                    }
                },
                modifier = Modifier
                    .padding(horizontal = 12.dp)
                    .fillMaxWidth(),
                enabled = connection == ConnectionState.Available
            ) {
                Text(text = stringResource(R.string.send_data_post))
            }

            Spacer(Modifier.height(16.dp))

            Text(
                text = viewModel.postResponse.collectAsState().value,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

