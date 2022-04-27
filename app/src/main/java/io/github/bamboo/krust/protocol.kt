@file:OptIn(ExperimentalSerializationApi::class)

package io.github.bamboo.krust

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator


/**
 * By convention, messages that flow from the Kotlin frontend to the Rust backend are called _commands_.
 */
@Serializable
@JsonClassDiscriminator("type")
sealed class Command {

    @Serializable
    @SerialName("Ping")
    data class Ping(val payload: String) : Command()
}


/**
 * Messages that flow from the Rust backend to the Kotlin frontend are called _events_.
 */
@Serializable
@JsonClassDiscriminator("type")
sealed class Event {

    @Serializable
    @SerialName("Started")
    object Started : Event()

    @Serializable
    @SerialName("Pong")
    data class Pong(val payload: String) : Event()

    @Serializable
    @SerialName("Tick")
    data class Tick(val secs: UInt) : Event()
}
