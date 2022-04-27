//! This module implements the JNI interface between the Kotlin frontend and the asynchronous Rust backend [agent].
//! It should require very little change from project to project, mostly different names and
//! perhaps different [tokio runtime settings][build_agent_runtime].
//! # Application specific logic
//! The [agent] module is where the application specific logic goes.
extern crate jni;

use std::sync::{Mutex, MutexGuard};
use std::thread;

use eyre::{bail, Context, eyre, Report};
use jni::{JavaVM, JNIEnv};
use jni::objects::{GlobalRef, JClass, JObject, JString};
use tokio::runtime;
use tokio::sync::mpsc;

use crate::agent::protocol::Event;
use crate::agent_ref::AgentRef;

mod agent;
mod agent_ref;
mod callback;

#[no_mangle]
#[allow(non_snake_case)]
pub extern "C" fn Java_io_github_bamboo_krust_Backend_start(
    env: JNIEnv,
    _class: JClass,
    /* Kotlin's (String) -> Unit */
    on_event: JObject,
    /* Kotlin's (String) -> Unit */
    on_error: JObject,
) {
    if let Err(e) = start(env, on_event, on_error) {
        throw_as_illegal_state_exception(&e, env).expect("start");
    }
}

#[no_mangle]
#[allow(non_snake_case)]
pub extern "C" fn Java_io_github_bamboo_krust_Backend_send(
    env: JNIEnv,
    _class: JClass,
    command: JString,
) {
    if let Err(e) = send(env, command) {
        throw_as_illegal_state_exception(&e, env).expect("send");
    }
}

#[no_mangle]
#[allow(non_snake_case)]
pub extern "C" fn Java_io_github_bamboo_krust_Backend_stop(env: JNIEnv, _class: JClass) {
    if let Err(e) = stop() {
        throw_as_illegal_state_exception(&e, env).expect("stop");
    }
    logging::stop();
}

fn start(env: JNIEnv, on_event: JObject, on_error: JObject) -> eyre::Result<()> {
    let mut agent_ref = lock_agent_ref()?;
    if agent_ref.is_some() {
        bail!("backend has already been started.");
    }
    *agent_ref = Some(spawn_agent(env, on_event, on_error)?);
    Ok(())
}

fn send(env: JNIEnv, command: JString) -> eyre::Result<()> {
    lock_agent_ref()?
        .as_ref()
        .ok_or_else(|| eyre!("backed must be started before send can be called"))?
        .send(env.get_string(command)?.to_bytes())
}

fn stop() -> eyre::Result<()> {
    lock_agent_ref()?
        .take()
        .ok_or_else(|| eyre!("backend has already been stopped"))?
        .stop()
}

fn lock_agent_ref() -> eyre::Result<MutexGuard<'static, Option<AgentRef>>> {
    AGENT_REF.lock().map_err(|e| eyre!("{:?}", e))
}

lazy_static::lazy_static! {
    static ref AGENT_REF: Mutex<Option<AgentRef>> = Mutex::new(None);
}

fn spawn_agent(env: JNIEnv, on_event: JObject, on_error: JObject) -> eyre::Result<AgentRef> {
    let (commands, agent_inbox) = tokio::sync::mpsc::unbounded_channel();
    let (agent_outbox, events) = mpsc::channel::<Event>(16);
    let (shutdown_sender, shutdown) = tokio::sync::watch::channel(false);

    let jvm = env.get_java_vm()?;
    let on_error = env.new_global_ref(on_error)?;
    let on_agent_error = on_error.clone();

    // let the agent run on a dedicated single-threaded tokio runtime,
    let join_handle = thread::spawn(move || {
        with_error_reporting(&jvm, on_agent_error, || {
            build_agent_runtime()
                .context("tokio runtime")?
                .block_on(async { agent::run_until(shutdown, agent_inbox, agent_outbox).await })?;
            Ok(())
        })
    });

    // while its events are serialized and pushed back to the jvm client in a separate thread.
    let jvm = env.get_java_vm()?;
    let on_event_jvm = env.get_java_vm()?;
    let on_event = env.new_global_ref(on_event)?;
    thread::spawn(move || {
        with_error_reporting(&jvm, on_error, || {
            callback::forward_all(events, on_event_jvm, on_event).context("callback::forward_all")
        })
    });

    Ok(AgentRef::new(commands, shutdown_sender, join_handle))
}

fn with_error_reporting<F: FnOnce() -> eyre::Result<()>>(
    jvm: &JavaVM,
    on_error: GlobalRef,
    f: F,
) -> eyre::Result<()> {
    if let Err(e) = f() {
        callback::with_callback(&jvm, on_error, |on_error| {
            on_error.invoke(&format_error(&e))?;
            Ok(())
        })?
    }
    Ok(())
}

fn build_agent_runtime() -> std::io::Result<runtime::Runtime> {
    runtime::Builder::new_current_thread().enable_time().build()
}

fn throw_as_illegal_state_exception(
    e: &eyre::Report,
    env: JNIEnv,
) -> Result<(), jni::errors::Error> {
    throw_illegal_state_exception(env, &format_error(&e))
}

fn format_error(e: &Report) -> String {
    format!("{:?}", e)
}

fn throw_illegal_state_exception(env: JNIEnv, error: &str) -> Result<(), jni::errors::Error> {
    env.throw_new(env.find_class("java/lang/IllegalStateException")?, &error)
}

#[cfg(not(target_os = "android"))]
mod logging {
    pub fn stop() {
        println!("stopping rust backend.");
    }
}

#[cfg(target_os = "android")]
mod logging {
    use std::ffi::CStr;

    pub fn stop() {
        ndk_glue::android_log(log::Level::Debug, LOG_TAG, LOG_MSG_STOPPING);
    }

    static LOG_TAG: &CStr = unsafe { CStr::from_bytes_with_nul_unchecked(b"krust\0") };

    static LOG_MSG_STOPPING: &CStr =
        unsafe { CStr::from_bytes_with_nul_unchecked(b"stopping rust backend.\0") };
}
