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
    val rank: Rank = Rank.POLICE_MASTER_CANDIDATE,
    val casesTowardNextRank: Int = 0,
    val streak: Int = 0,
    val bestStreak: Int = 0,
    val casesCompleted: Int = 0,
    val mastery: Map<String, Int> = emptyMap(),
    val dueAt: Map<String, Long> = emptyMap()
) {
    fun masteryOf(symbol: String): Int = mastery[symbol] ?: 0

    /** The symbols waiting for review, the most urgent first. Symbols never practised are not part of it. */
    fun dueSymbols(nowMillis: Long): List<String> =
        dueAt.filterValues { it <= nowMillis }.keys.sortedBy { masteryOf(it) }

    /** How much a case should want this symbol: due ones first, then new ones, then what is safe for now. */
    fun urgency(symbol: String, nowMillis: Long): Int {
        val due = dueAt[symbol] ?: return NEW_SYMBOL_URGENCY
        return if (due <= nowMillis) DUE_URGENCY + MAX_MASTERY - masteryOf(symbol) else 0
    }

    /**
     * Applies an event at [nowMillis]. Mastery works like a Leitner box: a confirmed symbol only moves up when it
     * was due, so repeating it right away proves nothing, and a miss brings it back at once.
     */
    fun after(event: ProgressEvent, nowMillis: Long = 0): Progress = when (event) {
        is ProgressEvent.Confirmed -> event.symbols.fold(this) { progress, symbol ->
            progress.review(symbol, nowMillis)
        }

        is ProgressEvent.Missed -> miss(event.symbol, nowMillis)

        ProgressEvent.StreakBroken -> copy(streak = 0)

        is ProgressEvent.CaseFinished -> finishCase(event.errorFree)

        is ProgressEvent.Misrecognized -> this
    }

    private fun review(symbol: String, nowMillis: Long): Progress {
        val due = dueAt[symbol]
        if (due != null && due > nowMillis) return this
        val level = (masteryOf(symbol) + 1).coerceAtMost(MAX_MASTERY)
        return copy(
            mastery = mastery + (symbol to level),
            dueAt = dueAt + (symbol to nowMillis + INTERVALS_MILLIS[level])
        )
    }

    private fun miss(symbol: String, nowMillis: Long): Progress {
        val level = (masteryOf(symbol) - MISS_PENALTY).coerceAtLeast(0)
        return copy(mastery = mastery + (symbol to level), dueAt = dueAt + (symbol to nowMillis))
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
        private const val NEW_SYMBOL_URGENCY = 4
        private const val DUE_URGENCY = 6
        private const val MINUTE = 60_000L
        private const val DAY = 24 * 60 * MINUTE

        /** The wait until the next review, by mastery level. */
        private val INTERVALS_MILLIS = listOf(0L, 10 * MINUTE, DAY, 3 * DAY, 7 * DAY, 21 * DAY)
    }
}
