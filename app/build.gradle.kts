import java.net.URI
import java.security.MessageDigest
import java.util.zip.ZipInputStream

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

/** Fetches the speech model at build time, so 46 MB of binaries stay out of git. */
abstract class PrepareSpeechModel : DefaultTask() {
    @get:Input
    abstract val modelName: Property<String>

    @get:Input
    abstract val modelSha256: Property<String>

    @get:Internal
    abstract val cachedZip: RegularFileProperty

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @TaskAction
    fun prepare() {
        val name = modelName.get()
        val sha256 = modelSha256.get()
        val zip = cachedZip.get().asFile
        if (!zip.exists()) {
            zip.parentFile.mkdirs()
            URI("https://alphacephei.com/vosk/models/$name.zip").toURL().openStream().use { input ->
                zip.outputStream().use(input::copyTo)
            }
        }
        val actual = MessageDigest.getInstance("SHA-256").digest(zip.readBytes()).joinToString("") { "%02x".format(it) }
        check(actual == sha256) { "Speech model checksum mismatch: $actual" }

        val target = outputDirectory.get().asFile.resolve("speech-model")
        target.deleteRecursively()
        ZipInputStream(zip.inputStream()).use { archive ->
            generateSequence { archive.nextEntry }.filterNot { it.isDirectory }.forEach { entry ->
                val file = target.resolve(entry.name.removePrefix("$name/"))
                check(file.canonicalPath.startsWith(target.canonicalPath)) { "Unsafe path in model: ${entry.name}" }
                file.parentFile.mkdirs()
                file.outputStream().use(archive::copyTo)
            }
        }
        target.resolve("uuid").writeText(sha256)
    }
}

android {
    namespace = "run.moritz.anton"
    compileSdk = 37

    defaultConfig {
        applicationId = "run.moritz.anton"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        ndk {
            abiFilters += listOf("arm64-v8a", "armeabi-v7a")
        }
    }

    buildTypes {
        release {
            // Debug keys until a release key exists, so the GitHub APK installs directly.
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    // Vosk ships native code for abandoned architectures that the ABI filter cannot remove alone.
    packaging {
        jniLibs.excludes += listOf("lib/mips/**", "lib/mips64/**")
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

androidComponents {
    onVariants { variant ->
        val prepareSpeechModel = tasks.register<PrepareSpeechModel>(
            "prepare${variant.name.replaceFirstChar {
                it.uppercase()
            }}SpeechModel"
        ) {
            modelName = "vosk-model-small-de-0.15"
            modelSha256 = "b7e53c90b1f0a38456f4cd62b366ecd58803cd97cd42b06438e2c131713d5e43"
            cachedZip = layout.buildDirectory.file("downloads/vosk-model-small-de-0.15.zip")
        }
        variant.sources.assets?.addGeneratedSourceDirectory(prepareSpeechModel, PrepareSpeechModel::outputDirectory)
    }
}

dependencies {
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.foundation)
    implementation(libs.compose.ui)
    implementation(libs.core.ktx)
    implementation(libs.activity.compose)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.room.runtime)
    ksp(libs.room.compiler)
    implementation(libs.vosk.android)

    testImplementation(libs.junit)
    testImplementation(libs.coroutines.test)

    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.compose.ui.test)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.espresso)
    debugImplementation(libs.compose.ui.test.manifest)
}
