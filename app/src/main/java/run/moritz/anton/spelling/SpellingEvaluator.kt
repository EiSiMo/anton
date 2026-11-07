package run.moritz.anton.spelling

import run.moritz.anton.cases.Field

/** The outcome of comparing one transmission with one field. */
sealed interface Evaluation {
    data class Correct(val confirmed: List<String>) : Evaluation

    /**
     * The transmission deviates at [position], counted in spoken characters of the field. [expectedWord] is
     * null when the player went on after the end of the field.
     */
    data class Wrong(
        val position: Int,
        val confirmed: List<String>,
        val sentWord: String,
        val expectedSymbol: String?,
        val expectedWord: String?
    ) : Evaluation

    data object NotUnderstood : Evaluation
}

/** Compares recognized symbols with a field. A transmission that stops early is not an error, just incomplete. */
class SpellingEvaluator(table: SpellingTable) {
    private val wordBySymbol = table.entries.associate { it.symbol to it.word }

    /** Longest symbols first, so that "Sch" wins over "S" when the expected word is shown. */
    private val symbolsByLength = table.entries.sortedByDescending { it.symbol.length }

    fun evaluate(field: Field, symbols: List<String>): Evaluation {
        if (symbols.isEmpty()) return Evaluation.NotUnderstood
        val maxSkip = if (field.kind.mayOpenWithName) minOf(MAX_SPOKEN_NAME_WORDS, symbols.size - 1) else 0
        val walks = (0..maxSkip).map { skip -> walk(field.spoken, symbols.drop(skip)) }
        walks.firstOrNull { it.reachedEnd && it.complete }?.let { return Evaluation.Correct(it.confirmed) }

        val best = walks.first { walk -> walk.position == walks.maxOf { it.position } }
        return when {
            best.complete -> Evaluation.NotUnderstood
            else -> wrong(field, best)
        }
    }

    /** How many leading [symbols] complete the field, or null if they do not. Later symbols belong to what follows. */
    fun symbolsCompleting(field: Field, symbols: List<String>): Int? = (1..symbols.size).firstOrNull { count ->
        evaluate(field, symbols.take(count)) is Evaluation.Correct
    }

    /** The field as joined symbols, the way a player who spells it perfectly would send it. */
    fun symbolsOf(field: Field): List<String> {
        val symbols = mutableListOf<String>()
        var position = 0
        while (position < field.spoken.length) {
            val symbol = expectedSymbolAt(field.spoken, position)
            symbols += symbol
            position += symbol.length
        }
        return symbols
    }

    private fun wrong(field: Field, walk: Walk): Evaluation.Wrong {
        val expected = walk.position.takeIf { it < field.spoken.length }?.let { expectedSymbolAt(field.spoken, it) }
        return Evaluation.Wrong(
            position = walk.position,
            confirmed = walk.confirmed,
            sentWord = wordFor(walk.nextSymbol!!),
            expectedSymbol = expected,
            expectedWord = expected?.let(::wordFor)
        )
    }

    private fun walk(spoken: String, symbols: List<String>): Walk {
        var position = 0
        val confirmed = mutableListOf<String>()
        for (symbol in symbols) {
            if (!spoken.regionMatches(position, symbol, 0, symbol.length, ignoreCase = true)) {
                return Walk(position, position >= spoken.length, confirmed, nextSymbol = symbol)
            }
            position += symbol.length
            confirmed += symbol
        }
        return Walk(position, position >= spoken.length, confirmed, nextSymbol = null)
    }

    private fun expectedSymbolAt(spoken: String, position: Int): String {
        val entry = symbolsByLength.firstOrNull { spoken.regionMatches(position, it.symbol, 0, it.symbol.length, true) }
        return checkNotNull(entry) { "'${spoken[position]}' of '$spoken' is not in the spelling table" }.symbol
    }

    private fun wordFor(symbol: String) = wordBySymbol[symbol] ?: symbol

    private class Walk(
        val position: Int,
        val reachedEnd: Boolean,
        val confirmed: List<String>,
        val nextSymbol: String?
    ) {
        val complete = nextSymbol == null
    }

    private companion object {
        /** A name said before spelling it may be heard as spelling words: one per part of a double name. */
        const val MAX_SPOKEN_NAME_WORDS = 2
    }
}
