package run.moritz.anton.cases

import java.time.LocalDate
import java.time.format.DateTimeFormatter

enum class FieldKind(val mayOpenWithName: Boolean, val keepsHyphen: Boolean) {
    SURNAME(mayOpenWithName = true, keepsHyphen = true),
    BIRTH_DATE(mayOpenWithName = false, keepsHyphen = false),
    FIRST_NAME(mayOpenWithName = true, keepsHyphen = true),
    PLATE(mayOpenWithName = false, keepsHyphen = false),
    DRILL(mayOpenWithName = false, keepsHyphen = true)
}

/** One part of a case that is transmitted on its own. */
data class Field(val kind: FieldKind, val text: String) {
    /** The characters that are actually spoken, in order. Separators such as date dots stay silent. */
    val spoken: String = text.filter { it.isLetterOrDigit() || (it == '-' && kind.keepsHyphen) }
}

enum class Sex { FEMALE, MALE }

/** A fictional person. Any likeness to a real person is coincidental. */
data class Person(
    val surname: String,
    val firstName: String,
    val sex: Sex,
    val birthDate: LocalDate,
    val birthPlace: String
)

/** One task the player works through, field by field. */
sealed interface Case {
    val fields: List<Field>

    data class IdCard(
        val person: Person,
        val expiryDate: LocalDate,
        val documentNumber: String,
        val accessNumber: String,
        val portraitSeed: Int
    ) : Case {
        override val fields = listOf(
            Field(FieldKind.SURNAME, person.surname),
            Field(FieldKind.BIRTH_DATE, person.birthDate.format(DATE_FORMAT)),
            Field(FieldKind.FIRST_NAME, person.firstName)
        )
    }

    data class Plate(val district: String, val letters: String, val digits: String) : Case {
        override val fields = listOf(Field(FieldKind.PLATE, "$district-$letters $digits"))
    }

    /** A short exercise of one [symbol]. It only changes mastery. */
    data class Drill(val symbol: String, override val fields: List<Field>) : Case

    companion object {
        val DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
    }
}
