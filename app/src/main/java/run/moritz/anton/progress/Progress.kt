package run.moritz.anton.progress

/** Something that happened in a case and changes the player's progress. */
sealed interface ProgressEvent {
    data class Confirmed(val symbols: List<String>) : ProgressEvent

    data class Missed(val symbol: String) : ProgressEvent

    data object StreakBroken : ProgressEvent

    data class CaseFinished(val errorFree: Boolean) : ProgressEvent

    /** The player said the recognizer was wrong. [expected] is what should have been heard, if anything. */
    data class Misrecognized(val expected: String?, val heard: String) : ProgressEvent
}

/** Everything the app remembers about a player. */
data class Progress(
    val rank: Rank = Rank.IN_TRAINING,
    val casesTowardNextRank: Int = 0,
    val streak: Int = 0,
    val bestStreak: Int = 0,
    val casesCompleted: Int = 0,
    val mastery: Map<String, Int> = emptyMap()
) {
    fun masteryOf(symbol: String): Int = mastery[symbol] ?: 0

    fun after(event: ProgressEvent): Progress = when (event) {
        is ProgressEvent.Confirmed -> event.symbols.fold(this) { progress, symbol -> progress.adjust(symbol, 1) }
        is ProgressEvent.Missed -> adjust(event.symbol, -MISS_PENALTY)
        ProgressEvent.StreakBroken -> copy(streak = 0)
        is ProgressEvent.CaseFinished -> finishCase(event.errorFree)
        is ProgressEvent.Misrecognized -> this
    }

    private fun adjust(symbol: String, change: Int): Progress {
        val level = (masteryOf(symbol) + change).coerceIn(0, MAX_MASTERY)
        return copy(mastery = mastery + (symbol to level))
    }

    private fun finishCase(errorFree: Boolean): Progress {
        if (!errorFree) return copy(casesCompleted = casesCompleted + 1)
        val streak = streak + 1
        val filled = casesTowardNextRank + 1
        val promoted = rank.casesToNext?.let { filled >= it } == true
        return copy(
            rank = if (promoted) checkNotNull(rank.next) else rank,
            casesTowardNextRank = if (promoted) 0 else filled,
            streak = streak,
            bestStreak = maxOf(bestStreak, streak),
            casesCompleted = casesCompleted + 1
        )
    }

    companion object {
        const val MAX_MASTERY = 5
        private const val MISS_PENALTY = 2
    }
}
