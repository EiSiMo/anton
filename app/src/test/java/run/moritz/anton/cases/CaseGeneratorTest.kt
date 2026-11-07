package run.moritz.anton.cases

import java.time.LocalDate
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import run.moritz.anton.progress.Rank
import run.moritz.anton.spelling.Evaluation
import run.moritz.anton.spelling.SpellingEvaluator
import run.moritz.anton.spelling.SpellingTable

class CaseGeneratorTest {
    private val table = SpellingTable.classicGerman
    private val today = LocalDate.of(2026, 11, 7)

    private fun generator(seed: Int = 1) = CaseGenerator(table, Random(seed)) { today }

    @Test
    fun `an ID card has its fields in the fixed order`() {
        val generator = generator()
        val card = generateSequence { generator.next(Rank.IN_TRAINING, emptyMap()) }
            .filterIsInstance<Case.IdCard>().first()
        assertEquals(
            listOf(FieldKind.SURNAME, FieldKind.BIRTH_DATE, FieldKind.FIRST_NAME),
            card.fields.map { it.kind }
        )
    }

    @Test
    fun `cases mix ID cards and plates`() {
        val generator = generator()
        val cases = List(200) { generator.next(Rank.IN_TRAINING, emptyMap()) }
        assertTrue(cases.any { it is Case.IdCard })
        assertTrue(cases.any { it is Case.Plate })
    }

    @Test
    fun `people are adults and their ID cards are valid`() {
        val generator = generator()
        repeat(300) {
            val card = generator.next(Rank.entries.last(), emptyMap()) as? Case.IdCard ?: return@repeat
            assertTrue(card.person.birthDate.isBefore(today.minusYears(18)))
            assertTrue(card.expiryDate.isAfter(today))
        }
    }

    @Test
    fun `every field of every level can be spelled with the table`() {
        val evaluator = SpellingEvaluator(table)
        val generator = generator()
        val symbols = table.entries.map { it.symbol }
        Rank.entries.forEach { rank ->
            repeat(120) {
                generator.next(rank, emptyMap()).fields.forEach { field ->
                    val spelled = field.spoken.map { char ->
                        symbols.first { it.equals(char.toString(), ignoreCase = true) }
                    }
                    assertTrue(field.text, evaluator.evaluate(field, spelled) is Evaluation.Correct)
                }
            }
        }
    }

    @Test
    fun `beginners only get short simple material`() {
        val generator = generator()
        repeat(300) {
            val case = generator.next(Rank.IN_TRAINING, emptyMap())
            case.fields.filter { it.kind == FieldKind.SURNAME }.forEach { field ->
                assertTrue(field.text, field.text.length <= 8 && field.text.none { it in "ÄÖÜäöüß-" })
            }
        }
    }

    @Test
    fun `material favours letters with low mastery`() {
        val weakWh = table.entries.associate { it.symbol to 5 } + mapOf("W" to 0, "H" to 0)
        fun whShare(mastery: Map<String, Int>): Int {
            val generator = generator(7)
            return List(400) { generator.next(Rank.POLICE_MASTER, mastery) }
                .sumOf { case -> case.fields.sumOf { field -> field.spoken.count { it in "WwHh" } } }
        }
        assertTrue(whShare(weakWh) > whShare(emptyMap()))
    }

    @Test
    fun `no case repeats shortly after itself`() {
        val generator = generator()
        val surnames = List(8) { generator.next(Rank.IN_TRAINING, emptyMap()).fields.first().text }
        assertEquals(surnames.size, surnames.toSet().size)
    }

    @Test
    fun `a drill keeps the wanted symbol in every field`() {
        listOf("Q", "ß", "Sch", "7", "-").forEach { symbol ->
            val drill = generator().drill(symbol)
            assertEquals(CaseGenerator.DRILL_LENGTH, drill.fields.size)
            drill.fields.forEach { field ->
                assertTrue(field.text, field.text.contains(symbol, ignoreCase = true))
            }
        }
    }
}
