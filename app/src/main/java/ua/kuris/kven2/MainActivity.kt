package ua.kuris.kven2

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import ua.kuris.kven2.ui.theme.KvenIITheme

private data class ChatLine(
    val speaker: String,
    val text: String,
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            KvenIITheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    KvenScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
private fun KvenScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val preferences = remember {
        context.getSharedPreferences("kven-auth", android.content.Context.MODE_PRIVATE)
    }
    var username by remember {
        mutableStateOf(preferences.getString("username", "").orEmpty())
    }
    var password by remember {
        mutableStateOf(preferences.getString("password", "").orEmpty())
    }
    var credentialsSaved by remember {
        mutableStateOf(username.isNotBlank() && password.isNotBlank())
    }
    var editingCredentials by remember {
        mutableStateOf(!credentialsSaved)
    }
    val client = remember(username, password) {
        KvenClient(
            baseUrl = BuildConfig.KVEN_BASE_URL,
            username = username,
            password = password,
        )
    }
    val mainHandler = remember { Handler(Looper.getMainLooper()) }
    val messages = remember { mutableStateListOf<ChatLine>() }
    var input by remember { mutableStateOf("") }
    var isSending by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var failedMessage by remember { mutableStateOf<String?>(null) }

    fun transmit(text: String, appendUser: Boolean) {
        val trimmed = text.trim()
        if (trimmed.isEmpty() || isSending) {
            return
        }
        if (appendUser) {
            messages += ChatLine("Вы", trimmed)
            input = ""
        }
        isSending = true
        error = null
        failedMessage = null

        Thread {
            val result = runCatching { client.sendMessage(trimmed) }
            mainHandler.post {
                isSending = false
                result.onSuccess { reply ->
                    messages += ChatLine("Квен", reply)
                }.onFailure { failure ->
                    failedMessage = trimmed
                    error = failure.message ?: failure.javaClass.simpleName
                }
            }
        }.start()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = "Kven II",
            style = MaterialTheme.typography.headlineSmall,
        )

        if (editingCredentials || !credentialsSaved) {
            OutlinedTextField(
                value = username,
                onValueChange = {
                    username = it
                    credentialsSaved = false
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSending,
                label = { Text("Логин") },
                singleLine = true,
            )

            OutlinedTextField(
                value = password,
                onValueChange = {
                    password = it
                    credentialsSaved = false
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSending,
                label = { Text("Пароль") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                Button(
                    onClick = {
                        preferences.edit()
                            .putString("username", username)
                            .putString("password", password)
                            .apply()
                        credentialsSaved = true
                        editingCredentials = false
                        error = null
                    },
                    enabled = !isSending && username.isNotBlank() && password.isNotBlank(),
                ) {
                    Text("Сохранить")
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = username,
                    style = MaterialTheme.typography.labelLarge,
                )
                TextButton(
                    onClick = { editingCredentials = true },
                    enabled = !isSending,
                ) {
                    Text("Изменить")
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(messages) { line ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = line.speaker,
                            style = MaterialTheme.typography.labelMedium,
                        )
                        Text(text = line.text)
                    }
                }
            }
        }

        if (error != null) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = "Ошибка: $error",
                        color = MaterialTheme.colorScheme.error,
                    )
                    val retryText = failedMessage
                    if (retryText != null) {
                        Button(
                            onClick = { transmit(retryText, appendUser = false) },
                            enabled = !isSending,
                        ) {
                            Text("Повторить")
                        }
                    }
                }
            }
        }

        OutlinedTextField(
            value = input,
            onValueChange = { input = it },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isSending,
            label = { Text("Сообщение") },
            minLines = 2,
            maxLines = 5,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(if (isSending) "Отправка…" else "")
            Button(
                onClick = { transmit(input, appendUser = true) },
                enabled = !isSending && input.isNotBlank() && client.isConfigured(),
            ) {
                Text("Отправить")
            }
        }
    }
}
