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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import java.time.format.DateTimeFormatter
import run.moritz.anton.R
import run.moritz.anton.cases.Case
import run.moritz.anton.cases.FieldKind

private val LicenceBlue = Color(0xFFBBDCE8)
private val LicencePeach = Color(0xFFF3E1D4)
private val LicenceRose = Color(0xFFF1CFCB)
private val NumberInk = Color(0xFF3F6A8A)
private val ShortDate: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yy")

/** The front of the German EU driving licence as issued since 2021. Its fields carry the EU numbers 1 to 9. */
@Composable
fun DrivingLicence(licence: Case.DrivingLicence, activeField: FieldKind?, modifier: Modifier = Modifier) {
    val person = licence.person
    val description = stringResource(
        R.string.licence_description,
        person.firstName,
        person.surname,
        person.birthDate.format(Case.DATE_FORMAT)
    )
    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .aspectRatio(85.6f / 54f)
            .clip(RoundedCornerShape(DocumentCorner))
            .border(1.dp, Color(0xFF8C9AA0), RoundedCornerShape(DocumentCorner))
            .semantics { contentDescription = description }
    ) {
        val u = maxWidth / 100
        LicenceBackground()
        EuFlag(
            stringResource(R.string.licence_country),
            printStyle(u, 5.2f, bold = true, color = Color.White),
            at(u, 1.8f, 2f).size(u * 23.5f, u * 14.4f)
        )
        Label(
            stringResource(R.string.licence_type),
            at(u, 26.8f, 1.9f),
            printStyle(u, 4.3f, bold = true, condensed = true),
            maxLines = 1
        )
        Label(
            stringResource(R.string.card_state),
            at(u, 54f, 2.6f),
            printStyle(u, 3.2f, color = EuBlue, condensed = true),
            maxLines = 1
        )

        val monochrome = remember {
            ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })
        }
        PersonPortrait(person, at(u, 1.8f, 17.4f).size(u * 23.5f, u * 31f), monochrome)

        val data = dataStyle(u, 3.25f, spaced = 0.1f)
        val number = printStyle(u, 2.6f, color = NumberInk)
        val x = 30.5f

        LicenceLine(u, "1.", 8.2f, number)
        DocumentValue(person.surname, at(u, x - 0.6f, 8.2f), data, activeField == FieldKind.SURNAME, u)
        LicenceLine(u, "2.", 14.6f, number)
        DocumentValue(person.firstName, at(u, x - 0.6f, 14.6f), data, activeField == FieldKind.FIRST_NAME, u)
        LicenceLine(u, "3.", 19f, number)
        DocumentValue(
            person.birthDate.format(ShortDate),
            at(u, x - 0.6f, 19f),
            data,
            activeField == FieldKind.BIRTH_DATE,
            u
        )
        Label(person.birthPlace, at(u, x + 19f, 19f), data, maxLines = 1)

        LicenceLine(u, "4a.", 23.4f, number)
        Label(licence.issueDate.format(ShortDate), at(u, x + 1.2f, 23.4f), data, maxLines = 1)
        Label("4c.", at(u, x + 21f, 23.4f), number, maxLines = 1)
        Label(licence.authority, at(u, x + 25.4f, 23.4f), data, maxLines = 1)
        LicenceLine(u, "4b.", 27.8f, number)
        Label(licence.expiryDate.format(ShortDate), at(u, x + 1.2f, 27.8f), data, maxLines = 1)
        LicenceLine(u, "5.", 32.2f, number)
        Label(licence.number, at(u, x, 32.2f), data, maxLines = 1)

        LicenceLine(u, "7.", 44f, number)
        Signature(person.surname, at(u, x, 37.5f).size(u * 35f, u * 13f), Color(0xFF16181C))

        Label("9.", at(u, 2.4f, 50.6f), number, maxLines = 1)
        Label(licence.classes, at(u, 6.4f, 50.6f), data, maxLines = 1)

        Label(
            stringResource(R.string.specimen),
            at(u, 80f, 32.2f),
            printStyle(u, 3.6f, bold = true, color = Ink.copy(alpha = 0.45f)),
            maxLines = 1
        )
    }
}

@Composable
private fun LicenceLine(u: Dp, number: String, y: Float, style: TextStyle) {
    Label(number, at(u, 26.6f, y), style, maxLines = 1)
}

@Composable
private fun LicenceBackground() {
    Canvas(Modifier.fillMaxSize()) {
        drawRect(LicencePeach)
        drawRect(
            Brush.horizontalGradient(
                listOf(
                    LicenceBlue.copy(alpha = 0.75f),
                    LicencePeach.copy(alpha = 0f),
                    LicenceRose.copy(alpha = 0.85f),
                    LicenceBlue.copy(alpha = 0.6f)
                )
            )
        )
        drawRect(
            Brush.verticalGradient(
                listOf(LicenceBlue.copy(alpha = 0.2f), Color.Transparent, LicenceBlue.copy(alpha = 0.45f))
            )
        )
        guilloche(Color(0xFFD99CA0).copy(alpha = 0.3f), 0.35f, 0.9f, lines = 20)
        guilloche(Color(0xFF7FB0C4).copy(alpha = 0.3f), 0.72f, 2.1f, lines = 20)
        repeat(4) { ring ->
            val radius = size.height * (0.16f + ring * 0.05f)
            drawCircle(
                Color(0xFF7FB0C4).copy(alpha = 0.22f),
                radius,
                Offset(size.width * 0.89f, size.height * 0.68f),
                style = Stroke(size.height * 0.012f)
            )
        }
        drawRoundRect(
            Color.White.copy(alpha = 0.75f),
            Offset(size.width * 0.955f, size.height * 0.82f),
            Size(size.width * 0.03f, size.width * 0.03f)
        )
    }
}
