package run.moritz.anton.cases

import java.time.LocalDate
import kotlin.random.Random
import run.moritz.anton.progress.Progress
import run.moritz.anton.progress.Rank
import run.moritz.anton.spelling.SpellingTable

/** Chooses the next case from the player's rank and mastery. */
class CaseGenerator(
    private val table: SpellingTable,
    private val random: Random = Random.Default,
    private val today: () -> LocalDate = LocalDate::now
) {
    private val recent = ArrayDeque<String>()
    private val symbolsByLength = table.entries.map { it.symbol.lowercase() }.sortedByDescending { it.length }

    /** The candidate that exercises the weakest letters wins, so weak letters come up more often. */
    fun next(rank: Rank, mastery: Map<String, Int>): Case {
        val candidates = List(CANDIDATES) { candidate(rank) }.filterNot { signature(it) in recent }
        val case = candidates.maxByOrNull { weakness(it, mastery) } ?: candidate(rank)
        recent.addLast(signature(case))
        if (recent.size > RECENT_MEMORY) recent.removeFirst()
        return case
    }

    fun drill(symbol: String): Case.Drill {
        val fields = when {
            symbol.length == 1 && symbol[0] in '0'..'9' -> List(DRILL_LENGTH) { digitsContaining(symbol) }
            else -> namesContaining(symbol).shuffled(random).padded().take(DRILL_LENGTH)
        }
        return Case.Drill(symbol, fields.map { Field(FieldKind.DRILL, it) })
    }

    private fun candidate(rank: Rank): Case {
        val level = Material.levels[levelFor(rank) - 1]
        return if (random.nextFloat() < ID_CARD_SHARE) idCard(level) else plate(level)
    }

    /** The newest unlocked level comes up more often the higher the rank climbs. */
    private fun levelFor(rank: Rank): Int {
        val top = rank.materialLevel
        val topWeight = 2 + rank.ordinal
        val roll = random.nextInt(top - 1 + topWeight)
        return if (roll < top - 1) roll + 1 else top
    }

    private fun idCard(level: MaterialLevel): Case.IdCard {
        val sex = Sex.entries.random(random)
        val firstNames = if (sex == Sex.FEMALE) level.femaleNames else level.maleNames
        val age = random.nextInt(MIN_AGE, MAX_AGE)
        val birthDate = today().minusYears(age.toLong()).minusDays(random.nextLong(1, DAYS_IN_YEAR))
        return Case.IdCard(
            person = Person(
                surname = level.surnames.random(random),
                firstName = firstNames.random(random),
                sex = sex,
                birthDate = birthDate,
                birthPlace = Material.birthPlaces.random(random)
            ),
            expiryDate = today().plusDays(random.nextLong(30, MAX_VALIDITY_DAYS)),
            documentNumber = randomString(DOCUMENT_ALPHABET, DOCUMENT_NUMBER_LENGTH),
            accessNumber = randomString("0123456789", ACCESS_NUMBER_LENGTH),
            portraitSeed = random.nextInt()
        )
    }

    private fun plate(level: MaterialLevel): Case.Plate {
        val district = level.districts.random(random)
        val letterCount = level.serialLetters.random(random)
        val digitCount = level.serialDigits.random(
            random
        ).coerceAtMost(MAX_PLATE_LENGTH - district.length - letterCount)
        val letters = generateSequence { randomString(Material.SERIAL_ALPHABET, letterCount) }
            .first { it !in Material.forbiddenSerials }
        val digits = random.nextInt(1, tenToThe(digitCount)).toString().padStart(digitCount, '1')
        return Case.Plate(district, letters, digits)
    }

    private fun weakness(case: Case, mastery: Map<String, Int>): Double {
        val symbols = case.fields.flatMap { symbolsOf(it.spoken) }
        if (symbols.isEmpty()) return 0.0
        val noise = random.nextDouble() * WEAKNESS_NOISE
        return symbols.sumOf { Progress.MAX_MASTERY - (mastery[it] ?: 0) }.toDouble() / symbols.size + noise
    }

    private fun symbolsOf(spoken: String): List<String> {
        val text = spoken.lowercase()
        val symbols = mutableListOf<String>()
        var position = 0
        while (position < text.length) {
            val symbol = symbolsByLength.firstOrNull { text.startsWith(it, position) }
            if (symbol != null) symbols += table.entries.first { it.symbol.lowercase() == symbol }.symbol
            position += symbol?.length ?: 1
        }
        return symbols
    }

    private fun namesContaining(symbol: String): List<String> {
        val pool = Material.levels.flatMap { it.surnames + it.femaleNames + it.maleNames }
        return pool.filter { it.contains(symbol, ignoreCase = true) }
    }

    private fun digitsContaining(digit: String): String {
        val digits = randomString("0123456789", DRILL_DIGITS - 1)
        val position = random.nextInt(DRILL_DIGITS)
        return digits.substring(0, position) + digit + digits.substring(position)
    }

    private fun List<String>.padded(): List<String> =
        if (size >= DRILL_LENGTH) this else (this + this + this + this + this)

    private fun randomString(alphabet: String, length: Int) = String(CharArray(length) { alphabet.random(random) })

    private fun signature(case: Case) = case.fields.first().text

    private fun tenToThe(power: Int) = (1..power).fold(1) { value, _ -> value * 10 }

    companion object {
        const val DRILL_LENGTH = 5
        private const val DRILL_DIGITS = 4
        private const val CANDIDATES = 6
        private const val RECENT_MEMORY = 24
        private const val ID_CARD_SHARE = 0.7f
        private const val MIN_AGE = 19
        private const val MAX_AGE = 84
        private const val DAYS_IN_YEAR = 365L
        private const val MAX_VALIDITY_DAYS = 3_600L
        private const val MAX_PLATE_LENGTH = 8
        private const val DOCUMENT_ALPHABET = "CFGHJKLMNPRTVWXYZ0123456789"
        private const val DOCUMENT_NUMBER_LENGTH = 9
        private const val ACCESS_NUMBER_LENGTH = 6
        private const val WEAKNESS_NOISE = 0.8
    }
}
