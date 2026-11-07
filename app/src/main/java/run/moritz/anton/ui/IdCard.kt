package run.moritz.anton.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import run.moritz.anton.R
import run.moritz.anton.cases.Case
import run.moritz.anton.cases.FieldKind

private val CardCream = Color(0xFFF4F0DC)
private val CardSand = Color(0xFFE9E3C4)
private val CardRose = Color(0xFFF1E3D6)
private val LabelInk = Color(0xFF26303A)

/** The front of the German ID card as issued since 2024, carrying the fictional data of a catalog person. */
@Composable
fun IdCard(card: Case.IdCard, activeField: FieldKind?, modifier: Modifier = Modifier) {
    val person = card.person
    val birthDate = person.birthDate.format(Case.DATE_FORMAT)
    val description = stringResource(R.string.card_description, person.firstName, person.surname, birthDate)
    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .aspectRatio(85.6f / 54f)
            .clip(RoundedCornerShape(DocumentCorner))
            .border(1.dp, Color(0xFF8C9188), RoundedCornerShape(DocumentCorner))
            .semantics { contentDescription = description }
    ) {
        val u = maxWidth / 100
        CardBackground()
        Header(card, u)
        PersonPortrait(person, at(u, 4f, 15.5f).size(u * 35f, u * 45f))

        val label = printStyle(u, 1.42f, bold = true, color = LabelInk)
        val left = 42.5f
        val value = dataStyle(u, valueSize(person.surname))

        Label(stringResource(R.string.card_surname), at(u, left, 13.4f), label, maxLines = 1)
        DocumentValue(printed(person.surname), at(u, left - 0.6f, 15.6f), value, activeField == FieldKind.SURNAME, u)

        Label(stringResource(R.string.card_first_name), at(u, left, 20.6f), label, maxLines = 1)
        DocumentValue(
            printed(person.firstName),
            at(u, left - 0.6f, 22.8f),
            dataStyle(u, valueSize(person.firstName)),
            activeField == FieldKind.FIRST_NAME,
            u
        )

        Label(stringResource(R.string.card_birth_date), at(u, left, 27.6f), label)
        DocumentValue(birthDate, at(u, left - 0.6f, 31.6f), dataStyle(u, 3.5f), activeField == FieldKind.BIRTH_DATE, u)
        Label(stringResource(R.string.card_nationality), at(u, 68f, 27.6f), label)
        Label(stringResource(R.string.card_nationality_value), at(u, 68f, 31.6f), dataStyle(u, 3.5f), maxLines = 1)

        Label(stringResource(R.string.card_birth_place), at(u, left, 35.8f), label, maxLines = 1)
        Label(printed(person.birthPlace), at(u, left, 38f), dataStyle(u, 3.5f), maxLines = 1)

        Label(stringResource(R.string.card_expiry), at(u, left, 42.6f), label)
        Label(person.expiryDate.format(Case.DATE_FORMAT), at(u, left, 46.7f), dataStyle(u, 3.5f), maxLines = 1)
        Label(card.accessNumber, at(u, 78f, 46.4f), dataStyle(u, 3.8f, spaced = 0.5f), maxLines = 1)

        Signature(person, at(u, 46f, 51.4f).size(u * 30f, u * 10f))
    }
}

private fun valueSize(text: String) = if (text.length > 22) 2.9f else 3.5f

@Composable
private fun CardBackground() {
    Canvas(Modifier.fillMaxSize()) {
        drawRect(
            Brush.linearGradient(listOf(CardCream, CardSand, CardRose), Offset.Zero, Offset(size.width, size.height))
        )
        guilloche(Color(0xFF9BAE7E).copy(alpha = 0.4f), 0.28f, 0.4f)
        guilloche(Color(0xFFC2A36E).copy(alpha = 0.36f), 0.58f, 1.7f)
        guilloche(Color(0xFF8FB0A2).copy(alpha = 0.34f), 0.85f, 2.9f)
    }
}

@Composable
private fun Header(card: Case.IdCard, u: Dp) {
    val blue = EuBlue
    EuFlag(
        stringResource(R.string.card_country),
        printStyle(u, 2.7f, bold = true, color = Color.White),
        at(u, 4f, 3.6f).size(u * 12.5f, u * 9f)
    )
    Label(
        stringResource(R.string.card_state),
        at(u, 18.5f, 3.3f),
        printStyle(u, 2.9f, bold = true, color = blue),
        maxLines = 1
    )
    Label(
        stringResource(R.string.card_state_translation),
        at(u, 18.5f, 6.5f),
        printStyle(u, 1.2f, bold = true, color = blue),
        maxLines = 1
    )
    Label(
        stringResource(R.string.card_type),
        at(u, 18.5f, 8.3f),
        printStyle(u, 2.5f, bold = true, color = blue),
        maxLines = 1
    )
    Label(
        stringResource(R.string.card_type_translation),
        at(u, 18.5f, 11f),
        printStyle(u, 1.15f, bold = true, color = blue),
        maxLines = 1
    )
    Label(card.documentNumber, at(u, 72.5f, 3.8f), dataStyle(u, 3.3f, spaced = 0.4f), maxLines = 1)
    Canvas(at(u, 58f, 9.4f).size(u * 5.4f, u * 3.2f)) {
        drawRect(Color(0xFF1E7A5A))
        drawOval(
            Color.White,
            Offset(size.width * 0.18f, size.height * 0.2f),
            size.copy(
                size.width * 0.64f,
                size.height * 0.6f
            )
        )
        drawCircle(EuBlue, size.height * 0.2f)
    }
}
