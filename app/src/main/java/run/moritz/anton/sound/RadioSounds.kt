package run.moritz.anton.sound

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Handler
import android.os.Looper
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** The sounds of the radio. None plays while the microphone is open, so they never reach the recognizer. */
interface RadioSounds {
    fun keyReleased()

    fun confirmed()

    fun rejected()

    fun promoted()
}

object SilentRadio : RadioSounds {
    override fun keyReleased() = Unit

    override fun confirmed() = Unit

    override fun rejected() = Unit

    override fun promoted() = Unit
}

/** Synthesizes every sound, so the app ships no audio files. */
class SynthesizedRadio : RadioSounds {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val squelch by lazy { noiseBurst(seconds = 0.11, level = 0.13) }
    private val confirm by lazy { tones(listOf(880.0 to 0.07, 1320.0 to 0.09), level = 0.22) }
    private val reject by lazy { tones(listOf(330.0 to 0.13, 262.0 to 0.22), level = 0.28) }
    private val promote by lazy {
        tones(listOf(523.0 to 0.12, 659.0 to 0.12, 784.0 to 0.12, 1047.0 to 0.32), level = 0.26)
    }

    override fun keyReleased() = play { squelch }

    override fun confirmed() = play { confirm }

    override fun rejected() = play { reject }

    override fun promoted() = play { promote }

    private fun play(samples: () -> ShortArray) {
        scope.launch {
            val data = samples()
            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setTransferMode(AudioTrack.MODE_STATIC)
                .setBufferSizeInBytes(data.size * Short.SIZE_BYTES)
                .build()
            track.write(data, 0, data.size)
            track.setNotificationMarkerPosition(data.size)
            track.setPlaybackPositionUpdateListener(
                object : AudioTrack.OnPlaybackPositionUpdateListener {
                    override fun onMarkerReached(finished: AudioTrack) = finished.release()

                    override fun onPeriodicNotification(track: AudioTrack) = Unit
                },
                Handler(Looper.getMainLooper())
            )
            track.play()
        }
    }

    private fun tones(notes: List<Pair<Double, Double>>, level: Double): ShortArray =
        notes.flatMap { (frequency, seconds) -> tone(frequency, seconds, level) }.toShortArray()

    private fun tone(frequency: Double, seconds: Double, level: Double): List<Short> {
        val count = (seconds * SAMPLE_RATE).toInt()
        return List(count) { index ->
            val time = index.toDouble() / SAMPLE_RATE
            val attack = (index / ATTACK_SAMPLES).coerceAtMost(1.0)
            val release = ((count - index) / RELEASE_SAMPLES).coerceAtMost(1.0)
            (sin(2 * PI * frequency * time) * attack * release * level * Short.MAX_VALUE).toInt().toShort()
        }
    }

    private fun noiseBurst(seconds: Double, level: Double): ShortArray {
        val random = Random(SQUELCH_SEED)
        val count = (seconds * SAMPLE_RATE).toInt()
        return ShortArray(count) { index ->
            val decay = exp(-SQUELCH_DECAY * index / count)
            ((random.nextDouble() * 2 - 1) * decay * level * Short.MAX_VALUE).toInt().toShort()
        }
    }

    private companion object {
        const val SAMPLE_RATE = 22_050
        const val ATTACK_SAMPLES = 80.0
        const val RELEASE_SAMPLES = 400.0
        const val SQUELCH_SEED = 5
        const val SQUELCH_DECAY = 4.0
    }
}
