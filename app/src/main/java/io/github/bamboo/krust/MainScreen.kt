package io.github.bamboo.krust

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import io.github.bamboo.krust.ui.theme.KrustTheme


@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    val state = remember {
        MainScreenState(
            initialStatus = "Tick(secs = 42)",
            initialError = "InterfaceError(error=java.lang.IllegalStateException: channel closed.\n" +
                    "\nLocation:\n" +
                    "\tsrc/agent_ref.rs:29:9)",
            onCommand = ::println,
        )
    }
    MainScreen(state)
}


/**
 * Presentation model for the [main app screen][MainScreen].
 *
 * Coordinates updates to the UI in response to [user interactions][onMessageChange] and
 * [backend events][onEvent].
 *
 * [User interactions][onMessageChange] might also trigger [commands][onCommand].
 *
 * @see MainScreen
 */
class MainScreenState(
    private val onCommand: (Command) -> Unit,
    initialStatus: String = "...",
    initialError: String? = null,
) {
    private
    val statusState = mutableStateOf(initialStatus)

    private
    val errorState = mutableStateOf(initialError)

    private
    val messageState = mutableStateOf("")

    val status by statusState

    val error by errorState

    val message by messageState

    /**
     * Updates the UI state in response to [backend events][Event].
     * @see Backend.onEvent
     */
    fun onEvent(e: Event) {
        when (e) {
            is Event.Started -> {
                errorState.value = null
            }
            else -> {
                statusState.value = e.toString()
            }
        }
    }

    /**
     * Thread-safe error handler.
     * @see Backend.onError
     */
    fun onError(e: Backend.Error) {
        errorState.value = e.toString()
    }

    fun onMessageChange(newValue: String) {
        onCommand(Command.Ping(newValue))
        messageState.value = newValue
    }
}


@Composable
fun MainScreen(state: MainScreenState) {
    KrustTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colors.background,
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Column(
                    modifier = Modifier.weight(1.0f, true),
                    verticalArrangement = Arrangement.Center,
                ) {
                    state.error?.let { error ->
                        val scrollState = rememberScrollState()
                        Text(
                            text = error,
                            color = MaterialTheme.colors.error,
                            modifier = Modifier.verticalScroll(scrollState),
                        )
                    }
                }
                Column(
                    modifier = Modifier.weight(1.0f, true),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Row {
                        TextField(
                            value = state.message,
                            onValueChange = state::onMessageChange,
                            placeholder = { Text("the word crash will crash the backend") }
                        )
                    }
                    Text(state.status)
                }
            }
        }
    }
}
