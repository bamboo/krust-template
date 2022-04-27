# The Krust Architecture

A template for efficient and reliable Android apps that are fun to write.

## Main Idea

Kotlin on the outside, Rust on the inside, exchanging messages asynchronously.

Structure the core application logic as
an [asynchronous agent written in Rust](./backend/src/agent/mod.rs) powered by the
[Tokio runtime](https://tokio.rs/).

Structure the presentation logic using
a [reactive presentation model](./app/src/main/java/io/github/bamboo/krust/MainScreen.kt)
powered by the modern and officially
supported [Jetpack Compose](https://developer.android.com/jetpack/compose/kotlin) framework.

Have them communicate by exchanging [messages](./backend/src/agent/protocol.rs) asynchronously.

## Using this template

After cloning the repository, rename all relevant identifiers by running the `krustInit` task:

```
$ ./gradlew krustInit --app-id my.app.id --app-name MyApp
```

Run `./gradlew help --task krustInit` for a description of the options available.

After that, the root project will be ready to be imported in Android Studio and
the [Rust backend](./backend) ready to be opened in your Rust IDE of choice.

## Building

```
./gradlew assembleDebug
```

## Requirements

Out of the box, the project is configured to build native libraries for the x86 and arm64 architectures (see the `cargo` section in [app/build.gradle.kts](./app/build.gradle.kts)) which requires the following toolchains to be installed:

```
rustup target add i686-linux-android
rustup target add aarch64-linux-android
```

## Tested with

* rustc 1.60.0 (7737e0b5c 2022-04-04)

* Android Studio 2021.1.1

* IntelliJ IDEA 2021.3.3
  - org.rust.lang (0.4.169.4584-213)
  - org.toml.lang (213.5744.224)

## Kudos

The Krust architecture is made possible by the following frameworks, libraries and tools:

- [Rust Android Gradle Plugin](https://github.com/mozilla/rust-android-gradle)
- [Jetpack Compose](https://developer.android.com/jetpack/compose/kotlin)
- [Safe JNI Bindings in Rust](https://docs.rs/jni/latest/jni/)
- [Tokio asynchronous runtime](https://tokio.rs/)
- [Kotlin Serialization](https://kotlinlang.org/docs/serialization.html)
- [serde](https://docs.rs/serde/latest/serde/)
- [thiserror](https://docs.rs/thiserror/latest/thiserror/)
- [eyre](https://docs.rs/eyre/latest/eyre/)
