package run.moritz.anton.progress

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

private const val MINUTE = 60_000L
private const val YEAR = 365 * 24 * 60 * MINUTE

class ProgressTest {
    @Test
    fun `an error-free case extends the streak and fills the bar`() {
        val progress = Progress().after(ProgressEvent.CaseFinished(errorFree = true))
        assertEquals(1, progress.streak)
        assertEquals(1, progress.casesTowardNextRank)
        assertEquals(1, progress.casesCompleted)
    }

    @Test
    fun `a case with errors counts as completed but fills nothing`() {
        val progress = Progress().after(ProgressEvent.CaseFinished(errorFree = false))
        assertEquals(0, progress.casesTowardNextRank)
        assertEquals(1, progress.casesCompleted)
    }

    @Test
    fun `filling the bar reaches the next rank and empties the bar`() {
        val before = Progress(casesTowardNextRank = Rank.POLICE_MASTER_CANDIDATE.casesToNext!! - 1)
        val after = before.after(ProgressEvent.CaseFinished(errorFree = true))
        assertEquals(Rank.POLICE_MASTER, after.rank)
        assertEquals(0, after.casesTowardNextRank)
    }

    @Test
    fun `ranks and the bar never drop when the streak breaks`() {
        val before = Progress(rank = Rank.POLICE_MASTER, casesTowardNextRank = 4, streak = 9)
        val after = before.after(ProgressEvent.StreakBroken)
        assertEquals(0, after.streak)
        assertEquals(Rank.POLICE_MASTER, after.rank)
        assertEquals(4, after.casesTowardNextRank)
    }

    @Test
    fun `the best streak survives a broken streak`() {
        val before = Progress(streak = 9, bestStreak = 9)
        assertEquals(9, before.after(ProgressEvent.StreakBroken).bestStreak)
    }

    @Test
    fun `the last rank has no next rank and stays full`() {
        val before = Progress(rank = Rank.entries.last())
        val after = before.after(ProgressEvent.CaseFinished(errorFree = true))
        assertEquals(Rank.entries.last(), after.rank)
        assertEquals(null, after.rank.next)
    }

    @Test
    fun `mastery rises with reviews that are due and is capped`() {
        val progress = (1..7).fold(Progress()) { state, review ->
            state.after(ProgressEvent.Confirmed(listOf("Q")), review * YEAR)
        }
        assertEquals(Progress.MAX_MASTERY, progress.masteryOf("Q"))
    }

    @Test
    fun `reviewing before the due date does not raise mastery`() {
        val once = Progress().after(ProgressEvent.Confirmed(listOf("Q", "Q", "Q")), 0)
        assertEquals(1, once.masteryOf("Q"))
        val soon = once.after(ProgressEvent.Confirmed(listOf("Q")), MINUTE)
        assertEquals(1, soon.masteryOf("Q"))
    }

    @Test
    fun `the interval grows with mastery`() {
        val first = Progress().after(ProgressEvent.Confirmed(listOf("Q")), 0)
        val firstInterval = first.dueAt.getValue("Q")
        val second = first.after(ProgressEvent.Confirmed(listOf("Q")), firstInterval)
        val secondInterval = second.dueAt.getValue("Q") - firstInterval
        assertTrue(secondInterval > firstInterval)
    }

    @Test
    fun `a miss costs two levels and brings the symbol back at once`() {
        val learned = (1..3).fold(Progress()) { state, review ->
            state.after(ProgressEvent.Confirmed(listOf("Q")), review * YEAR)
        }
        val missed = learned.after(ProgressEvent.Missed("Q"), 10 * YEAR)
        assertEquals(1, missed.masteryOf("Q"))
        assertEquals(10 * YEAR, missed.dueAt.getValue("Q"))
        assertEquals(0, Progress().after(ProgressEvent.Missed("Q"), 0).masteryOf("Q"))
    }

    @Test
    fun `due symbols are the ones that were seen and whose date has come`() {
        val progress = Progress()
            .after(ProgressEvent.Confirmed(listOf("Q")), 0)
            .after(ProgressEvent.Missed("X"), 0)
        assertEquals(listOf("X"), progress.dueSymbols(1))
        assertEquals(setOf("Q", "X"), progress.dueSymbols(YEAR).toSet())
    }

    @Test
    fun `a due symbol is more urgent than a new one and a new one more than a safe one`() {
        val progress = Progress()
            .after(ProgressEvent.Confirmed(listOf("Q")), 0)
            .after(ProgressEvent.Missed("X"), 0)
        assertTrue(progress.urgency("X", 1) > progress.urgency("A", 1))
        assertTrue(progress.urgency("A", 1) > progress.urgency("Q", 1))
    }

    @Test
    fun `material gets harder with rank`() {
        assertEquals(1, Rank.POLICE_MASTER_CANDIDATE.materialLevel)
        assertEquals(2, Rank.POLICE_MASTER.materialLevel)
        assertEquals(3, Rank.SENIOR_POLICE_MASTER.materialLevel)
        assertEquals(4, Rank.entries.last().materialLevel)
        assertTrue(Rank.POLICE_MASTER.unlocksMaterial)
        assertFalse(Rank.entries.last().unlocksMaterial)
    }

    @Test
    fun `every rank has the insignia from the Berlin table`() {
        assertEquals(19, Rank.entries.size)
        assertEquals(Insignia(Metal.BLUE, stripe = true), Rank.POLICE_MASTER_CANDIDATE.insignia)
        assertEquals(Insignia(Metal.BLUE, stars = 2), Rank.POLICE_MASTER.insignia)
        assertEquals(Insignia(Metal.SILVER, stars = 6), Rank.FIRST_CHIEF_INSPECTOR_ALLOWANCE.insignia)
        assertEquals(Insignia(Metal.GOLD, stars = 4, wreath = true), Rank.entries.last().insignia)
    }
}
