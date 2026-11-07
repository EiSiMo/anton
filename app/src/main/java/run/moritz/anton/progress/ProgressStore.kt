package run.moritz.anton.progress

/** Keeps the player's progress across app starts. */
interface ProgressStore {
    suspend fun load(): Progress

    suspend fun save(progress: Progress)

    suspend fun recordMisrecognition(expected: String?, heard: String)

    suspend fun reset()
}
