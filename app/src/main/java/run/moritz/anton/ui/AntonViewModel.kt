package run.moritz.anton.ui

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import run.moritz.anton.AppContainer
import run.moritz.anton.cases.Case
import run.moritz.anton.cases.Field
import run.moritz.anton.progress.Progress
import run.moritz.anton.progress.ProgressEvent
import run.moritz.anton.progress.Rank
import run.moritz.anton.recognition.RecognitionVocabulary
import run.moritz.anton.recognition.SpeechRecognizer
import run.moritz.anton.session.CaseSession
import run.moritz.anton.settings.Settings
import run.moritz.anton.sound.RadioSounds
import run.moritz.anton.sound.SilentRadio
import run.moritz.anton.spelling.Evaluation
import run.moritz.anton.spelling.SpellingEvaluator

enum class Screen { STATION, CASE, TABLE, SETTINGS }

sealed interface SpeechState {
    data object NeedsPermission : SpeechState

    data object PermissionDenied : SpeechState

    data object Loading : SpeechState

    data object Ready : SpeechState

    data class Failed(val reason: String) : SpeechState
}

enum class TransmissionPhase { IDLE, SENDING, EVALUATING }

sealed interface CaseDialog {
    data class Plausibility(val wrong: Evaluation.Wrong) : CaseDialog

    data object TableWarning : CaseDialog

    data object LeaveWarning : CaseDialog

    data class Result(val errorFree: Boolean) : CaseDialog

    data class Certificate(val rank: Rank) : CaseDialog
}

enum class LogNotice { NONE, CONFIRMED, NOT_UNDERSTOOD, MICROPHONE_FAILED }

/** Everything the case screen shows. */
data class CaseUi(
    val case: Case,
    val number: Int,
    val fieldIndex: Int = 0,
    val phase: TransmissionPhase = TransmissionPhase.IDLE,
    val sentSymbols: List<String> = emptyList(),
    val notice: LogNotice = LogNotice.NONE,
    val dialog: CaseDialog? = null
) {
    val field: Field get() = case.fields[fieldIndex.coerceAtMost(case.fields.lastIndex)]
    val finished: Boolean get() = fieldIndex >= case.fields.size
}

class AntonViewModel(private val container: AppContainer) : ViewModel() {
    val table = container.table
    private val vocabulary = RecognitionVocabulary(table)
    private val evaluator = SpellingEvaluator(table)
    private var recognizer: SpeechRecognizer? = null
    private var session: CaseSession? = null

    var screen by mutableStateOf(Screen.STATION)
        private set
    var speech by mutableStateOf<SpeechState>(SpeechState.NeedsPermission)
        private set
    var progress by mutableStateOf(Progress())
        private set
    var settings by mutableStateOf(container.settingsStore.load())
        private set
    var caseUi by mutableStateOf<CaseUi?>(null)
        private set

    private val sounds: RadioSounds get() = if (settings.radioSounds) container.sounds else SilentRadio

    val capturesVolumeKeys: Boolean get() = screen == Screen.CASE && settings.volumeKeysTalk

    init {
        viewModelScope.launch { progress = container.progressStore.load() }
    }

    fun onMicrophoneGranted() {
        if (speech == SpeechState.Ready || speech == SpeechState.Loading) return
        speech = SpeechState.Loading
        viewModelScope.launch {
            runCatching { container.createRecognizer(vocabulary) }
                .onSuccess {
                    recognizer = it
                    speech = SpeechState.Ready
                }
                .onFailure {
                    Log.e(TAG, "Speech model could not be loaded", it)
                    speech = SpeechState.Failed(it.message ?: it.toString())
                }
        }
    }

    fun onMicrophoneDenied() {
        speech = SpeechState.PermissionDenied
    }

    fun startNextCase() = open(container.generator.next(progress.rank, progress.mastery))

    fun startDrill(symbol: String) = open(container.generator.drill(symbol))

    private fun open(case: Case) {
        session = CaseSession(case.fields, countsTowardProgress = case !is Case.Drill, evaluator)
        caseUi = CaseUi(case, number = progress.casesCompleted + 1)
        screen = Screen.CASE
    }

    /** The talk key went down or up, on screen or on the device. */
    fun talk(pressed: Boolean) {
        val ui = caseUi ?: return
        val active = recognizer ?: return
        if (pressed) {
            if (screen == Screen.CASE && ui.phase == TransmissionPhase.IDLE && ui.dialog == null && !ui.finished) {
                open(active, ui)
            }
        } else if (ui.phase == TransmissionPhase.SENDING) {
            close(active, ui)
        }
    }

    private fun open(active: SpeechRecognizer, ui: CaseUi) {
        val opened = runCatching {
            active.startTransmission { heard ->
                viewModelScope.launch { showHeard(heard) }
            }
        }
        opened.onSuccess {
            caseUi = ui.copy(phase = TransmissionPhase.SENDING, sentSymbols = emptyList(), notice = LogNotice.NONE)
        }.onFailure {
            Log.e(TAG, "Transmission could not be opened", it)
            caseUi = ui.copy(notice = LogNotice.MICROPHONE_FAILED)
        }
    }

    private fun showHeard(heard: String) {
        val ui = caseUi ?: return
        if (ui.phase == TransmissionPhase.SENDING) caseUi = ui.copy(sentSymbols = vocabulary.decode(heard))
    }

    private fun close(active: SpeechRecognizer, ui: CaseUi) {
        caseUi = ui.copy(phase = TransmissionPhase.EVALUATING)
        sounds.keyReleased()
        viewModelScope.launch {
            runCatching { active.finishTransmission() }
                .onSuccess(::evaluate)
                .onFailure {
                    Log.e(TAG, "Transmission failed", it)
                    caseUi = caseUi?.copy(phase = TransmissionPhase.IDLE, notice = LogNotice.MICROPHONE_FAILED)
                }
        }
    }

    private fun evaluate(heard: String) {
        val active = checkNotNull(session)
        Log.d(TAG, "Heard \"$heard\" for field ${active.currentField?.kind}")
        val symbols = vocabulary.decode(heard)
        val events = active.transmit(symbols, heard)
        val pending = active.pending
        when {
            pending != null -> sounds.rejected()
            !active.notUnderstood -> sounds.confirmed()
        }
        caseUi = caseUi?.copy(
            phase = TransmissionPhase.IDLE,
            sentSymbols = if (pending == null && !active.notUnderstood) emptyList() else symbols,
            notice = if (active.notUnderstood) LogNotice.NOT_UNDERSTOOD else LogNotice.NONE,
            dialog = pending?.let { CaseDialog.Plausibility(it) }
        )
        apply(active, events)
    }

    fun repeatField() {
        val active = session ?: return
        val events = active.repeat()
        caseUi = caseUi?.copy(dialog = null, sentSymbols = emptyList())
        apply(active, events)
    }

    fun overruleField() {
        val active = session ?: return
        val events = active.overrule()
        sounds.confirmed()
        caseUi = caseUi?.copy(dialog = null, sentSymbols = emptyList())
        apply(active, events)
    }

    private fun apply(active: CaseSession, events: List<ProgressEvent>) {
        val rankBefore = progress.rank
        progress = events.fold(progress) { state, event -> state.after(event) }
        val saved = progress
        viewModelScope.launch {
            events.filterIsInstance<ProgressEvent.Misrecognized>().forEach {
                container.progressStore.recordMisrecognition(it.expected, it.heard)
            }
            if (events.isNotEmpty()) container.progressStore.save(saved)
        }

        val ui = caseUi ?: return
        val advanced = active.fieldIndex > ui.fieldIndex
        caseUi = ui.copy(
            fieldIndex = active.fieldIndex,
            notice = if (advanced) LogNotice.CONFIRMED else ui.notice
        )
        if (active.finished) finish(active, rankBefore)
    }

    private fun finish(active: CaseSession, rankBefore: Rank) {
        val promoted = progress.rank != rankBefore
        if (promoted) sounds.promoted()
        caseUi = caseUi?.copy(
            dialog = if (promoted) CaseDialog.Certificate(progress.rank) else CaseDialog.Result(active.errorFree)
        )
    }

    /** The table was asked for from within a case. It warns once, because looking up costs the streak. */
    fun requestTableFromCase() {
        val ui = caseUi ?: return
        if (session?.tableStillFree == true) {
            caseUi = ui.copy(dialog = CaseDialog.TableWarning)
        } else {
            screen = Screen.TABLE
        }
    }

    fun confirmTable() {
        val active = session ?: return
        val events = active.openTable()
        caseUi = caseUi?.copy(dialog = null)
        apply(active, events)
        screen = Screen.TABLE
    }

    fun requestLeaveCase() {
        val ui = caseUi ?: return
        val untouched = ui.fieldIndex == 0 && session?.errorFree == true
        if (ui.case is Case.Drill || untouched) leaveCase() else caseUi = ui.copy(dialog = CaseDialog.LeaveWarning)
    }

    fun dismissDialog() {
        caseUi = caseUi?.copy(dialog = null)
    }

    fun leaveCase() {
        talk(false)
        session = null
        caseUi = null
        screen = Screen.STATION
    }

    fun openTable() {
        screen = Screen.TABLE
    }

    fun closeTable() {
        screen = if (caseUi != null) Screen.CASE else Screen.STATION
    }

    fun drillAgain() {
        (caseUi?.case as? Case.Drill)?.let { startDrill(it.symbol) }
    }

    fun drillFinishedToTable() {
        session = null
        caseUi = null
        screen = Screen.TABLE
    }

    fun openSettings() {
        screen = Screen.SETTINGS
    }

    fun closeSettings() {
        screen = Screen.STATION
    }

    fun update(settings: Settings) {
        this.settings = settings
        container.settingsStore.save(settings)
    }

    fun resetProgress() {
        progress = Progress()
        viewModelScope.launch { container.progressStore.reset() }
    }

    private companion object {
        const val TAG = "Anton"
    }
}
