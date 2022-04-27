//! This is where the actual backend implementation goes, structured as an [asynchronous agent][run_until].
use std::time::Duration;

use tokio::sync::mpsc::error::SendError;
use tokio::sync::mpsc::{Sender, UnboundedReceiver};
use tokio::sync::watch::Receiver;

use crate::agent::protocol::*;

pub mod protocol;

pub type Inbox = UnboundedReceiver<Command>;

pub type Outbox = Sender<Event>;

#[derive(Debug, thiserror::Error)]
pub enum Error {
    #[error("Failed to send event.")]
    OutboxError(#[from] SendError<Event>),
    #[error("The agent has crashed.")]
    Crash,
}

pub async fn run_until(
    mut shutdown: Receiver<bool>,
    mut inbox: Inbox,
    outbox: Outbox,
) -> Result<(), Error> {
    let mut one_sec = tokio::time::interval(Duration::from_secs(1));
    let mut secs = 1;

    outbox.send(Event::Started).await?;
    loop {
        tokio::select! {
            _ = shutdown.changed() => break,
            _ = one_sec.tick() => {
                outbox.send(Event::Tick { secs }).await?;
                secs += 1;
            },
            Some(c) = inbox.recv() => {
                match c {
                    Command::Ping { payload } => {
                        // simulate an error
                        if payload == "crash" {
                            // open problem: how to transparently propagate the backtrace from here?
                            return Err(Error::Crash)
                        }
                        outbox.send(Event::Pong { payload }).await?;
                    }
                }
            },
            else => break,
        }
    }
    Ok(())
}

#[cfg(test)]
mod tests {
    use tokio::sync::{mpsc, watch};

    use crate::agent::protocol::{Command, Event};
    use crate::agent::run_until;

    #[tokio::test]
    async fn agent_is_well_behaved() -> eyre::Result<()> {
        let (commands, inbox) = mpsc::unbounded_channel();
        let (outbox, mut events) = mpsc::channel(1);
        let (shutdown_sender, shutdown) = watch::channel(false);
        let join_handle = tokio::spawn(async { run_until(shutdown, inbox, outbox).await });

        commands.send(Command::Ping {
            payload: String::from("command"),
        })?;

        assert_eq!(events.recv().await, Some(Event::Started));

        assert_eq!(
            events.recv().await,
            Some(Event::Pong {
                payload: String::from("command")
            })
        );

        let _ = shutdown_sender.send(true);
        assert_eq!(join_handle.await?.map_err(|e| format!("{:?}", e)), Ok(()));

        Ok(())
    }
}
