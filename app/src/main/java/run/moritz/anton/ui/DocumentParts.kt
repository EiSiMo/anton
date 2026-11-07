package run.moritz.anton.ui

import android.graphics.BitmapFactory
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextGeometricTransform
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import run.moritz.anton.cases.Person
import run.moritz.anton.cases.PersonCatalog

/** All documents measure in hundredths of their width, so they keep their proportions on every screen. */
@Suppress("ModifierFactoryExtensionFunction") // Reads like a coordinate on the document, which is all it is.
internal fun at(u: Dp, x: Float, y: Float): Modifier = Modifier.offset(u * x, u * y)

internal val EuBlue = Color(0xFF1D4FA3)
internal val EuYellow = Color(0xFFFFCC00)
internal val Ink = Color(0xFF14181C)

/** Names on an ID card are printed in capitals, and the German eszett stays what it is. */
fun printed(text: String): String = text.split('ß').joinToString("ß") { it.uppercase() }

@Composable
internal fun printStyle(
    u: Dp,
    size: Float,
    bold: Boolean = false,
    color: Color = Ink,
    condensed: Boolean = false
): TextStyle {
    val fontSize = with(LocalDensity.current) { (u * size).toSp() }
    return TextStyle(
        fontFamily = Classic.Print,
        fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
        fontSize = fontSize,
        lineHeight = fontSize * 1.08f,
        color = color,
        textGeometricTransform = if (condensed) TextGeometricTransform(scaleX = 0.78f) else null
    )
}

/** The machine-like lettering of personal data. */
@Composable
internal fun dataStyle(u: Dp, size: Float, color: Color = Ink, spaced: Float = 0f): TextStyle {
    val fontSize = with(LocalDensity.current) { (u * size).toSp() }
    return TextStyle(
        fontFamily = Classic.DocumentData,
        fontSize = fontSize,
        lineHeight = fontSize,
        color = color,
        letterSpacing = with(LocalDensity.current) { (u * spaced).toSp() }
    )
}

/** A value on a document. The field being transmitted is marked like with a highlighter. */
@Composable
internal fun DocumentValue(text: String, modifier: Modifier, style: TextStyle, active: Boolean, u: Dp) {
    val mark = if (active) {
        Modifier.background(Classic.Warning).border(1.dp, Classic.Title).padding(horizontal = u * 0.6f)
    } else {
        Modifier.padding(horizontal = u * 0.6f)
    }
    val lineHeight = with(LocalDensity.current) { style.fontSize.toDp() } * VALUE_LINE_HEIGHT
    val raised = (lineHeight - with(LocalDensity.current) { style.lineHeight.toDp() }) / 2
    Label(
        text,
        modifier.offset(y = -raised).then(mark),
        style.copy(lineHeight = style.fontSize * VALUE_LINE_HEIGHT),
        maxLines = 1
    )
}

/** Tall enough for the dots of capital umlauts, which would otherwise be cut off by the marked box. */
private const val VALUE_LINE_HEIGHT = 1.4f

/** The blue field with the ring of stars and a country mark. */
@Composable
internal fun EuFlag(mark: String, markStyle: TextStyle, modifier: Modifier) {
    Box(modifier.background(EuBlue), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val ring = size.height * 0.37f
            val middle = Offset(size.width / 2, size.height / 2)
            repeat(12) { index ->
                val angle = index * PI / 6
                val at = middle + Offset(ring * cos(angle).toFloat(), ring * sin(angle).toFloat())
                drawCircle(EuYellow, size.height * 0.052f, at)
            }
        }
        Label(mark, style = markStyle, maxLines = 1)
    }
}

/** The fine wavy security print of a document. */
internal fun DrawScope.guilloche(color: Color, base: Float, phase: Float, lines: Int = 24, swing: Float = 0.06f) {
    val steps = 120
    val width = 0.45f * density
    repeat(lines) { line ->
        val path = Path()
        for (step in 0..steps) {
            val t = step.toFloat() / steps
            val wave = sin(t * 2 * PI * 2.5 + phase + line * 0.28) + 0.45 * sin(t * 2 * PI * 6 - phase + line * 0.13)
            val y = size.height * (base + (line - lines / 2) * 0.01f + swing * wave.toFloat())
            if (step == 0) path.moveTo(0f, y) else path.lineTo(size.width * t, y)
        }
        drawPath(path, color, style = Stroke(width))
    }
}

/** The portrait of a person, cut like a passport picture. */
@Composable
internal fun PersonPortrait(person: Person, modifier: Modifier, filter: ColorFilter? = null) {
    val context = LocalContext.current
    val portrait by produceState<ImageBitmap?>(null, person.portrait) {
        value = withContext(Dispatchers.IO) {
            val path = "${PersonCatalog.PORTRAIT_DIRECTORY}/${person.portrait}"
            val bitmap = context.assets.open(path).use(BitmapFactory::decodeStream)
            checkNotNull(bitmap) { "Portrait $path cannot be decoded" }.asImageBitmap()
        }
    }
    Box(modifier.background(Color(0xFFD9DADA))) {
        portrait?.let {
            Image(
                it,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                alignment = Alignment.TopCenter,
                contentScale = ContentScale.Crop,
                colorFilter = filter
            )
        }
    }
}

/** The person's own signature: the same person always signs alike, and no two sign the same way. */
@Composable
internal fun Signature(person: Person, modifier: Modifier, color: Color = Color(0xFF1A2348)) {
    val strokes = remember(person) { SignatureHand(person).strokes() }
    val weight = remember(person) { 0.75f + (person.surname.hashCode().mod(5)) * 0.14f }
    Canvas(modifier) {
        strokes.forEach { stroke ->
            val path = Path()
            val points = stroke.points.map { Offset(it.x * size.width, it.y * size.height) }
            path.moveTo(points.first().x, points.first().y)
            if (stroke.smooth) {
                points.zipWithNext().forEach { (from, to) ->
                    val middle = (from + to) / 2f
                    path.quadraticTo(from.x, from.y, middle.x, middle.y)
                }
                path.lineTo(points.last().x, points.last().y)
            } else {
                points.drop(1).forEach { path.lineTo(it.x, it.y) }
            }
            val style = Stroke(weight * density, cap = StrokeCap.Round, join = StrokeJoin.Round)
            drawPath(path, color, style = style)
        }
    }
}

private class SignatureStroke(val points: List<Offset>, val smooth: Boolean)

/**
 * Writes a name the way a hurried hand does: a large initial, a few readable letters that dissolve into a line,
 * and a closing flourish. Every person gets their own slant, size, rhythm and flourish.
 */
private class SignatureHand(private val person: Person) {
    private val random = Random("${person.firstName} ${person.surname}".hashCode())
    private val slant = random.nextFloat() * 0.6f - 0.1f
    private val rise = random.nextFloat() * 0.3f - 0.2f
    private val letterHeight = 0.16f + random.nextFloat() * 0.2f
    private val readable = 0.3f + random.nextFloat() * 0.7f
    private val smooth = random.nextFloat() > 0.22f
    private val withFirstInitial = random.nextFloat() < 0.45f
    private val baseline = 0.62f

    fun strokes(): List<SignatureStroke> {
        val body = mutableListOf<Offset>()
        val extras = mutableListOf<SignatureStroke>()
        var x = 0.02f
        if (withFirstInitial) {
            extras += SignatureStroke(initial(x, 0.5f + random.nextFloat() * 0.3f), smooth)
            x += 0.14f + random.nextFloat() * 0.08f
        }
        val initialPoints = initial(x, 1f)
        body += initialPoints
        x = initialPoints.maxOf { it.x } * 0.6f + x * 0.4f + 0.03f

        val letters = person.surname.drop(1).filter { it.isLetter() }.lowercase()
        val written = letters.take((letters.length * readable).toInt().coerceAtLeast(2))
        val advance = (0.62f - x * 0.4f) / letters.length.coerceAtLeast(4)
        written.forEach { letter ->
            body += letterShape(letter, x, advance)
            x += advance
        }
        val rest = letters.length - written.length
        if (rest > 0) {
            body += trail(x, advance * rest)
            x += advance * rest
        }
        extras += flourish(body, x)
        return (listOf(SignatureStroke(body, smooth)) + extras).fitted()
    }

    private fun jitter(amount: Float) = (random.nextFloat() - 0.5f) * amount

    private fun initial(x: Float, scale: Float): List<Offset> {
        val top = baseline - (0.45f + random.nextFloat() * 0.2f) * scale
        val low = baseline + (0.1f + random.nextFloat() * 0.3f) * scale
        val width = (0.05f + random.nextFloat() * 0.07f) * scale
        return when (random.nextInt(4)) {
            0 -> listOf(p(x, baseline), p(x + width * 0.3f, top), p(x + width, top + 0.1f), p(x + width * 0.2f, low))

            1 -> listOf(
                p(x + width, top + 0.12f),
                p(x + width * 0.3f, top),
                p(x, baseline - 0.1f),
                p(x + width, baseline),
                p(x + width * 1.3f, baseline - 0.2f),
                p(x + width * 0.5f, low)
            )

            2 -> listOf(p(x - 0.01f, top + 0.15f), p(x + width * 1.4f, top), p(x + width * 0.5f, low), p(x, baseline))

            else -> listOf(
                p(x, low),
                p(x + width * 0.6f, top),
                p(x + width, baseline),
                p(x + width * 1.2f, top + 0.08f)
            )
        }
    }

    private fun letterShape(letter: Char, x: Float, advance: Float): List<Offset> {
        val up = baseline - letterHeight * (0.8f + random.nextFloat() * 0.4f)
        return when (letter) {
            in ASCENDERS -> {
                val top = baseline - letterHeight * (1.9f + random.nextFloat() * 0.7f)
                listOf(p(x, up), p(x + advance * 0.4f, top), p(x + advance * 0.55f, baseline), p(x + advance, up))
            }

            in DESCENDERS -> {
                val low = baseline + letterHeight * (1.2f + random.nextFloat() * 0.9f)
                listOf(p(x, up), p(x + advance * 0.5f, low), p(x + advance * 0.2f, baseline), p(x + advance, up))
            }

            else -> listOf(p(x + advance * 0.3f, baseline + jitter(0.06f)), p(x + advance * 0.7f, up + jitter(0.1f)))
        }
    }

    /** The end of the name, where the pen just runs on. */
    private fun trail(x: Float, length: Float): List<Offset> {
        val waves = random.nextInt(0, 4)
        return List(waves * 2 + 2) { index ->
            val swing = if (index % 2 == 0) 0f else letterHeight * 0.45f
            p(x + length * (index + 1) / (waves * 2 + 2), baseline - swing)
        }
    }

    private fun flourish(body: MutableList<Offset>, endX: Float): List<SignatureStroke> {
        val low = baseline + 0.16f + random.nextFloat() * 0.14f
        return when (random.nextInt(6)) {
            0 -> {
                body += listOf(p(endX + 0.1f, baseline - 0.1f), p(endX - 0.2f, low), p(0.08f, low + 0.08f))
                emptyList()
            }

            1 -> listOf(
                SignatureStroke(listOf(p(0.1f, low), p(endX * 0.6f, low + 0.05f), p(endX + 0.08f, low - 0.05f)), true)
            )

            2 -> {
                body += listOf(p(endX + 0.14f, baseline - 0.42f))
                emptyList()
            }

            3 -> {
                body +=
                    listOf(
                        p(endX + 0.04f, baseline + 0.1f),
                        p(endX - 0.06f, baseline + 0.02f),
                        p(
                            endX + 0.1f,
                            baseline - 0.1f
                        )
                    )
                emptyList()
            }

            4 -> listOf(
                SignatureStroke(listOf(p(endX * 0.45f, baseline - 0.4f), p(endX * 0.58f, baseline - 0.42f)), false)
            )

            else -> emptyList()
        }
    }

    /** Applies the hand's slant and rising baseline. */
    private fun p(x: Float, y: Float) = Offset(x + slant * (baseline - y) * 0.3f, y + rise * x)

    /** Scales the signature into its box, keeping some of each hand's own size. */
    private fun List<SignatureStroke>.fitted(): List<SignatureStroke> {
        val all = flatMap { it.points }
        val left = all.minOf { it.x }
        val top = all.minOf { it.y }
        val width = (all.maxOf { it.x } - left).coerceAtLeast(0.01f)
        val height = (all.maxOf { it.y } - top).coerceAtLeast(0.01f)
        val fill = 0.72f + random.nextFloat() * 0.26f
        return map { stroke ->
            val points = stroke.points.map { Offset((it.x - left) / width * fill, (it.y - top) / height * 0.95f) }
            SignatureStroke(points, stroke.smooth)
        }
    }

    private companion object {
        const val ASCENDERS = "bdfhklt"
        const val DESCENDERS = "gjpqyz"
    }
}

internal val DocumentCorner = 12.dp
