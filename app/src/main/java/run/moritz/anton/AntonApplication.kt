package run.moritz.anton

import android.app.Application
import android.content.Context
import run.moritz.anton.cases.CaseGenerator
import run.moritz.anton.cases.PersonCatalog
import run.moritz.anton.progress.ProgressStore
import run.moritz.anton.progress.RoomProgressStore
import run.moritz.anton.recognition.RecognitionVocabulary
import run.moritz.anton.recognition.SpeechRecognizer
import run.moritz.anton.recognition.VoskSpeechRecognizer
import run.moritz.anton.settings.SettingsStore
import run.moritz.anton.settings.SharedPreferencesSettingsStore
import run.moritz.anton.sound.RadioSounds
import run.moritz.anton.sound.SynthesizedRadio
import run.moritz.anton.spelling.SpellingTable

/** The app's long-lived collaborators. Tests swap in their own. */
class AppContainer(
    val table: SpellingTable,
    val progressStore: ProgressStore,
    val settingsStore: SettingsStore,
    val sounds: RadioSounds,
    val generator: CaseGenerator,
    val clock: () -> Long,
    val createRecognizer: suspend (RecognitionVocabulary) -> SpeechRecognizer
)

class AntonApplication : Application() {
    var container: AppContainer? = null
        get() {
            if (field == null) field = productionContainer(this)
            return field
        }

    private fun productionContainer(context: Context): AppContainer {
        val table = SpellingTable.classicGerman
        return AppContainer(
            table = table,
            progressStore = RoomProgressStore.create(context),
            settingsStore = SharedPreferencesSettingsStore(context),
            sounds = SynthesizedRadio(),
            generator = CaseGenerator(table, PersonCatalog.load(context)),
            clock = System::currentTimeMillis,
            createRecognizer = { vocabulary -> VoskSpeechRecognizer.create(context, vocabulary) }
        )
    }
}
