//! Defines the protocol between the Kotlin frontend and the Rust backend in the form of
//! json encoded [_commands_][Command] and [_messages_][Message].
//! [serde's internally tagged representation](https://serde.rs/enum-representations.html#internally-tagged)
//! was chosen for easier integration with Kotlin serialization.
use serde::{Deserialize, Serialize};

/// By convention, messages that flow from the Kotlin frontend to the Rust backend are called _commands_.
#[derive(Debug, Deserialize)]
#[serde(tag = "type")]
pub enum Command {
    Ping { payload: String },
}

/// Messages that flow from the Rust backend to the Kotlin frontend are called _events_.
#[derive(PartialEq, Debug, Serialize)]
#[serde(tag = "type")]
pub enum Event {
    Started,
    Pong { payload: String },
    Tick { secs: u32 },
}
