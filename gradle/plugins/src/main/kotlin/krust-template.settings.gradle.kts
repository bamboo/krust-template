// # krust-template plugin
// Registers the `krustInit` task in the root project.
gradle.rootProject {
    tasks.register("krustInit", krust.KrustInit::class) {
        fromAppId.set("io.github.bamboo.krust")
        fromAppName.set("Krust")
        inputFiles.setFrom(
            "app/build.gradle.kts",
            "app/src/androidTest/java/io/github/bamboo/krust/ExampleInstrumentedTest.kt",
            "app/src/test/java/io/github/bamboo/krust/JsonEncodingTest.kt",
            "app/src/main/res/values/strings.xml",
            "app/src/main/res/values/themes.xml",
            "app/src/main/AndroidManifest.xml",
            "app/src/main/java/io/github/bamboo/krust/ui/theme/Theme.kt",
            "app/src/main/java/io/github/bamboo/krust/ui/theme/Shape.kt",
            "app/src/main/java/io/github/bamboo/krust/ui/theme/Color.kt",
            "app/src/main/java/io/github/bamboo/krust/ui/theme/Type.kt",
            "app/src/main/java/io/github/bamboo/krust/MainActivity.kt",
            "app/src/main/java/io/github/bamboo/krust/protocol.kt",
            "app/src/main/java/io/github/bamboo/krust/MainScreen.kt",
            "app/src/main/java/io/github/bamboo/krust/Backend.kt",
            "backend/src/lib.rs",
            "README.md",
            "settings.gradle.kts",
        )
    }
}
