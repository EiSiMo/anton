package run.moritz.anton.recognition

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import org.json.JSONArray
import org.json.JSONObject
import org.vosk.Model
import org.vosk.Recognizer
import org.vosk.android.StorageService

/** Records from the microphone while a transmission is open and decodes it with a fixed Vosk grammar. */
class VoskSpeechRecognizer private constructor(model: Model, vocabulary: RecognitionVocabulary) : SpeechRecognizer {
    private val recognizer = Recognizer(model, SAMPLE_RATE.toFloat(), JSONArray(vocabulary.grammar).toString())
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var recording: Deferred<String>? = null

    @Volatile
    private var transmitting = false

    @SuppressLint("MissingPermission") // The screen only opens a transmission once the permission is granted.
    override fun startTransmission(onPartial: (String) -> Unit) {
        check(recording == null) { "A transmission is already open" }
        val bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL, ENCODING).coerceAtLeast(MIN_BUFFER_BYTES)
        val audio = AudioRecord(MediaRecorder.AudioSource.VOICE_RECOGNITION, SAMPLE_RATE, CHANNEL, ENCODING, bufferSize)
        check(audio.state == AudioRecord.STATE_INITIALIZED) { "Microphone could not be opened" }
        audio.startRecording()
        transmitting = true
        recording = scope.async {
            try {
                decode(audio, bufferSize, onPartial)
            } finally {
                audio.stop()
                audio.release()
            }
        }
    }

    override suspend fun finishTransmission(): String {
        val job = checkNotNull(recording) { "No transmission is open" }
        delay(RELEASE_TAIL_MILLIS)
        transmitting = false
        recording = null
        return job.await()
    }

    /**
     * Vosk closes a segment at every pause and forgets it on the next audio chunk, so a transmission is the sum
     * of its closed segments plus the final one.
     */
    private fun decode(audio: AudioRecord, bufferSize: Int, onPartial: (String) -> Unit): String {
        val buffer = ByteArray(bufferSize)
        val segments = mutableListOf<String>()
        try {
            while (transmitting) {
                val read = audio.read(buffer, 0, buffer.size)
                check(read >= 0) { "Microphone read failed with code $read" }
                if (recognizer.acceptWaveForm(buffer, read)) {
                    segments += JSONObject(recognizer.result).getString("text")
                    onPartial(segments.joined())
                } else {
                    onPartial((segments + JSONObject(recognizer.partialResult).getString("partial")).joined())
                }
            }
            segments += JSONObject(recognizer.finalResult).getString("text")
            return segments.joined()
        } finally {
            recognizer.reset()
        }
    }

    private fun List<String>.joined() = filter { it.isNotBlank() }.joinToString(" ")

    companion object {
        private const val SAMPLE_RATE = 16_000
        private const val CHANNEL = AudioFormat.CHANNEL_IN_MONO
        private const val ENCODING = AudioFormat.ENCODING_PCM_16BIT
        private const val MIN_BUFFER_BYTES = 4_096
        private const val RELEASE_TAIL_MILLIS = 300L
        private const val MODEL_ASSET_PATH = "speech-model"

        suspend fun create(context: Context, vocabulary: RecognitionVocabulary): VoskSpeechRecognizer {
            val model = suspendCoroutine { continuation ->
                StorageService.unpack(
                    context,
                    MODEL_ASSET_PATH,
                    "model",
                    { continuation.resume(it) },
                    { error: IOException -> continuation.resumeWithException(error) }
                )
            }
            return VoskSpeechRecognizer(model, vocabulary)
        }
    }
}
