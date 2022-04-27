//! An [agent reference][AgentRef] allows asynchronous communication with an agent via
//! [json encoded][AgentRef::send] [commands][Command] and to eventually request the
//! agent [to stop][AgentRef::stop].
use crate::agent::protocol::Command;
use std::thread::JoinHandle;
use tokio::sync::mpsc::UnboundedSender;
use tokio::sync::watch::Sender;

pub struct AgentRef {
    commands: UnboundedSender<Command>,
    shutdown_sender: Sender<bool>,
    join_handle: JoinHandle<eyre::Result<()>>,
}

impl AgentRef {
    pub fn new(
        commands: UnboundedSender<Command>,
        shutdown_sender: Sender<bool>,
        join_handle: JoinHandle<eyre::Result<()>>,
    ) -> Self {
        Self {
            commands,
            shutdown_sender,
            join_handle,
        }
    }

    pub fn send(&self, c: &[u8]) -> eyre::Result<()> {
        self.commands.send(serde_json::from_slice(c)?)?;
        Ok(())
    }

    pub fn stop(self) -> eyre::Result<()> {
        let _ = self.shutdown_sender.send(true);
        let agent_result = self
            .join_handle
            .join()
            .map_err(|_| eyre::eyre!("Failed to join agent thread."))?;
        Ok(agent_result?)
    }
}
