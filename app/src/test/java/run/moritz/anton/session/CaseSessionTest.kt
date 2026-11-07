package run.moritz.anton.session

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import run.moritz.anton.cases.Field
import run.moritz.anton.cases.FieldKind
import run.moritz.anton.progress.ProgressEvent
import run.moritz.anton.spelling.Evaluation
import run.moritz.anton.spelling.SpellingEvaluator
import run.moritz.anton.spelling.SpellingTable

class CaseSessionTest {
    private val fields = listOf(Field(FieldKind.SURNAME, "Ott"), Field(FieldKind.FIRST_NAME, "Ida"))
    private val evaluator = SpellingEvaluator(SpellingTable.classicGerman)

    private fun session(drill: Boolean = false) = CaseSession(fields, countsTowardProgress = !drill, evaluator)

    private val ott = listOf("O", "T", "T")
    private val ida = listOf("I", "D", "A")

    @Test
    fun `a correct transmission confirms the field and opens the next`() {
        val session = session()
        val events = session.transmit(ott, "heard")
        assertEquals(1, session.fieldIndex)
        assertEquals(listOf(ProgressEvent.Confirmed(ott)), events)
    }

    @Test
    fun `a case without errors finishes error-free`() {
        val session = session()
        session.transmit(ott, "heard")
        val events = session.transmit(ida, "heard")
        assertTrue(session.finished)
        assertEquals(ProgressEvent.CaseFinished(errorFree = true), events.last())
    }

    @Test
    fun `not understood changes nothing and keeps the case error-free`() {
        val session = session()
        assertEquals(emptyList<ProgressEvent>(), session.transmit(emptyList(), ""))
        assertTrue(session.notUnderstood)
        assertEquals(0, session.fieldIndex)
        session.transmit(ott, "heard")
        assertFalse(session.notUnderstood)
        assertEquals(ProgressEvent.CaseFinished(errorFree = true), session.transmit(ida, "heard").last())
    }

    @Test
    fun `a wrong word waits for the plausibility decision`() {
        val session = session()
        assertEquals(emptyList<ProgressEvent>(), session.transmit(listOf("O", "B"), "otto berta"))
        assertTrue(session.pending is Evaluation.Wrong)
    }

    @Test
    fun `repeating a wrong field breaks the streak and costs the case its error-free state`() {
        val session = session()
        session.transmit(listOf("O", "B"), "otto berta")
        val events = session.repeat()
        assertEquals(listOf(ProgressEvent.Missed("T"), ProgressEvent.StreakBroken), events)
        assertEquals(null, session.pending)
        assertEquals(0, session.fieldIndex)
        session.transmit(ott, "heard")
        assertEquals(ProgressEvent.CaseFinished(errorFree = false), session.transmit(ida, "heard").last())
    }

    @Test
    fun `das war richtig counts the field as correct and records the misrecognition`() {
        val session = session()
        session.transmit(listOf("O", "B"), "otto berta")
        val events = session.overrule()
        assertEquals(
            listOf(
                ProgressEvent.Confirmed(listOf("O", "T", "T")),
                ProgressEvent.Misrecognized(expected = "Theodor", heard = "otto berta")
            ),
            events
        )
        assertEquals(1, session.fieldIndex)
        assertEquals(ProgressEvent.CaseFinished(errorFree = true), session.transmit(ida, "heard").last())
    }

    @Test
    fun `opening the table breaks the streak once and the case is no longer error-free`() {
        val session = session()
        assertEquals(listOf(ProgressEvent.StreakBroken), session.openTable())
        assertEquals(emptyList<ProgressEvent>(), session.openTable())
        session.transmit(ott, "heard")
        assertEquals(ProgressEvent.CaseFinished(errorFree = false), session.transmit(ida, "heard").last())
    }

    @Test
    fun `a drill changes mastery but never the streak or the rank`() {
        val session = session(drill = true)
        session.transmit(listOf("O", "B"), "otto berta")
        assertEquals(listOf(ProgressEvent.Missed("T")), session.repeat())
        assertEquals(emptyList<ProgressEvent>(), session.openTable())
        session.transmit(ott, "heard")
        val events = session.transmit(ida, "heard")
        assertTrue(session.finished)
        assertEquals(listOf(ProgressEvent.Confirmed(ida)), events)
    }

    @Test
    fun `a completed field advances while the transmission goes on`() {
        val session = session()
        val advance = session.advance(ott + listOf("I"))
        assertEquals(3, advance.consumed)
        assertEquals(listOf(ProgressEvent.Confirmed(ott)), advance.events)
        assertEquals(1, session.fieldIndex)
    }

    @Test
    fun `several fields can complete in one go and finish the case`() {
        val session = session()
        val advance = session.advance(ott + ida)
        assertEquals(6, advance.consumed)
        assertTrue(session.finished)
        assertEquals(ProgressEvent.CaseFinished(errorFree = true), advance.events.last())
    }

    @Test
    fun `an unfinished field does not advance and costs nothing`() {
        val session = session()
        val advance = session.advance(listOf("O", "T"))
        assertEquals(0, advance.consumed)
        assertEquals(emptyList<ProgressEvent>(), advance.events)
        assertFalse(session.notUnderstood)
        assertEquals(0, session.fieldIndex)
    }
}
