package run.moritz.anton.recognition

/** Turns one push-to-talk transmission into recognized text. */
interface SpeechRecognizer {
    /** Opens the microphone. [onPartial] receives everything heard so far, from a background thread. */
    fun startTransmission(onPartial: (String) -> Unit = {})

    /** Ends the transmission and returns everything that was recognized in it. */
    suspend fun finishTransmission(): String
}
