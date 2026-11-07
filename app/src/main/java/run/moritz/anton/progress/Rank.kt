package run.moritz.anton.progress

enum class Metal { BLUE, SILVER, GOLD }

/** What a Berlin Schutzpolizei shoulder board shows. */
data class Insignia(val metal: Metal, val stars: Int = 0, val stripe: Boolean = false, val wreath: Boolean = false)

/** The player's permanent progress level, in promotion order. */
enum class Rank(val insignia: Insignia) {
    IN_TRAINING(Insignia(Metal.BLUE, stripe = true)),
    POLICE_MASTER(Insignia(Metal.BLUE, stars = 2)),
    SENIOR_POLICE_MASTER(Insignia(Metal.BLUE, stars = 3)),
    INSPECTOR_CANDIDATE(Insignia(Metal.SILVER, stripe = true)),
    INSPECTOR(Insignia(Metal.SILVER, stars = 1)),
    SENIOR_INSPECTOR(Insignia(Metal.SILVER, stars = 2)),
    CHIEF_INSPECTOR(Insignia(Metal.SILVER, stars = 3)),
    CHIEF_INSPECTOR_A12(Insignia(Metal.SILVER, stars = 4)),
    FIRST_CHIEF_INSPECTOR(Insignia(Metal.SILVER, stars = 5)),
    FIRST_CHIEF_INSPECTOR_ALLOWANCE(Insignia(Metal.SILVER, stars = 6)),
    COUNCIL_TRAINEE(Insignia(Metal.GOLD, stripe = true)),
    COUNCIL(Insignia(Metal.GOLD, stars = 1)),
    SENIOR_COUNCIL(Insignia(Metal.GOLD, stars = 2)),
    DIRECTOR(Insignia(Metal.GOLD, stars = 3)),
    LEADING_DIRECTOR(Insignia(Metal.GOLD, stars = 4)),
    BERLIN_DIRECTOR(Insignia(Metal.GOLD, stars = 1, wreath = true)),
    FIRST_BERLIN_DIRECTOR(Insignia(Metal.GOLD, stars = 2, wreath = true)),
    VICE_PRESIDENT(Insignia(Metal.GOLD, stars = 3, wreath = true)),
    PRESIDENT(Insignia(Metal.GOLD, stars = 4, wreath = true))
    ;

    val next: Rank? get() = entries.getOrNull(ordinal + 1)

    /** Error-free cases until the next rank, or null on the last rank. */
    val casesToNext: Int? get() = next?.let { minOf(CASES_PER_STEP * (ordinal + 1), MAX_CASES_PER_RANK) }

    /** The hardest material this rank works on, from 1 to [MAX_MATERIAL_LEVEL]. */
    val materialLevel: Int get() = minOf(ordinal + 1, MAX_MATERIAL_LEVEL)

    val unlocksMaterial: Boolean get() = ordinal in 1 until MAX_MATERIAL_LEVEL

    private companion object {
        const val CASES_PER_STEP = 5
        const val MAX_CASES_PER_RANK = 50
        const val MAX_MATERIAL_LEVEL = 4
    }
}
