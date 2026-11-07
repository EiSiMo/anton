package run.moritz.anton.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextGeometricTransform
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import run.moritz.anton.R
import run.moritz.anton.cases.Case

private val InspectionColors = listOf(
    Color(0xFFE85D9E),
    Color(0xFF3FA34D),
    Color(0xFFF08A24),
    Color(0xFF3A7BD5),
    Color(0xFFF2D024),
    Color(0xFF8B5A2B)
)

/** A German EU licence plate. */
@Composable
fun LicencePlate(plate: Case.Plate, modifier: Modifier = Modifier) {
    val description = stringResource(R.string.plate_description, plate.fields.first().text)
    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .aspectRatio(4.6f)
            .clip(RoundedCornerShape(8.dp))
            .background(Color.Black)
            .padding(3.dp)
            .semantics { contentDescription = description }
    ) {
        val unit = maxHeight / 100
        val textSize = with(LocalDensity.current) { (unit * 74).toSp() }
        Row(
            Modifier.fillMaxSize().clip(RoundedCornerShape(5.dp)).background(Color.White),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                Modifier.fillMaxHeight().width(unit * 48).background(EuBlue).padding(vertical = unit * 6),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Canvas(Modifier.size(unit * 42)) {
                    val ring = size.minDimension * 0.4f
                    val center = Offset(size.width / 2, size.height / 2)
                    repeat(12) { star ->
                        val angle = star * PI / 6
                        val at = center + Offset(ring * cos(angle).toFloat(), ring * sin(angle).toFloat())
                        drawCircle(EuYellow, size.minDimension * 0.058f, at)
                    }
                }
                val letter = with(LocalDensity.current) { (unit * 34).toSp() }
                Label(
                    stringResource(R.string.plate_country),
                    style = TextStyle(
                        fontFamily = Classic.Print,
                        fontWeight = FontWeight.Bold,
                        fontSize = letter,
                        color = Color.White
                    )
                )
            }
            val style = TextStyle(
                fontFamily = Classic.Print,
                fontWeight = FontWeight.Bold,
                fontSize = textSize,
                color = Color.Black,
                textGeometricTransform = TextGeometricTransform(scaleX = 0.92f)
            )
            Row(
                Modifier.weight(1f).padding(horizontal = unit * 8),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Label(plate.district, style = style, maxLines = 1)
                Seals(plate, Modifier.padding(horizontal = unit * 2), unit * 26)
                Label(plate.letters, style = style, maxLines = 1)
                Label(plate.digits, style = style, maxLines = 1)
            }
        }
    }
}

/** The inspection sticker above the district seal. */
@Composable
private fun Seals(plate: Case.Plate, modifier: Modifier, diameter: Dp) {
    val colorIndex = Math.floorMod(plate.hashCode(), InspectionColors.size)
    Column(modifier, verticalArrangement = Arrangement.spacedBy(diameter / 5)) {
        Box(Modifier.size(diameter)) {
            Canvas(Modifier.fillMaxSize()) {
                val r = size.minDimension / 2
                drawCircle(InspectionColors[colorIndex], r)
                repeat(12) { hour ->
                    val angle = hour * PI / 6
                    val at = center + Offset(r * 0.74f * cos(angle).toFloat(), r * 0.74f * sin(angle).toFloat())
                    drawCircle(Color.Black, r * 0.07f, at)
                }
                drawCircle(Color.Black, r * 0.25f)
            }
        }
        Canvas(Modifier.size(diameter)) {
            val r = size.minDimension / 2
            drawCircle(Color(0xFFE9E9E9), r)
            drawCircle(Color(0xFF7A7A7A), r, style = Stroke(r * 0.1f))
            drawCircle(Color(0xFF9A9A9A), r * 0.42f)
        }
    }
}
