package run.moritz.anton.cases

/** The plates of one difficulty level, see the specification. */
internal class PlateLevel(val districts: List<String>, val serialLetters: IntRange, val serialDigits: IntRange)

internal object Material {
    val plateLevels = listOf(
        PlateLevel(
            districts = listOf("B", "M", "K", "F", "S", "D", "H", "HH", "HB", "E", "DO", "L", "N", "P", "BN"),
            serialLetters = 1..2,
            serialDigits = 1..3
        ),
        PlateLevel(
            districts = listOf(
                "GÖ", "LÖ", "TÜ", "WÜ", "FÜ", "MÜ", "BÖ", "DÜW", "SÜW", "KÖT", "SÖM", "HRO", "HGW", "HWI"
            ),
            serialLetters = 1..2,
            serialDigits = 2..4
        ),
        PlateLevel(
            districts = listOf(
                "MYK", "HVL", "OHV", "TF", "LOS", "SPN", "UM", "PR", "MOL", "BAR", "OSL", "EE", "EW", "AIC", "WAF"
            ),
            serialLetters = 1..2,
            serialDigits = 1..4
        ),
        PlateLevel(
            districts = listOf(
                "QLB", "QFT", "KYF", "NVP", "VER", "VK", "ZW", "WW", "ZI", "SYK", "PCH", "VG", "VR", "WSF", "ZEL"
            ),
            serialLetters = 2..2,
            serialDigits = 3..4
        )
    )

    /** Middle letters of a plate. Letters that are too easily confused are not issued. */
    const val SERIAL_ALPHABET = "ABCDEFGHIJKLMNOPRSTUVWXYZ"

    /** Letter combinations that German registration offices do not issue. */
    val forbiddenSerials = setOf("HJ", "KZ", "NS", "SA", "SS")

    val licenceAuthorities = listOf(
        "Stadt Berlin", "Stadt Hamburg", "Stadt Köln", "Stadt Leipzig", "Stadt Kassel", "Stadt Bremen",
        "Landratsamt München", "Landratsamt Starnberg", "Landratsamt Tübingen", "Kreis Pinneberg", "Kreis Mettmann"
    )

    val licenceClasses = listOf("AM/B/L", "AM/B/BE/L", "A/AM/B/L", "AM/B/C1/L", "A/AM/B/BE/C1/L")
}
