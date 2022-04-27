package io.github.bamboo.krust

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json


/**
 * Exposes the Rust backend as a service that can be [started][onStart], have commands [sent to][send]
 * and then eventually [stopped][onStop].
 **/
class Backend(

    /**
     * Receives asynchronous events from the agent. All events are dispatched in the same thread
     * and never in the UI thread.
     */
    private
    val onEvent: (Event) -> Unit,

    /**
     * Receives potentially asynchronous error reports from the agent. More than one error can be reported and
     * each error might be reported in a different thread thus the given function
     * must be thread-safe.
     */
    private
    val onError: (Error) -> Unit,
) {

    sealed interface Error {

        /**
         * An unrecoverable error reported by the backend, no more events
         * should be expected after this error.
         */
        data class AgentError(val report: String) : Error

        /**
         * An error in the communication between the frontend and the backend.
         */
        data class InterfaceError(val error: Exception) : Error
    }

    init {
        System.loadLibrary("backend")
    }

    private
    val format = Json

    fun onStart() {
        withErrorReporting {
            start(
                onEvent = { json ->
                    withErrorReporting {
                        onEvent(format.decodeFromString(json))
                    }
                },
                onError = {
                    onError(Error.AgentError(it))
                },
            )
        }
    }

    fun send(command: Command) {
        withErrorReporting {
            send(format.encodeToString(command))
        }
    }

    fun onStop() {
        withErrorReporting {
            stop()
        }
    }

    private
    fun withErrorReporting(f: () -> Unit) {
        try {
            f()
        } catch (e: Exception) {
            onError(Error.InterfaceError(e))
        }
    }

    // Implemented in Rust.
    private
    external fun start(
        onEvent: (String) -> Unit,
        onError: (String) -> Unit,
    )

    private
    external fun send(
        json: String,
    )

    private
    external fun stop()
}
