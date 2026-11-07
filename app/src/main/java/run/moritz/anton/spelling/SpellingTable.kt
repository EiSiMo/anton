package run.moritz.anton.spelling

/** One spoken form: [symbol] is what is meant, [word] is what is said. */
data class SpellingEntry(val symbol: String, val word: String)

/** A spelling alphabet together with the digits and signs spoken over the radio. */
class SpellingTable(val letters: List<SpellingEntry>, val digits: List<SpellingEntry>, val signs: List<SpellingEntry>) {
    val entries: List<SpellingEntry> = letters + digits + signs

    companion object {
        val classicGerman = SpellingTable(
            letters = listOf(
                "A" to "Anton", "Ä" to "Ärger", "B" to "Berta", "C" to "Cäsar", "Ch" to "Charlotte",
                "D" to "Dora", "E" to "Emil", "F" to "Friedrich", "G" to "Gustav", "H" to "Heinrich",
                "I" to "Ida", "J" to "Julius", "K" to "Kaufmann", "L" to "Ludwig", "M" to "Martha",
                "N" to "Nordpol", "O" to "Otto", "Ö" to "Ökonom", "P" to "Paula", "Q" to "Quelle",
                "R" to "Richard", "S" to "Samuel", "Sch" to "Schule", "T" to "Theodor", "U" to "Ulrich",
                "Ü" to "Übermut", "V" to "Viktor", "W" to "Wilhelm", "X" to "Xanthippe", "Y" to "Ypsilon",
                "Z" to "Zacharias", "ß" to "Eszett"
            ).toEntries(),
            digits = listOf(
                "0" to "null", "1" to "eins", "2" to "zwo", "3" to "drei", "4" to "vier",
                "5" to "fünf", "6" to "sechs", "7" to "sieben", "8" to "acht", "9" to "neun"
            ).toEntries(),
            signs = listOf("-" to "Bindestrich").toEntries()
        )

        private fun List<Pair<String, String>>.toEntries() = map { (symbol, word) -> SpellingEntry(symbol, word) }
    }
}
