package run.moritz.anton.progress

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

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
        val before = Progress(casesTowardNextRank = Rank.IN_TRAINING.casesToNext!! - 1)
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
    fun `mastery rises with confirmed letters and is capped`() {
        val progress = (1..7).fold(Progress()) { state, _ -> state.after(ProgressEvent.Confirmed(listOf("Q"))) }
        assertEquals(Progress.MAX_MASTERY, progress.masteryOf("Q"))
    }

    @Test
    fun `mastery falls faster than it rises and stops at zero`() {
        val rising = Progress().after(ProgressEvent.Confirmed(listOf("Q", "Q", "Q")))
        assertEquals(1, rising.after(ProgressEvent.Missed("Q")).masteryOf("Q"))
        assertEquals(0, Progress().after(ProgressEvent.Missed("Q")).masteryOf("Q"))
    }

    @Test
    fun `material gets harder with rank`() {
        assertEquals(1, Rank.IN_TRAINING.materialLevel)
        assertEquals(2, Rank.POLICE_MASTER.materialLevel)
        assertEquals(3, Rank.SENIOR_POLICE_MASTER.materialLevel)
        assertEquals(4, Rank.entries.last().materialLevel)
        assertTrue(Rank.POLICE_MASTER.unlocksMaterial)
        assertFalse(Rank.entries.last().unlocksMaterial)
    }

    @Test
    fun `every rank has the insignia from the Berlin table`() {
        assertEquals(19, Rank.entries.size)
        assertEquals(Insignia(Metal.BLUE, stripe = true), Rank.IN_TRAINING.insignia)
        assertEquals(Insignia(Metal.BLUE, stars = 2), Rank.POLICE_MASTER.insignia)
        assertEquals(Insignia(Metal.SILVER, stars = 6), Rank.FIRST_CHIEF_INSPECTOR_ALLOWANCE.insignia)
        assertEquals(Insignia(Metal.GOLD, stars = 4, wreath = true), Rank.entries.last().insignia)
    }
}
