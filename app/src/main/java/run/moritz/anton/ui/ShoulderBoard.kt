package run.moritz.anton.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import run.moritz.anton.progress.Insignia
import run.moritz.anton.progress.Metal

private val BoardTop = Color(0xFF16345C)
private val BoardBottom = Color(0xFF0B2545)
private val BoardEdge = Color(0xFF071A33)
private val BoardStitch = Color(0xFF2C4F80)

private data class MetalShades(val light: Color, val base: Color, val dark: Color)

private val Metal.shades: MetalShades
    get() = when (this) {
        Metal.BLUE -> MetalShades(Color(0xFF7FB2F0), Color(0xFF3A7BD5), Color(0xFF235AA6))
        Metal.SILVER -> MetalShades(Color(0xFFFFFFFF), Color(0xFFC9CED6), Color(0xFF858C96))
        Metal.GOLD -> MetalShades(Color(0xFFFFF1A8), Color(0xFFE0B83A), Color(0xFF9A7414))
    }

/** A Berlin shoulder board, lying on its side with the button end on the right. */
@Composable
fun ShoulderBoard(insignia: Insignia, description: String, modifier: Modifier = Modifier) {
    Canvas(modifier.aspectRatio(2.6f).semantics { contentDescription = description }) {
        val h = size.height
        val w = size.width
        val shades = insignia.metal.shades
        drawPath(boardOutline(w, h, 0f), BoardEdge)
        val inset = h * 0.035f
        drawPath(boardOutline(w, h, inset), Brush.verticalGradient(listOf(BoardTop, BoardBottom)))
        drawPath(boardOutline(w, h, inset * 2.2f), BoardStitch, style = Stroke(h * 0.012f))

        val button = Offset(w - h * 0.36f, h / 2)
        drawCircle(shades.dark, h * 0.11f, button)
        drawCircle(shades.base, h * 0.09f, button)
        drawCircle(shades.light, h * 0.035f, button - Offset(h * 0.03f, h * 0.03f))

        if (insignia.stripe) {
            val left = w * 0.1f
            val stripeWidth = h * 0.3f
            drawRect(shades.dark, Offset(left, 0f), Size(stripeWidth, h))
            drawRect(
                Brush.verticalGradient(listOf(shades.light, shades.base, shades.dark)),
                Offset(left + h * 0.015f, 0f),
                Size(stripeWidth - h * 0.03f, h)
            )
        }

        if (insignia.wreath) {
            wreathWithStars(insignia.stars, button.x, h, shades)
        } else {
            val available = w - h * 1.15f
            val starRadius = minOf(h * 0.27f, available / (insignia.stars + 0.6f) * 0.5f)
            val spacing = minOf(starRadius * 2.05f, available / insignia.stars.coerceAtLeast(1))
            val start = button.x - h * 0.38f - spacing * (insignia.stars - 1) - starRadius * 0.4f
            repeat(insignia.stars) { index ->
                star(Offset(start + index * spacing, h / 2), starRadius, shades)
            }
        }
    }
}

/** Straight at the shoulder seam on the left, rounded at the button end on the right. */
private fun boardOutline(w: Float, h: Float, inset: Float) = Path().apply {
    val radius = h * 0.5f - inset
    moveTo(inset, inset)
    lineTo(w - inset - radius, inset)
    arcTo(Rect(Offset(w - inset - radius * 2, inset), Size(radius * 2, radius * 2)), -90f, 180f, false)
    lineTo(inset, h - inset)
    close()
}

private fun DrawScope.star(center: Offset, radius: Float, shades: MetalShades) {
    val outline = Path()
    for (point in 0 until 10) {
        val r = if (point % 2 == 0) radius else radius * 0.42f
        val angle = -PI / 2 + point * PI / 5
        val x = center.x + r * cos(angle).toFloat()
        val y = center.y + r * sin(angle).toFloat()
        if (point == 0) outline.moveTo(x, y) else outline.lineTo(x, y)
    }
    outline.close()
    drawPath(
        outline,
        Brush.linearGradient(
            listOf(shades.light, shades.base, shades.dark),
            center - Offset(radius, radius),
            center + Offset(radius, radius)
        )
    )
}

/** The top ranks wear the first star inside a wreath and the others beside it. */
private fun DrawScope.wreathWithStars(stars: Int, buttonX: Float, h: Float, shades: MetalShades) {
    val center = Offset(h * 0.52f, h / 2)
    val wreathRadius = h * 0.34f
    val starRadius = h * 0.17f
    wreath(center, wreathRadius, shades)
    star(center, starRadius, shades)
    val first = center.x + wreathRadius + h * 0.34f
    val last = buttonX - h * 0.3f
    val others = stars - 1
    repeat(others) { index ->
        val x = if (others == 1) (first + last) / 2 else first + (last - first) * index / (others - 1)
        star(Offset(x, h / 2), starRadius, shades)
    }
}

/** Two laurel branches rise from the bottom. Their pointed leaves stand in pairs and grow towards the top. */
private fun DrawScope.wreath(center: Offset, radius: Float, shades: MetalShades) {
    val nodes = 7
    val start = 0.16
    val sweep = 2.42
    for (side in listOf(-1.0, 1.0)) {
        val stem = Path()
        for (step in 0..24) {
            val point = onArc(center, radius, side, start * 0.2 + (sweep + start * 0.8) * step / 24)
            if (step == 0) stem.moveTo(point.x, point.y) else stem.lineTo(point.x, point.y)
        }
        drawPath(stem, shades.dark, style = Stroke(radius * 0.045f))
        repeat(nodes) { node ->
            val along = start + sweep * node / (nodes - 1)
            val base = onArc(center, radius, side, along)
            val growth = onArc(center, radius, side, along + 0.02) - base
            val heading = atan2(growth.y, growth.x)
            val length = radius * (0.42f - 0.012f * node)
            if (node == nodes - 1) {
                leaf(base, heading, length, shades.light)
            } else {
                leaf(base, heading - LEAF_SPREAD, length, shades.base)
                leaf(base, heading + LEAF_SPREAD, length, shades.light)
            }
        }
    }
}

private const val LEAF_SPREAD = 0.62f

/** A point on the wreath, measured from its bottom. [side] is -1 for the right branch and 1 for the left. */
private fun onArc(center: Offset, radius: Float, side: Double, along: Double): Offset {
    val angle = PI / 2 + side * along
    return Offset(center.x + radius * cos(angle).toFloat(), center.y + radius * sin(angle).toFloat())
}

/** A pointed leaf from [base] in the direction [heading]. */
private fun DrawScope.leaf(base: Offset, heading: Float, length: Float, color: Color) {
    val along = Offset(cos(heading), sin(heading))
    val across = Offset(-along.y, along.x)
    val tip = base + along * length
    val middle = base + along * (length * 0.45f)
    val path = Path().apply {
        moveTo(base.x, base.y)
        val left = middle + across * (length * 0.34f)
        quadraticTo(left.x, left.y, tip.x, tip.y)
        val right = middle - across * (length * 0.34f)
        quadraticTo(right.x, right.y, base.x, base.y)
        close()
    }
    drawPath(path, color)
}
