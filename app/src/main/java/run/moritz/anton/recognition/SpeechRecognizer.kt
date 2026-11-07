package run.moritz.anton.recognition

/** Turns one push-to-talk transmission into recognized text. */
interface SpeechRecognizer {
    fun startTransmission()

    /** Ends the transmission and returns everything that was recognized in it. */
    suspend fun finishTransmission(): String
}
