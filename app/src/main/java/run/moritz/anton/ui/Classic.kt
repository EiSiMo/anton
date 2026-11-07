package run.moritz.anton.ui

import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import kotlin.math.floor
import run.moritz.anton.R

/** The look of classic Windows government software: tokens shared by every screen. */
object Classic {
    val Window = Color(0xFFD4D0C8)
    val Title = Color(0xFF0A246A)
    val TitleEnd = Color(0xFF2E5AA8)
    val White = Color(0xFFFFFFFF)
    val Black = Color(0xFF000000)
    val Shadow = Color(0xFF808080)
    val DarkShadow = Color(0xFF404040)
    val Error = Color(0xFFB00000)
    val Warning = Color(0xFFFFD800)
    val Confirmed = Color(0xFF00703C)
    val Disabled = Color(0xFF808080)

    val Sans = FontFamily(Font(R.font.tahoma_regular), Font(R.font.tahoma_bold, FontWeight.Bold))
    val Mono = FontFamily(Font(R.font.liberation_mono_bold, FontWeight.Bold))
    val Serif = FontFamily(Font(R.font.liberation_serif_regular), Font(R.font.liberation_serif_bold, FontWeight.Bold))
    val Print = FontFamily(Font(R.font.liberation_sans_regular), Font(R.font.liberation_sans_bold, FontWeight.Bold))

    val Body = TextStyle(fontFamily = Sans, fontSize = 15.sp, lineHeight = 20.sp, color = Black)
    val Strong = Body.copy(fontWeight = FontWeight.Bold)
    val Small = Body.copy(fontSize = 13.sp, lineHeight = 16.sp)
    val TitleText = Strong.copy(fontSize = 16.sp, color = White)
    val Radio = TextStyle(fontFamily = Mono, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Black)
    val Tile = Radio.copy(fontSize = 18.sp)
}

@Composable
fun Label(text: String, modifier: Modifier = Modifier, style: TextStyle = Classic.Body, maxLines: Int = Int.MAX_VALUE) {
    BasicText(text, modifier, style, maxLines = maxLines, overflow = TextOverflow.Ellipsis)
}

/** The width of one bevel line: a whole number of device pixels, so edges stay crisp. */
internal fun DrawScope.bevelLine(): Float = floor(density).coerceAtLeast(1f)

private fun DrawScope.edge(inset: Float, line: Float, topLeft: Color, bottomRight: Color) {
    val width = size.width - 2 * inset
    val height = size.height - 2 * inset
    drawRect(bottomRight, Offset(inset, size.height - inset - line), Size(width, line))
    drawRect(bottomRight, Offset(size.width - inset - line, inset), Size(line, height))
    drawRect(topLeft, Offset(inset, inset), Size(width - line, line))
    drawRect(topLeft, Offset(inset, inset), Size(line, height - line))
}

/** A control that stands out of the window. */
fun Modifier.raised(): Modifier = drawWithContent {
    drawContent()
    val line = bevelLine()
    edge(0f, line, Classic.White, Classic.DarkShadow)
    edge(line, line, Classic.Window, Classic.Shadow)
}

/** A field that is let into the window. */
fun Modifier.sunken(): Modifier = drawWithContent {
    drawContent()
    val line = bevelLine()
    edge(0f, line, Classic.Shadow, Classic.White)
    edge(line, line, Classic.DarkShadow, Classic.Window)
}

/** The thin single bevel of status bar panes and letter cells. */
fun Modifier.thinSunken(): Modifier = drawWithContent {
    drawContent()
    edge(0f, bevelLine(), Classic.Shadow, Classic.White)
}

/** The thin raised bevel of column headers. */
fun Modifier.thinRaised(): Modifier = drawWithContent {
    drawContent()
    edge(0f, bevelLine(), Classic.White, Classic.Shadow)
}
