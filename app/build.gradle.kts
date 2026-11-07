import java.net.URI
import java.security.MessageDigest
import java.util.zip.ZipInputStream

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

val modelName = "vosk-model-small-de-0.15"
val modelSha256 = "b7e53c90b1f0a38456f4cd62b366ecd58803cd97cd42b06438e2c131713d5e43"

/** Fetches the speech model at build time, so 46 MB of binaries stay out of git. */
val prepareSpeechModel by tasks.registering {
    val outputDir = layout.buildDirectory.dir("generated/speechModel")
    val cachedZip = layout.buildDirectory.file("downloads/$modelName.zip")
    inputs.property("modelSha256", modelSha256)
    outputs.dir(outputDir)
    doLast {
        val zip = cachedZip.get().asFile
        if (!zip.exists()) {
            zip.parentFile.mkdirs()
            URI("https://alphacephei.com/vosk/models/$modelName.zip").toURL().openStream().use { input ->
                zip.outputStream().use(input::copyTo)
            }
        }
        val actual = MessageDigest.getInstance("SHA-256").digest(zip.readBytes()).joinToString("") { "%02x".format(it) }
        check(actual == modelSha256) { "Speech model checksum mismatch: $actual" }

        val target = outputDir.get().asFile.resolve("speech-model")
        target.deleteRecursively()
        ZipInputStream(zip.inputStream()).use { archive ->
            generateSequence { archive.nextEntry }.filterNot { it.isDirectory }.forEach { entry ->
                val file = target.resolve(entry.name.removePrefix("$modelName/"))
                check(file.canonicalPath.startsWith(target.canonicalPath)) { "Unsafe path in model: ${entry.name}" }
                file.parentFile.mkdirs()
                file.outputStream().use(archive::copyTo)
            }
        }
        target.resolve("uuid").writeText(modelSha256)
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
        versionName = "0.1.0"
    }

    buildFeatures {
        compose = true
    }

    sourceSets.getByName("main").assets.directories.add(
        prepareSpeechModel.map { it.outputs.files.singleFile.path }.get(),
    )

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

tasks.matching { it.name.startsWith("merge") && it.name.endsWith("Assets") }.configureEach {
    dependsOn(prepareSpeechModel)
}

dependencies {
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.foundation)
    implementation(libs.compose.ui)
    implementation(libs.activity.compose)
    implementation(libs.vosk.android)
    testImplementation(libs.junit)
}
