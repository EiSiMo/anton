package run.moritz.anton.spelling

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import run.moritz.anton.cases.Field
import run.moritz.anton.cases.FieldKind

class SpellingEvaluatorTest {
    private val evaluator = SpellingEvaluator(SpellingTable.classicGerman)

    private fun surname(text: String) = Field(FieldKind.SURNAME, text)

    @Test
    fun `a field spelled letter by letter is correct`() {
        val result = evaluator.evaluate(surname("Otto"), listOf("O", "T", "T", "O"))
        assertEquals(Evaluation.Correct(listOf("O", "T", "T", "O")), result)
    }

    @Test
    fun `ch and sch may be one word or letter by letter`() {
        val field = surname("Schach")
        val joined = evaluator.evaluate(field, listOf("Sch", "A", "Ch"))
        val spelledOut = evaluator.evaluate(field, listOf("S", "C", "H", "A", "C", "H"))
        assertTrue(joined is Evaluation.Correct)
        assertTrue(spelledOut is Evaluation.Correct)
    }

    @Test
    fun `a wrong word reports position, sent word and expected word`() {
        val result = evaluator.evaluate(surname("Kowalczyk"), listOf("K", "O", "W", "A", "L", "C", "S"))
        assertEquals(
            Evaluation.Wrong(
                position = 6,
                confirmed = listOf("K", "O", "W", "A", "L", "C"),
                sentWord = "Samuel",
                expectedSymbol = "Z",
                expectedWord = "Zacharias"
            ),
            result
        )
    }

    @Test
    fun `a wrong word at a sch offers the joined word`() {
        val result = evaluator.evaluate(surname("Schulz"), listOf("A"))
        assertEquals("Schule", (result as Evaluation.Wrong).expectedWord)
    }

    @Test
    fun `zwei is a wrong word for the digit two`() {
        val field = Field(FieldKind.BIRTH_DATE, "02.01.1990")
        val result = evaluator.evaluate(field, listOf("0", "zwei"))
        assertEquals("zwei", (result as Evaluation.Wrong).sentWord)
        assertEquals("zwo", result.expectedWord)
    }

    @Test
    fun `dots of a date and the hyphen and space of a plate are silent`() {
        val date = evaluator.evaluate(Field(FieldKind.BIRTH_DATE, "03.12.1987"), "03121987".map { it.toString() })
        val plate = evaluator.evaluate(
            Field(FieldKind.PLATE, "B-AK 12"),
            listOf("B", "A", "K", "1", "2")
        )
        assertTrue(date is Evaluation.Correct)
        assertTrue(plate is Evaluation.Correct)
    }

    @Test
    fun `the hyphen of a double name is spoken`() {
        val field = surname("Abt-Ott")
        val spoken = evaluator.evaluate(field, listOf("A", "B", "T", "-", "O", "T", "T"))
        val silent = evaluator.evaluate(field, listOf("A", "B", "T", "O", "T", "T"))
        assertTrue(spoken is Evaluation.Correct)
        assertEquals("Bindestrich", (silent as Evaluation.Wrong).expectedWord)
    }

    @Test
    fun `spaces in a name are silent and eszett is its own letter`() {
        val spaced = evaluator.evaluate(surname("de Voß"), listOf("D", "E", "V", "O", "ß"))
        assertTrue(spaced is Evaluation.Correct)
    }

    @Test
    fun `a spoken name that is itself a spelling word is dropped`() {
        val result = evaluator.evaluate(surname("Otto"), listOf("O", "O", "T", "T", "O"))
        assertEquals(Evaluation.Correct(listOf("O", "T", "T", "O")), result)
    }

    @Test
    fun `a spoken name is not dropped from a plate`() {
        val result = evaluator.evaluate(Field(FieldKind.PLATE, "B-A 1"), listOf("O", "B", "A", "1"))
        assertTrue(result is Evaluation.Wrong)
    }

    @Test
    fun `an error after a spoken name is reported at its real position`() {
        val result = evaluator.evaluate(surname("Otto"), listOf("O", "O", "T", "B", "O"))
        assertEquals(2, (result as Evaluation.Wrong).position)
        assertEquals("Berta", result.sentWord)
    }

    @Test
    fun `nothing recognized is not understood`() {
        assertEquals(Evaluation.NotUnderstood, evaluator.evaluate(surname("Otto"), emptyList()))
    }

    @Test
    fun `a transmission that stops early is not understood`() {
        assertEquals(Evaluation.NotUnderstood, evaluator.evaluate(surname("Otto"), listOf("O", "T")))
    }

    @Test
    fun `a word after the end of the field is wrong`() {
        val result = evaluator.evaluate(surname("Ida"), listOf("I", "D", "A", "B"))
        assertEquals(3, (result as Evaluation.Wrong).position)
        assertEquals("Berta", result.sentWord)
        assertEquals(null, result.expectedWord)
    }
}
