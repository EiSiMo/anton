package run.moritz.anton.cases

import java.io.File
import java.time.LocalDate
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import run.moritz.anton.progress.Progress
import run.moritz.anton.progress.ProgressEvent
import run.moritz.anton.progress.Rank
import run.moritz.anton.spelling.Evaluation
import run.moritz.anton.spelling.SpellingEvaluator
import run.moritz.anton.spelling.SpellingTable

class CaseGeneratorTest {
    private val table = SpellingTable.classicGerman
    private val today = LocalDate.of(2026, 11, 7)
    private val persons = PersonCatalog.parse(File("src/main/assets/persons/persons.json").readText())

    private fun generator(seed: Int = 1) = CaseGenerator(table, persons, Random(seed)) { today }

    private fun CaseGenerator.next(rank: Rank) = next(Progress(rank = rank), 0)

    @Test
    fun `all persons are in the catalog and have a portrait`() {
        assertEquals(80, persons.size)
        persons.forEach { person ->
            assertTrue(person.portrait, File("src/main/assets/persons/portraits/${person.portrait}").exists())
        }
    }

    @Test
    fun `ID cards and driving licences have their fields in the fixed order`() {
        val generator = generator()
        val documents = List(200) { generator.next(Rank.POLICE_MASTER_CANDIDATE) }.filter { it !is Case.Plate }
        assertTrue(documents.any { it is Case.IdCard })
        assertTrue(documents.any { it is Case.DrivingLicence })
        documents.forEach { document ->
            assertEquals(
                listOf(FieldKind.SURNAME, FieldKind.BIRTH_DATE, FieldKind.FIRST_NAME),
                document.fields.map { it.kind }
            )
        }
    }

    @Test
    fun `cases mix documents and plates`() {
        val generator = generator()
        assertTrue(List(200) { generator.next(Rank.POLICE_MASTER_CANDIDATE) }.any { it is Case.Plate })
    }

    @Test
    fun `a driving licence is valid and issued after the eighteenth birthday`() {
        val generator = generator()
        repeat(300) {
            val licence = generator.next(Rank.entries.last()) as? Case.DrivingLicence ?: return@repeat
            assertTrue(licence.expiryDate.isAfter(today))
            assertTrue(licence.issueDate.isAfter(licence.person.birthDate.plusYears(18)))
            assertFalse(licence.issueDate.isAfter(today))
        }
    }

    @Test
    fun `every field of every rank can be spelled with the table`() {
        val evaluator = SpellingEvaluator(table)
        val generator = generator()
        val symbols = table.entries.map { it.symbol }
        Rank.entries.forEach { rank ->
            repeat(120) {
                generator.next(rank).fields.forEach { field ->
                    val spelled = field.spoken.map { char ->
                        symbols.first { it.equals(char.toString(), ignoreCase = true) }
                    }
                    assertTrue(field.text, evaluator.evaluate(field, spelled) is Evaluation.Correct)
                }
            }
        }
    }

    @Test
    fun `beginners only get persons of the first level`() {
        val generator = generator()
        val simple = persons.filter { it.level == 1 }.map { it.surname }
        repeat(300) {
            val person = generator.next(Rank.POLICE_MASTER_CANDIDATE).person ?: return@repeat
            assertTrue(person.surname, person.surname in simple)
        }
    }

    @Test
    fun `plates always separate the district from the rest`() {
        val generator = generator()
        repeat(300) {
            val plate = generator.next(Rank.entries.last()) as? Case.Plate ?: return@repeat
            assertEquals("${plate.district}-${plate.letters}${plate.digits}", plate.fields.single().spoken)
        }
    }

    @Test
    fun `material favours symbols that are due`() {
        val now = 1_000_000L
        val fresh = Progress(rank = Rank.SENIOR_POLICE_MASTER)
        val safe = table.entries.fold(fresh) { state, entry ->
            state.after(ProgressEvent.Confirmed(listOf(entry.symbol)), 0)
        }.copy(dueAt = table.entries.associate { it.symbol to Long.MAX_VALUE })
        val weak = safe.after(ProgressEvent.Missed("Z"), now).after(ProgressEvent.Missed("Y"), now)

        fun share(progress: Progress): Int {
            val generator = generator(7)
            return List(400) { generator.next(progress, now + 1) }
                .sumOf { case -> case.fields.sumOf { field -> field.spoken.count { it in "ZzYy" } } }
        }
        assertTrue(share(weak) > share(safe))
    }

    @Test
    fun `no case repeats shortly after itself`() {
        val generator = generator()
        val first = List(8) { generator.next(Rank.POLICE_MASTER_CANDIDATE).fields.first().text }
        assertEquals(first.size, first.toSet().size)
    }

    @Test
    fun `a drill keeps the wanted symbol in every field`() {
        table.entries.forEach { entry ->
            val drill = generator().drill(entry.symbol)
            assertEquals(CaseGenerator.DRILL_LENGTH, drill.fields.size)
            drill.fields.forEach { field ->
                assertTrue(field.text, field.text.contains(entry.symbol, ignoreCase = true))
            }
        }
    }
}
