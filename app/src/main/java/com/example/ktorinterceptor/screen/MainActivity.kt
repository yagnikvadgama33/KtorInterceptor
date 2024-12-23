package com.example.ktorinterceptor.screen

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
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
        val connection by connectivityState()
        val isConnected = connection === ConnectionState.Available

        LaunchedEffect(isConnected) {
            val message = if (isConnected) {
                getString(R.string.internet_connected)
            } else {
                getString(R.string.please_connect_to_internet)
            }
            snackbarHostState.showSnackbar(
                message = message,
                duration = if (isConnected) SnackbarDuration.Short else SnackbarDuration.Indefinite
            )
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Composable
    fun MainUI(
        innerPadding: PaddingValues,
        viewModel: MainViewModel
    ) {
        val connection by connectivityState()
        val isEnabled = connection == ConnectionState.Available

        var id by remember { mutableStateOf(TextFieldValue("")) }
        var name by remember { mutableStateOf(TextFieldValue("")) }
        var body by remember { mutableStateOf(TextFieldValue("")) }
        val keyboardController = LocalSoftwareKeyboardController.current

        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(horizontal = 12.dp)
        ) {

            SectionHeader(title = stringResource(R.string.get_response))

            Spacer(Modifier.height(12.dp))

            CustomTextField(
                value = id,
                onValueChange = { id = it },
                placeholder = stringResource(R.string.enter_post_id),
                keyboardType = KeyboardType.Number,
                isEnabled = isEnabled,
                keyboardController = keyboardController
            )
            Spacer(Modifier.height(12.dp))

            CustomButton(
                text = stringResource(R.string.fetch_data_get),
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
                isEnabled = isEnabled
            )

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

            SectionHeader(title = stringResource(R.string.post_response))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.padding(vertical = 12.dp)
            ) {
                CustomTextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = stringResource(R.string.title),
                    isEnabled = isEnabled,
                    modifier = Modifier
                        .weight(0.5f)
                        .padding(end = 3.dp),
                    keyboardController = keyboardController
                )

                CustomTextField(
                    value = body,
                    onValueChange = { body = it },
                    placeholder = stringResource(R.string.body),
                    isEnabled = isEnabled,
                    modifier = Modifier
                        .weight(0.5f)
                        .padding(start = 3.dp),
                    keyboardController = keyboardController
                )
            }

            CustomButton(
                text = stringResource(R.string.send_data_post),
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
                isEnabled = isEnabled
            )

            Spacer(Modifier.height(16.dp))

            Text(
                text = viewModel.postResponse.collectAsState().value,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }

    @Composable
    fun SectionHeader(title: String) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium
        )
    }

    @Composable
    fun CustomTextField(
        value: TextFieldValue,
        onValueChange: (TextFieldValue) -> Unit,
        placeholder: String,
        isEnabled: Boolean,
        modifier: Modifier = Modifier,
        keyboardType: KeyboardType = KeyboardType.Text,
        keyboardController: SoftwareKeyboardController?
    ) {
        TextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder) },
            maxLines = 1,
            modifier = modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions.Default.copy(
                keyboardType = keyboardType,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = { keyboardController?.hide() }
            ),
            enabled = isEnabled
        )
    }

    @Composable
    fun CustomButton(
        text: String,
        onClick: () -> Unit,
        isEnabled: Boolean,
        modifier: Modifier = Modifier
            .padding(horizontal = 12.dp)
            .fillMaxWidth()
    ) {
        Button(
            onClick = onClick,
            modifier = modifier,
            enabled = isEnabled
        ) {
            Text(text = text)
        }
    }
}


