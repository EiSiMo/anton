package run.moritz.anton

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import run.moritz.anton.recognition.RecognitionVocabulary
import run.moritz.anton.recognition.SpeechRecognizer
import run.moritz.anton.recognition.TrialPrompt
import run.moritz.anton.recognition.VoskSpeechRecognizer
import run.moritz.anton.recognition.recognitionTrial
import run.moritz.anton.spelling.SpellingTable

private const val TAG = "AntonRecognitionTrial"

/** Measures recognition quality on a real device. Results go to logcat under [TAG]. */
class MainActivity : ComponentActivity() {
    private val vocabulary = RecognitionVocabulary(SpellingTable.classicGerman)
    private val prompts: List<TrialPrompt> = recognitionTrial(SpellingTable.classicGerman)

    private var recognizer by mutableStateOf<SpeechRecognizer?>(null)
    private var failure by mutableStateOf<String?>(null)
    private var promptIndex by mutableIntStateOf(0)
    private var correct by mutableIntStateOf(0)
    private var transmitting by mutableStateOf(false)
    private var lastHeard by mutableStateOf("")
    private var evaluating = false

    private val requestMicrophone = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) loadRecognizer() else failure = getString(R.string.microphone_denied)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            loadRecognizer()
        } else {
            requestMicrophone.launch(Manifest.permission.RECORD_AUDIO)
        }
        setContent {
            TrialScreen(
                prompt = prompts.getOrNull(promptIndex),
                position = promptIndex + 1,
                total = prompts.size,
                correct = correct,
                ready = recognizer != null,
                transmitting = transmitting,
                lastHeard = lastHeard,
                failure = failure,
                onTalkChanged = ::setTalking,
            )
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.keyCode != KeyEvent.KEYCODE_VOLUME_DOWN && event.keyCode != KeyEvent.KEYCODE_VOLUME_UP) {
            return super.dispatchKeyEvent(event)
        }
        when (event.action) {
            KeyEvent.ACTION_DOWN -> if (event.repeatCount == 0) setTalking(true)
            KeyEvent.ACTION_UP -> setTalking(false)
        }
        return true
    }

    private fun loadRecognizer() {
        lifecycleScope.launch {
            runCatching { VoskSpeechRecognizer.create(this@MainActivity, vocabulary) }
                .onSuccess { recognizer = it }
                .onFailure {
                    Log.e(TAG, "Speech model could not be loaded", it)
                    failure = it.message
                }
        }
    }

    private fun setTalking(talking: Boolean) {
        val active = recognizer ?: return
        val prompt = prompts.getOrNull(promptIndex) ?: return
        if (talking && !transmitting && !evaluating) {
            active.startTransmission()
            transmitting = true
        } else if (!talking && transmitting) {
            transmitting = false
            evaluating = true
            lifecycleScope.launch {
                val heard = active.finishTransmission()
                val symbols = vocabulary.decode(heard)
                val isCorrect = symbols == prompt.expected
                Log.i(TAG, "ok=$isCorrect expected=${prompt.expected} decoded=$symbols heard=\"$heard\"")
                if (isCorrect) correct += 1
                lastHeard = heard
                promptIndex += 1
                evaluating = false
                if (promptIndex == prompts.size) Log.i(TAG, "finished correct=$correct total=${prompts.size}")
            }
        }
    }
}

@Composable
private fun TrialScreen(
    prompt: TrialPrompt?,
    position: Int,
    total: Int,
    correct: Int,
    ready: Boolean,
    transmitting: Boolean,
    lastHeard: String,
    failure: String?,
    onTalkChanged: (Boolean) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().background(Color(0xFFD4D0C8)).safeDrawingPadding().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        BasicText(stringResource(R.string.trial_progress, minOf(position, total), total, correct), style = LABEL)
        when {
            failure != null -> BasicText(failure, style = PROMPT)
            !ready -> BasicText(stringResource(R.string.model_loading), style = PROMPT)
            prompt == null -> BasicText(stringResource(R.string.trial_finished, correct, total), style = PROMPT)
            else -> BasicText(prompt.text, style = PROMPT)
        }
        BasicText(stringResource(R.string.last_heard, lastHeard), style = LABEL, modifier = Modifier.weight(1f))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .background(if (transmitting) Color(0xFF0A246A) else Color(0xFFECE9D8))
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = {
                            onTalkChanged(true)
                            tryAwaitRelease()
                            onTalkChanged(false)
                        },
                    )
                },
            contentAlignment = Alignment.Center,
        ) {
            BasicText(
                stringResource(R.string.talk),
                style = PROMPT.copy(color = if (transmitting) Color.White else Color.Black),
            )
        }
    }
}

private val LABEL = TextStyle(fontSize = 16.sp, color = Color.Black)
private val PROMPT = TextStyle(fontSize = 26.sp, color = Color.Black)
