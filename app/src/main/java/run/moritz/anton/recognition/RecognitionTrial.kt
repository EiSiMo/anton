package run.moritz.anton.recognition

import run.moritz.anton.spelling.SpellingEntry
import run.moritz.anton.spelling.SpellingTable

/** One thing the speaker is asked to say and the symbols it must decode to. */
data class TrialPrompt(val text: String, val expected: List<String>)

/** Prompts that measure recognition on a real device: every spoken form, then realistic transmissions. */
fun recognitionTrial(table: SpellingTable): List<TrialPrompt> {
    val single = table.entries.map { TrialPrompt(it.word, listOf(it.symbol)) }.shuffled()
    val spelled = listOf("KOWALCZYK", "HAMBURG", "QUAX", "ZYXWV")
        .map { name -> name.map { letter -> table.letters.first { it.symbol == letter.toString() } } }
        .map { it.toPrompt() }
    val digits = "03121987".map { digit -> table.digits.first { it.symbol == digit.toString() } }.toPrompt()
    val namedFirst = TrialPrompt("Kowalczyk, Kaufmann, Otto, Wilhelm", listOf("K", "O", "W"))
    val wrongDigit = TrialPrompt("zwei", listOf("zwei"))
    return single + spelled + digits + namedFirst + wrongDigit
}

private fun List<SpellingEntry>.toPrompt() = TrialPrompt(joinToString(", ") { it.word }, map { it.symbol })
