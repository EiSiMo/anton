package run.moritz.anton.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random
import run.moritz.anton.R
import run.moritz.anton.cases.Case
import run.moritz.anton.cases.FieldKind
import run.moritz.anton.cases.Sex

private val InkGrey = Color(0xFF45505A)
private val Ink = Color(0xFF14181C)
private val NumberRed = Color(0xFF8E1B2E)
private val CardMint = Color(0xFFD7E8D2)
private val CardCream = Color(0xFFF3EFD2)
private val CardRose = Color(0xFFF2D8DC)

/** Name fields are printed in capitals, and the German eszett stays what it is. */
fun printed(text: String): String = text.split('ß').joinToString("ß") { it.uppercase() }

/**
 * The front of a German ID card, carrying fictional data. All measures are in hundredths of the card width,
 * so the card keeps its proportions on every screen.
 */
@Composable
fun IdCard(card: Case.IdCard, activeField: FieldKind?, modifier: Modifier = Modifier) {
    val person = card.person
    val birthDate = person.birthDate.format(Case.DATE_FORMAT)
    val description = stringResource(R.string.card_description, person.firstName, person.surname, birthDate)
    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .aspectRatio(85.6f / 54f)
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, Color(0xFF8C9188), RoundedCornerShape(12.dp))
            .semantics { contentDescription = description }
    ) {
        val u = maxWidth / 100
        CardBackground()
        Eagle(at(u, 47f, 13f).size(u * 46f, u * 46f), Color(0xFF8FAF96).copy(alpha = 0.3f))
        val type = cardText(u, 3.1f, bold = true)
        val small = cardText(u, 1.55f, color = InkGrey)
        val label = cardText(u, 1.75f, color = InkGrey)

        Label(stringResource(R.string.card_state), at(u, 4f, 3.2f), cardText(u, 3.5f, bold = true), maxLines = 1)
        Label(stringResource(R.string.card_state_translation), at(u, 4f, 7.6f), small, maxLines = 1)
        Label(stringResource(R.string.card_type), at(u, 66f, 3.4f), type, maxLines = 1)
        Label(stringResource(R.string.card_type_translation), at(u, 66f, 7.6f), small, maxLines = 1)

        Label(
            card.documentNumber,
            at(u, 73f, 10.6f),
            cardText(u, 3.5f, bold = true, color = NumberRed, mono = true),
            maxLines = 1
        )

        Portrait(card, at(u, 4f, 13f).size(u * 27f, u * 35f))

        val left = 34f
        val right = 67f
        CardField(
            u,
            left,
            12.5f,
            stringResource(R.string.card_surname),
            printed(person.surname),
            activeField == FieldKind.SURNAME
        )
        CardField(
            u,
            left,
            22f,
            stringResource(R.string.card_first_name),
            printed(person.firstName),
            activeField == FieldKind.FIRST_NAME
        )
        CardField(
            u,
            left,
            31.5f,
            stringResource(R.string.card_birth_date),
            birthDate,
            activeField == FieldKind.BIRTH_DATE
        )
        CardField(
            u,
            right,
            31.5f,
            stringResource(R.string.card_nationality),
            stringResource(R.string.card_nationality_value),
            false
        )
        CardField(u, left, 41f, stringResource(R.string.card_birth_place), person.birthPlace, false)
        CardField(u, right, 41f, stringResource(R.string.card_expiry), card.expiryDate.format(Case.DATE_FORMAT), false)

        Label(stringResource(R.string.card_signature), at(u, 4f, 50f), label, maxLines = 1)
        Signature(person.surname, at(u, 4f, 51.6f).size(u * 27f, u * 8f))

        Portrait(card, at(u, 82f, 45.5f).size(u * 11f, u * 14f).alpha(0.32f))
        Label("CAN", at(u, 34f, 53.4f), label, maxLines = 1)
        Label(card.accessNumber, at(u, 39f, 52.6f), cardText(u, 2.8f, mono = true, bold = true), maxLines = 1)
    }
}

@Suppress("ModifierFactoryExtensionFunction") // Reads like a coordinate on the card, which is all it is.
private fun at(u: Dp, x: Float, y: Float): Modifier = Modifier.offset(u * x, u * y)

@Composable
private fun cardText(u: Dp, size: Float, bold: Boolean = false, color: Color = Ink, mono: Boolean = false): TextStyle {
    val fontSize = with(LocalDensity.current) { (u * size).toSp() }
    return TextStyle(
        fontFamily = if (mono) Classic.Mono else Classic.Print,
        fontWeight = if (bold || mono) FontWeight.Bold else FontWeight.Normal,
        fontSize = fontSize,
        lineHeight = fontSize,
        color = color
    )
}

@Composable
private fun BoxWithConstraintsScope.CardField(
    u: Dp,
    x: Float,
    y: Float,
    label: String,
    value: String,
    active: Boolean
) {
    Label(label, at(u, x, y), cardText(u, 1.75f, color = InkGrey), maxLines = 1)
    val highlight = if (active) {
        Modifier.background(Classic.Warning).border(1.dp, Classic.Title).padding(horizontal = u * 0.7f)
    } else {
        Modifier.padding(horizontal = u * 0.7f)
    }
    Label(value, at(u, x - 0.7f, y + 2.5f).then(highlight), cardText(u, 3.5f, bold = true), maxLines = 1)
}

@Composable
private fun CardBackground() {
    Canvas(Modifier.fillMaxSize()) {
        drawRect(
            Brush.linearGradient(listOf(CardMint, CardCream, CardRose), Offset.Zero, Offset(size.width, size.height))
        )
        guilloche(Color(0xFF7FAE8E).copy(alpha = 0.33f), 0.2f, 0f)
        guilloche(Color(0xFFD98A9B).copy(alpha = 0.3f), 0.55f, 1.3f)
        guilloche(Color(0xFFB7A96A).copy(alpha = 0.26f), 0.85f, 2.4f)
    }
}

private fun DrawScope.guilloche(color: Color, base: Float, phase: Float) {
    val lines = 15
    val steps = 90
    repeat(lines) { line ->
        val path = Path()
        for (step in 0..steps) {
            val t = step.toFloat() / steps
            val wave = sin(t * 2 * PI * 3 + phase + line * 0.34) + 0.5 * sin(t * 2 * PI * 7 - phase + line * 0.17)
            val y = size.height * (base + (line - lines / 2) * 0.012f + 0.05f * wave.toFloat())
            if (step == 0) path.moveTo(0f, y) else path.lineTo(size.width * t, y)
        }
        drawPath(path, color, style = Stroke(0.7f * density / 2))
    }
}

/** A heraldic eagle in a unit square: wings spread, feathers hanging, head turned to one side. */
@Composable
private fun Eagle(modifier: Modifier, color: Color) {
    Canvas(modifier) {
        val s = size.minDimension
        fun shape(mirrored: Boolean, vararg points: Pair<Float, Float>) {
            val path = Path()
            points.forEachIndexed { index, (x, y) ->
                val px = (if (mirrored) 1 - x else x) * s
                if (index == 0) path.moveTo(px, y * s) else path.lineTo(px, y * s)
            }
            path.close()
            drawPath(path, color)
        }
        fun both(vararg points: Pair<Float, Float>) {
            shape(false, *points)
            shape(true, *points)
        }

        // Head with beak, looking to the viewer's left.
        shape(
            false,
            0.56f to 0.09f, 0.5f to 0.05f, 0.44f to 0.07f, 0.41f to 0.11f, 0.34f to 0.14f, 0.42f to 0.15f,
            0.45f to 0.2f, 0.45f to 0.3f, 0.55f to 0.3f, 0.56f to 0.18f
        )
        // Body and tail.
        shape(
            false,
            0.42f to 0.27f, 0.58f to 0.27f, 0.6f to 0.5f, 0.56f to 0.68f, 0.6f to 0.86f, 0.53f to 0.8f,
            0.5f to 0.95f, 0.47f to 0.8f, 0.4f to 0.86f, 0.44f to 0.68f, 0.4f to 0.5f
        )
        // Wing arm and hanging feathers.
        both(0.42f to 0.28f, 0.3f to 0.17f, 0.12f to 0.12f, 0.03f to 0.2f, 0.03f to 0.34f, 0.42f to 0.42f)
        for (feather in 0 until 6) {
            val rootX = 0.04f + feather * 0.062f
            val rootWidth = 0.056f
            val tipY = 0.56f + feather * 0.048f
            val tipX = rootX - 0.03f + feather * 0.012f
            both(
                rootX to 0.3f,
                rootX + rootWidth to 0.3f,
                tipX + rootWidth * 0.7f to tipY - 0.02f,
                tipX + rootWidth * 0.3f to tipY,
                tipX to tipY - 0.03f
            )
        }
        // Legs with spread talons.
        both(
            0.42f to 0.56f, 0.34f to 0.62f, 0.26f to 0.68f, 0.3f to 0.7f, 0.34f to 0.68f, 0.32f to 0.75f, 0.37f to 0.7f,
            0.39f to 0.75f, 0.42f to 0.66f
        )
    }
}

/** A greyscale passport picture of a fictional person, drawn from a seed and softened like a photo. */
@Composable
private fun Portrait(card: Case.IdCard, modifier: Modifier) {
    val seed = card.portraitSeed
    val person = card.person
    val female = person.sex == Sex.FEMALE
    val age = LocalDate.now().year - person.birthDate.year
    Canvas(modifier.clip(RoundedCornerShape(2.dp)).blur(0.7.dp, BlurredEdgeTreatment.Rectangle)) {
        val random = Random(seed)
        val w = size.width
        val h = size.height
        val skinTone = 0.62f + random.nextFloat() * 0.24f
        val skin = grey(skinTone)
        val shade = grey(skinTone * 0.72f)
        val deep = grey(skinTone * 0.45f)
        val hairTone = if (age > GREY_HAIR_AGE) {
            0.48f + random.nextFloat() * 0.3f
        } else {
            0.08f + random.nextFloat() * random.nextFloat() * 0.5f
        }
        val hair = grey(hairTone)
        val hairDark = grey(hairTone * 0.7f)
        val clothing = grey(0.2f + random.nextFloat() * 0.34f)

        drawRect(Brush.verticalGradient(listOf(Color(0xFFEDEEEC), Color(0xFFD6D8D5))))

        val headWidth = w * (0.4f + random.nextFloat() * 0.06f)
        val headHeight = h * (0.44f + random.nextFloat() * 0.05f)
        val head = Rect(Offset((w - headWidth) / 2, h * 0.17f), Size(headWidth, headHeight))
        val longHair = female && random.nextFloat() < 0.8f
        val bob = female && !longHair
        val bald = !female && age > GREY_HAIR_AGE && random.nextFloat() < 0.35f

        if (longHair || bob) {
            val length = if (longHair) h * (0.62f + random.nextFloat() * 0.16f) else h * 0.5f
            drawRoundRect(
                hairDark,
                Offset(w / 2 - headWidth * 0.66f, head.top - h * 0.04f),
                Size(headWidth * 1.32f, length),
                CornerRadius(headWidth * 0.55f, headWidth * 0.55f)
            )
        }

        drawRect(deep, Offset(w / 2 - headWidth * 0.25f, head.bottom - h * 0.1f), Size(headWidth * 0.5f, h * 0.24f))
        val shoulders = Path().apply {
            moveTo(0f, h)
            lineTo(0f, h * 0.93f)
            cubicTo(w * 0.08f, h * 0.84f, w * 0.26f, h * 0.83f, w * 0.38f, h * 0.77f)
            lineTo(w * 0.5f, h * 0.87f)
            lineTo(w * 0.62f, h * 0.77f)
            cubicTo(w * 0.74f, h * 0.83f, w * 0.92f, h * 0.84f, w, h * 0.93f)
            lineTo(w, h)
            close()
        }
        drawPath(shoulders, clothing)

        val ear = head.top + headHeight * 0.44f
        if (!longHair && !bob) {
            drawOval(shade, Offset(head.left - w * 0.022f, ear), Size(w * 0.055f, h * 0.085f))
            drawOval(shade, Offset(head.right - w * 0.033f, ear), Size(w * 0.055f, h * 0.085f))
        }
        drawOval(
            Brush.radialGradient(
                listOf(skin, skin, shade),
                head.center - Offset(0f, headHeight * 0.06f),
                headWidth * 0.62f
            ),
            head.topLeft,
            head.size
        )

        if (!bald) {
            val sideDrop = if (longHair || bob) 0.66f else 0.3f + random.nextFloat() * 0.08f
            val parting = 0.3f + random.nextFloat() * 0.4f
            val cap = Path().apply {
                moveTo(head.left - w * 0.012f, head.top + headHeight * sideDrop)
                cubicTo(
                    head.left - w * 0.05f,
                    head.top - h * 0.075f,
                    head.right + w * 0.05f,
                    head.top - h * 0.075f,
                    head.right + w * 0.012f,
                    head.top + headHeight * sideDrop
                )
                quadraticTo(
                    head.right - headWidth * 0.1f,
                    head.top + headHeight * 0.24f,
                    head.left + headWidth * parting,
                    head.top + headHeight * 0.13f
                )
                quadraticTo(
                    head.left + headWidth * 0.1f,
                    head.top + headHeight * 0.24f,
                    head.left - w * 0.012f,
                    head.top + headHeight * sideDrop
                )
                close()
            }
            drawPath(cap, hair)
        }

        val eyeY = head.top + headHeight * 0.46f
        val eyeGap = headWidth * 0.23f
        listOf(-1f, 1f).forEach { side ->
            val eye = Offset(w / 2 + side * eyeGap, eyeY)
            drawOval(shade, eye - Offset(w * 0.058f, h * 0.024f), Size(w * 0.116f, h * 0.048f), alpha = 0.55f)
            drawOval(Color(0xFFF0F0F0), eye - Offset(w * 0.036f, h * 0.011f), Size(w * 0.072f, h * 0.022f))
            drawCircle(Color(0xFF262626), h * 0.0115f, eye)
            drawLine(
                if (bald) deep else hairDark,
                eye + Offset(-w * 0.052f, -h * 0.026f),
                eye + Offset(w * 0.052f, -h * 0.03f),
                strokeWidth = h * 0.0085f,
                cap = StrokeCap.Round,
                alpha = 0.85f
            )
        }
        val noseTip = eyeY + h * 0.095f
        drawOval(shade, Offset(w / 2 - w * 0.035f, noseTip - h * 0.012f), Size(w * 0.07f, h * 0.022f), alpha = 0.7f)
        drawLine(
            shade,
            Offset(w / 2 + w * 0.012f, eyeY),
            Offset(w / 2 + w * 0.024f, noseTip),
            h * 0.006f,
            alpha = 0.45f
        )
        drawLine(
            deep,
            Offset(w / 2 - w * 0.062f, eyeY + h * 0.158f),
            Offset(w / 2 + w * 0.062f, eyeY + h * 0.158f),
            strokeWidth = h * 0.0085f,
            cap = StrokeCap.Round,
            alpha = 0.8f
        )
        if (random.nextFloat() < GLASSES_SHARE) {
            listOf(-1f, 1f).forEach { side ->
                drawRoundRect(
                    Color(0xFF303030),
                    Offset(w / 2 + side * eyeGap - w * 0.072f, eyeY - h * 0.032f),
                    Size(w * 0.144f, h * 0.064f),
                    CornerRadius(h * 0.014f, h * 0.014f),
                    style = Stroke(h * 0.0055f)
                )
            }
        }
    }
}

private const val GREY_HAIR_AGE = 58
private const val GLASSES_SHARE = 0.22f

private fun grey(level: Float) = Color(level, level, level)

/** A handwritten looking line derived from the name. */
@Composable
private fun Signature(name: String, modifier: Modifier) {
    Canvas(modifier) {
        val random = Random(name.hashCode())
        val path = Path()
        val w = size.width
        val h = size.height
        path.moveTo(w * 0.04f, h * 0.6f)
        path.cubicTo(w * 0.06f, h * 0.05f, w * 0.14f, h * 0.05f, w * 0.13f, h * 0.7f)
        var x = w * 0.15f
        val strokes = 7 + name.length / 2
        repeat(strokes) {
            val nextX = x + w * 0.8f / strokes
            val peak = h * (0.15f + random.nextFloat() * 0.5f)
            path.cubicTo(x + w * 0.02f, peak, nextX - w * 0.03f, h * 0.9f, nextX, h * 0.55f)
            x = nextX
        }
        path.cubicTo(x + w * 0.02f, h * 0.4f, w * 0.5f, h * 0.95f, w * 0.1f, h * 0.82f)
        drawPath(
            path,
            Color(0xFF1A2A5A),
            style = Stroke(1.2f * density, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}
