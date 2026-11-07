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
    Label(text, modifier.then(mark), style, maxLines = 1)
}

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

/** A handwritten looking line derived from the name. */
@Composable
internal fun Signature(name: String, modifier: Modifier, color: Color = Color(0xFF1A2348)) {
    Canvas(modifier) {
        val random = Random(name.hashCode())
        val path = Path()
        val w = size.width
        val h = size.height
        path.moveTo(w * 0.03f, h * 0.72f)
        path.cubicTo(w * 0.05f, h * 0.02f, w * 0.15f, h * 0.0f, w * 0.1f, h * 0.9f)
        path.cubicTo(w * 0.14f, h * 0.5f, w * 0.17f, h * 0.45f, w * 0.19f, h * 0.62f)
        var x = w * 0.19f
        val strokes = 6 + name.length / 2
        repeat(strokes) {
            val nextX = x + w * 0.7f / strokes
            val peak = h * (0.1f + random.nextFloat() * 0.5f)
            path.cubicTo(x + w * 0.012f, peak, nextX - w * 0.02f, h * 0.95f, nextX, h * 0.58f)
            x = nextX
        }
        path.cubicTo(x + w * 0.05f, h * 0.2f, w * 0.6f, h * 1.0f, w * 0.12f, h * 0.88f)
        drawPath(path, color, style = Stroke(1.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

internal val DocumentCorner = 12.dp
