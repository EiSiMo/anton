package run.moritz.anton.recognition

import run.moritz.anton.spelling.SpellingTable

/**
 * Translates between the spelling table and what the speech model can hear.
 *
 * Words missing from the model are represented by phrases of similar sounding known words. Words that are
 * deliberately wrong, such as "zwei" instead of "zwo", stay in the grammar so they are heard rather than
 * forced onto the nearest correct word.
 */
class RecognitionVocabulary(table: SpellingTable) {
    private val phraseToSymbol: Map<String, String> = buildMap {
        for (entry in table.entries) {
            val word = entry.word.lowercase()
            val phrases = STAND_INS[word] ?: listOf(word)
            phrases.forEach { put(it, entry.symbol) }
        }
        WRONG_WORDS.forEach { put(it, it) }
    }

    private val longestPhrase = phraseToSymbol.keys.maxOf { it.split(" ").size }

    val grammar: List<String> = phraseToSymbol.keys.toList() + UNKNOWN

    /** Turns recognized text into symbols. Unknown speech is dropped. */
    fun decode(recognized: String): List<String> {
        val words = recognized.split(" ").filter { it.isNotBlank() }
        val symbols = mutableListOf<String>()
        var position = 0
        while (position < words.size) {
            val match = (minOf(longestPhrase, words.size - position) downTo 1).firstNotNullOfOrNull { length ->
                phraseToSymbol[words.subList(position, position + length).joinToString(" ")]?.let { it to length }
            }
            if (match == null) {
                position += 1
            } else {
                symbols += match.first
                position += match.second
            }
        }
        return symbols
    }

    private companion object {
        const val UNKNOWN = "[unk]"
        val STAND_INS = mapOf(
            "xanthippe" to listOf("san tippe", "sankt tippe", "xanten tippe", "kanten tippe"),
            "eszett" to listOf("es zett")
        )
        val WRONG_WORDS = listOf("zwei")
    }
}
