package io.github.bamboo.krust

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent


/**
 * Connects the [backend][Backend] to the [presentation model][MainScreenState] and
 * keeps the backend active during the [activity's visible lifetime](https://developer.android.com/reference/android/app/Activity.html#ActivityLifecycle)
 * (between [onStart] and [onStop]).
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MainScreen(state)
        }
    }

    override fun onStart() {
        super.onStart()
        backend.onStart()
    }

    override fun onStop() {
        backend.onStop()
        super.onStop()
    }

    private
    var state = MainScreenState(
        onCommand = this::onCommand,
    )

    private
    val backend = Backend(
        onEvent = state::onEvent,
        onError = state::onError,
    )

    private
    fun onCommand(command: Command) {
        backend.send(command)
    }
}