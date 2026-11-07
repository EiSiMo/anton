package run.moritz.anton.cases

import java.time.LocalDate
import kotlin.random.Random
import run.moritz.anton.progress.Progress
import run.moritz.anton.progress.Rank
import run.moritz.anton.spelling.SpellingTable

/** Chooses the next case from the player's rank and which symbols are due for review. */
class CaseGenerator(
    private val table: SpellingTable,
    private val persons: List<Person>,
    private val random: Random = Random.Default,
    private val today: () -> LocalDate = LocalDate::now
) {
    private val recent = ArrayDeque<String>()
    private val symbolsByLength = table.entries.map { it.symbol.lowercase() }.sortedByDescending { it.length }

    /** Of several candidates, the one with the most urgent symbols wins. */
    fun next(progress: Progress, nowMillis: Long): Case {
        val candidates = List(CANDIDATES) { candidate(progress.rank) }.filterNot { signature(it) in recent }
        val case = candidates.maxByOrNull { urgency(it, progress, nowMillis) } ?: candidate(progress.rank)
        recent.addLast(signature(case))
        if (recent.size > RECENT_MEMORY) recent.removeFirst()
        return case
    }

    fun drill(symbol: String): Case.Drill {
        val fields = when {
            symbol.length == 1 && symbol[0] in '0'..'9' -> List(DRILL_LENGTH) { digitsContaining(symbol) }
            else -> namesContaining(symbol).shuffled(random).paddedWith(symbol).take(DRILL_LENGTH)
        }
        return Case.Drill(symbol, fields.map { Field(FieldKind.DRILL, it) })
    }

    private fun candidate(rank: Rank): Case {
        val level = levelFor(rank)
        val roll = random.nextFloat()
        return when {
            roll < ID_CARD_SHARE -> idCard(personOf(level))
            roll < ID_CARD_SHARE + LICENCE_SHARE -> drivingLicence(personOf(level))
            else -> plate(Material.plateLevels[level - 1])
        }
    }

    private fun personOf(level: Int): Person = persons.filter { it.level == level }.random(random)

    /** The newest unlocked level comes up more often the higher the rank climbs. */
    private fun levelFor(rank: Rank): Int {
        val top = rank.materialLevel
        val topWeight = 2 + rank.ordinal
        val roll = random.nextInt(top - 1 + topWeight)
        return if (roll < top - 1) roll + 1 else top
    }

    private fun idCard(person: Person) = Case.IdCard(
        person = person,
        documentNumber = randomString(DOCUMENT_ALPHABET, DOCUMENT_NUMBER_LENGTH),
        accessNumber = randomString(DIGITS, ACCESS_NUMBER_LENGTH)
    )

    private fun drivingLicence(person: Person): Case.DrivingLicence {
        val earliest = person.birthDate.plusYears(LICENCE_MIN_AGE).plusDays(1)
        val latest = today()
        val yearsBack = random.nextLong(0, LICENCE_VALIDITY_YEARS - 1)
        val issued = latest.minusYears(yearsBack).minusDays(random.nextLong(0, DAYS_IN_YEAR))
        val issueDate = if (issued.isBefore(earliest)) earliest else issued
        return Case.DrivingLicence(
            person = person,
            issueDate = issueDate,
            expiryDate = issueDate.plusYears(LICENCE_VALIDITY_YEARS).minusDays(1),
            authority = Material.licenceAuthorities.random(random),
            number = randomString(LICENCE_ALPHABET, LICENCE_NUMBER_LENGTH),
            classes = Material.licenceClasses.random(random)
        )
    }

    private fun plate(level: PlateLevel): Case.Plate {
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

    private fun urgency(case: Case, progress: Progress, nowMillis: Long): Double {
        val symbols = case.fields.flatMap { symbolsOf(it.spoken) }
        if (symbols.isEmpty()) return 0.0
        val noise = random.nextDouble() * URGENCY_NOISE
        return symbols.sumOf { progress.urgency(it, nowMillis) }.toDouble() / symbols.size + noise
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

    private fun namesContaining(symbol: String): List<String> = persons.flatMap {
        listOf(it.surname, it.firstName)
    }.distinct().filter { it.contains(symbol, ignoreCase = true) }

    private fun digitsContaining(digit: String): String {
        val digits = randomString(DIGITS, DRILL_DIGITS - 1)
        val position = random.nextInt(DRILL_DIGITS)
        return digits.substring(0, position) + digit + digits.substring(position)
    }

    /** Few names hold rare letters such as a capital Q, so those fill up with made-up groups around the letter. */
    private fun List<String>.paddedWith(symbol: String): List<String> {
        val missing = (DRILL_LENGTH - size).coerceAtLeast(0)
        return this + List(missing) {
            randomString(FILLER_ALPHABET, 1) + symbol.lowercase() + randomString(FILLER_ALPHABET, 1).lowercase()
        }
    }

    private fun randomString(alphabet: String, length: Int) = String(CharArray(length) { alphabet.random(random) })

    private fun signature(case: Case) = case.person?.surname ?: case.fields.first().text

    private fun tenToThe(power: Int) = (1..power).fold(1) { value, _ -> value * 10 }

    companion object {
        const val DRILL_LENGTH = 5
        private const val DRILL_DIGITS = 4
        private const val CANDIDATES = 8
        private const val RECENT_MEMORY = 12
        private const val ID_CARD_SHARE = 0.5f
        private const val LICENCE_SHARE = 0.2f
        private const val DAYS_IN_YEAR = 365L
        private const val MAX_PLATE_LENGTH = 8
        private const val DIGITS = "0123456789"
        private const val FILLER_ALPHABET = "ABDEFGKLMNOPRSTU"
        private const val DOCUMENT_ALPHABET = "CFGHJKLMNPRTVWXYZ0123456789"
        private const val DOCUMENT_NUMBER_LENGTH = 9
        private const val ACCESS_NUMBER_LENGTH = 6
        private const val LICENCE_ALPHABET = "ABCDEFGHJKLMNPRSTUVWXYZ0123456789"
        private const val LICENCE_NUMBER_LENGTH = 11
        private const val LICENCE_MIN_AGE = 18L
        private const val LICENCE_VALIDITY_YEARS = 15L
        private const val URGENCY_NOISE = 0.8
    }
}
