package run.moritz.anton.cases

import java.time.LocalDate
import java.time.format.DateTimeFormatter

enum class FieldKind(val mayOpenWithName: Boolean, val keepsHyphen: Boolean) {
    SURNAME(mayOpenWithName = true, keepsHyphen = true),
    BIRTH_DATE(mayOpenWithName = false, keepsHyphen = false),
    FIRST_NAME(mayOpenWithName = true, keepsHyphen = true),
    PLATE(mayOpenWithName = false, keepsHyphen = true),
    DRILL(mayOpenWithName = false, keepsHyphen = true)
}

/** One part of a case that is transmitted on its own. */
data class Field(val kind: FieldKind, val text: String) {
    /** The characters that are actually spoken, in order. Separators such as date dots stay silent. */
    val spoken: String = text.filter { it.isLetterOrDigit() || (it == '-' && kind.keepsHyphen) }
}

enum class Sex { FEMALE, MALE }

/** A fictional person from the catalog. [level] is the difficulty of the name. */
data class Person(
    val surname: String,
    val firstName: String,
    val sex: Sex,
    val birthDate: LocalDate,
    val birthPlace: String,
    val expiryDate: LocalDate,
    val level: Int,
    val portrait: String
) {
    /** Surname, date of birth and first name, in the order they are given over the radio. */
    val fields: List<Field> = listOf(
        Field(FieldKind.SURNAME, surname),
        Field(FieldKind.BIRTH_DATE, birthDate.format(Case.DATE_FORMAT)),
        Field(FieldKind.FIRST_NAME, firstName)
    )
}

/** One task the player works through, field by field. */
sealed interface Case {
    val fields: List<Field>

    /** The person whose document this is, if it has one. */
    val person: Person? get() = null

    data class IdCard(override val person: Person, val documentNumber: String, val accessNumber: String) : Case {
        override val fields = person.fields
    }

    data class DrivingLicence(
        override val person: Person,
        val issueDate: LocalDate,
        val expiryDate: LocalDate,
        val authority: String,
        val number: String,
        val classes: String
    ) : Case {
        override val fields = person.fields
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
