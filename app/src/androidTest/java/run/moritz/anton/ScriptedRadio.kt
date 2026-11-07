package run.moritz.anton

import androidx.test.platform.app.InstrumentationRegistry
import kotlin.random.Random
import run.moritz.anton.cases.CaseGenerator
import run.moritz.anton.cases.PersonCatalog
import run.moritz.anton.progress.Progress
import run.moritz.anton.progress.ProgressStore
import run.moritz.anton.recognition.SpeechRecognizer
import run.moritz.anton.settings.Settings
import run.moritz.anton.settings.SettingsStore
import run.moritz.anton.sound.SilentRadio
import run.moritz.anton.spelling.SpellingTable

/** A recognizer that hears what the test says it heard. */
class ScriptedRecognizer : SpeechRecognizer {
    var nextHeard = ""
    private var listener: (String) -> Unit = {}

    override fun startTransmission(onPartial: (String) -> Unit) {
        listener = onPartial
        onPartial(nextHeard)
    }

    /** Hears more while the transmission stays open. */
    fun hear(heard: String) {
        nextHeard = heard
        listener(heard)
    }

    override suspend fun finishTransmission(): String = nextHeard
}

class MemoryProgressStore(var progress: Progress = Progress()) : ProgressStore {
    val misrecognitions = mutableListOf<Pair<String?, String>>()

    override suspend fun load() = progress

    override suspend fun save(progress: Progress) {
        this.progress = progress
    }

    override suspend fun recordMisrecognition(expected: String?, heard: String) {
        misrecognitions += expected to heard
    }

    override suspend fun reset() {
        progress = Progress()
    }
}

class MemorySettingsStore(private var settings: Settings = Settings()) : SettingsStore {
    override fun load() = settings

    override fun save(settings: Settings) {
        this.settings = settings
    }
}

/** Installs a container with scripted speech, so a test can play through real screens. */
fun installScriptedApp(
    recognizer: ScriptedRecognizer,
    store: ProgressStore,
    settings: SettingsStore = MemorySettingsStore(),
    seed: Int = 42
) {
    val table = SpellingTable.classicGerman
    val application = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as AntonApplication
    application.container = AppContainer(
        table = table,
        progressStore = store,
        settingsStore = settings,
        sounds = SilentRadio,
        generator = CaseGenerator(table, PersonCatalog.load(application), Random(seed)),
        clock = System::currentTimeMillis,
        createRecognizer = { recognizer }
    )
}

/** What a player says for a text, as the speech model would write it. */
fun spoken(text: String): String {
    val table = SpellingTable.classicGerman
    return text.filter { it.isLetterOrDigit() || it == '-' }.map { char ->
        val entry = table.entries.first { it.symbol.equals(char.toString(), ignoreCase = true) }
        when (entry.word) {
            "Xanthippe" -> "san tippe"
            "Eszett" -> "es zett"
            else -> entry.word.lowercase()
        }
    }.joinToString(" ")
}
