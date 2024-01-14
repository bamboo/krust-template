//! Bridges the worlds of Rust async channels and Java object based callbacks by
//! [forwarding all][forward_all] messages received in a channel to a given Java object.
//! The Java object is expected to expose a `public void invoke(String)` method.
use jni::errors::Error;
use jni::objects::{GlobalRef, JMethodID, JObject, JValue};
use jni::signature::{Primitive, ReturnType};
use jni::{JNIEnv, JavaVM};
use thiserror::*;
use tokio::sync::mpsc;

use crate::agent::protocol::Event;

#[derive(Debug, Error)]
pub enum CallbackError {
    #[error("JNI call failure: {0}")]
    JniCall(#[from] jni::errors::Error),
    #[error("json encoding failure: {0}")]
    JsonEncoding(#[from] serde_json::Error),
}

/// Forwards json encoded events from the `events` channel to the given jvm `on_event` callback
/// until `events` is closed/dropped or the `on_event` invocation fails.
/// `on_event` is assumed to expose a `public void invoke(String)` method just like a
/// Kotlin `(String) -> Unit` function does.
pub fn forward_all(
    mut events: mpsc::Receiver<Event>,
    jvm: JavaVM,
    on_event: GlobalRef,
) -> Result<(), CallbackError> {
    with_callback(&jvm, on_event, |mut on_event| unsafe {
        loop {
            match events.blocking_recv() {
                Some(e) => on_event.invoke(&serde_json::to_string(&e)?)?,
                None => break,
            }
        }
        Ok(())
    })
}

pub fn with_callback<F>(jvm: &JavaVM, callback_ref: GlobalRef, f: F) -> Result<(), CallbackError>
where
    F: FnOnce(StringCallback) -> Result<(), CallbackError>,
{
    let _thread_guard = jvm.attach_current_thread()?;
    {
        f(StringCallback::new(
            jvm.get_env()?,
            callback_ref.as_obj(),
            "invoke",
        )?)
    }
}

pub struct StringCallback<'a> {
    env: JNIEnv<'a>,
    object: &'a JObject<'a>,
    method: JMethodID,
}

impl<'a> StringCallback<'a> {
    fn new(
        mut env: JNIEnv<'a>,
        object: &'a JObject,
        method_name: &str,
    ) -> Result<StringCallback<'a>, Error> {
        let class = env.get_object_class(object)?;
        let method = env.get_method_id(class, method_name, "(Ljava/lang/String;)V")?;
        Ok(StringCallback {
            env,
            object,
            method,
        })
    }

    pub unsafe fn invoke(&mut self, string: &str) -> Result<(), Error> {
        let java_string = self.env.new_string(string)?;
        self.env.call_method_unchecked(
            self.object,
            self.method,
            ReturnType::Primitive(Primitive::Void),
            &[JValue::Object(&java_string).as_jni()],
        )?;
        Ok(())
    }
}
