package run.moritz.anton.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.core.graphics.createBitmap
import androidx.core.graphics.set
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

enum class Glyph { CLOSE, SETTINGS, UP, DOWN }

@Composable
fun GlyphIcon(glyph: Glyph, modifier: Modifier = Modifier, color: Color = Classic.Black) {
    Canvas(modifier) {
        val s = size.minDimension
        val c = Offset(size.width / 2, size.height / 2)
        when (glyph) {
            Glyph.CLOSE -> {
                val r = s * 0.3f
                val width = s * 0.17f
                drawLine(color, c + Offset(-r, -r), c + Offset(r, r), width)
                drawLine(color, c + Offset(r, -r), c + Offset(-r, r), width)
            }

            Glyph.SETTINGS -> {
                val width = s * 0.09f
                val top = s * 0.3f
                val bottom = s * 0.7f
                drawLine(color, Offset(s * 0.1f, top), Offset(s * 0.9f, top), width)
                drawLine(color, Offset(s * 0.1f, bottom), Offset(s * 0.9f, bottom), width)
                listOf(Offset(s * 0.65f, top), Offset(s * 0.35f, bottom)).forEach { knob ->
                    val half = s * 0.11f
                    drawRect(Classic.Window, knob - Offset(half, half), Size(half * 2, half * 2))
                    drawRect(color, knob - Offset(half, half), Size(half * 2, half * 2), style = Stroke(width))
                }
            }

            Glyph.UP, Glyph.DOWN -> {
                val direction = if (glyph == Glyph.UP) -1f else 1f
                val path = Path().apply {
                    moveTo(c.x, c.y + direction * s * 0.2f)
                    lineTo(c.x - s * 0.3f, c.y - direction * s * 0.12f)
                    lineTo(c.x + s * 0.3f, c.y - direction * s * 0.12f)
                    close()
                }
                drawPath(path, color)
            }
        }
    }
}

/** The navy bar on top of a window. It also reaches behind the system status bar. */
@Composable
fun TitleBar(title: String, modifier: Modifier = Modifier, action: @Composable RowScope.() -> Unit = {}) {
    Box(
        modifier
            .fillMaxWidth()
            .background(Brush.horizontalGradient(listOf(Classic.Title, Classic.Title, Classic.TitleEnd)))
    ) {
        Row(
            Modifier.statusBarsPadding().height(52.dp).fillMaxWidth().padding(start = 12.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Label(title, Modifier.weight(1f), Classic.TitleText, maxLines = 1)
            action()
        }
    }
}

@Composable
fun TitleBarButton(glyph: Glyph, description: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    Box(
        modifier
            .size(44.dp)
            .background(Classic.Window)
            .let { if (pressed) it.sunken() else it.raised() }
            .clickable(interaction, null, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center
    ) {
        GlyphIcon(glyph, Modifier.size(22.dp).offset { if (pressed) IntOffset(2, 2) else IntOffset.Zero })
    }
}

@Composable
fun ClassicButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    primary: Boolean = false,
    enabled: Boolean = true,
    height: Dp = 52.dp,
    style: TextStyle = if (primary) Classic.Strong else Classic.Body
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val nudge = if (pressed) 1.dp else 0.dp
    Box(
        modifier
            .heightIn(min = height)
            .background(if (primary) Classic.Black else Classic.Window)
            .padding(if (primary) 1.dp else 0.dp)
            .background(Classic.Window)
            .let { if (pressed) it.sunken() else it.raised() }
            .clickable(interaction, null, enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Label(
            text,
            Modifier.offset(nudge, nudge).padding(horizontal = 8.dp, vertical = 6.dp),
            style.copy(
                color = if (enabled) Classic.Black else Classic.Disabled,
                textAlign = TextAlign.Center
            )
        )
    }
}

/** A labelled frame with an etched line, like a Windows group box. */
@Composable
fun GroupBox(title: String, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Box(modifier.fillMaxWidth()) {
        Column(
            Modifier
                .padding(top = 10.dp)
                .fillMaxWidth()
                .drawBehind {
                    val line = bevelLine()
                    drawRect(
                        Classic.White,
                        Offset(line, line),
                        Size(size.width - line, size.height - line),
                        style = Stroke(line)
                    )
                    drawRect(
                        Classic.Shadow,
                        Offset.Zero,
                        Size(size.width - line, size.height - line),
                        style = Stroke(line)
                    )
                }
                .padding(start = 12.dp, end = 12.dp, top = 16.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            content = content
        )
        Label(
            title,
            Modifier.padding(start = 8.dp).background(Classic.Window).padding(horizontal = 4.dp),
            Classic.Strong
        )
    }
}

/** A read-only value in a sunken white field. */
@Composable
fun ValueField(text: String, modifier: Modifier = Modifier, style: TextStyle = Classic.Body, minHeight: Dp = 36.dp) {
    Box(
        modifier
            .heightIn(min = minHeight)
            .background(Classic.White)
            .sunken()
            .padding(horizontal = 8.dp, vertical = 5.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Label(text, style = style)
    }
}

/** A row of a form: label on the left, value on the right. */
@Composable
fun FormRow(label: String, labelWidth: Dp = 128.dp, value: @Composable RowScope.() -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Label(label, Modifier.width(labelWidth))
        value()
    }
}

/** A progress bar that fills block by block. */
@Composable
fun BlockProgress(filled: Float, modifier: Modifier = Modifier, blocks: Int = 20) {
    Box(modifier.fillMaxWidth().height(26.dp).background(Classic.White).sunken().padding(4.dp)) {
        Canvas(Modifier.fillMaxSize()) {
            val gap = 2.dp.toPx()
            val width = (size.width - gap * (blocks - 1)) / blocks
            repeat((filled.coerceIn(0f, 1f) * blocks).roundToInt()) { index ->
                val left = (index * (width + gap)).roundToInt().toFloat()
                drawRect(Classic.Title, Offset(left, 0f), Size(width.roundToInt().toFloat(), size.height))
            }
        }
    }
}

@Composable
fun StatusBar(modifier: Modifier = Modifier, panes: @Composable RowScope.() -> Unit) {
    Box(modifier.fillMaxWidth().background(Classic.Window)) {
        Row(
            Modifier.navigationBarsPadding().height(32.dp).fillMaxWidth().padding(horizontal = 12.dp, vertical = 3.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            content = panes
        )
    }
}

@Composable
fun StatusPane(
    text: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    color: Color = Classic.Black
) {
    Box(
        modifier
            .fillMaxHeight()
            .thinSunken()
            .let { if (onClick != null) it.clickable(role = Role.Button, onClick = onClick) else it }
            .padding(horizontal = 6.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Label(text, style = Classic.Small.copy(color = color), maxLines = 1)
    }
}

/** Grey 50 % dither behind modal windows, the look of a Windows shutdown screen. */
@Composable
private fun ditherBrush(): Brush {
    val cell = with(LocalDensity.current) { 1.dp.roundToPx().coerceAtLeast(1) }
    return remember(cell) {
        val bitmap = createBitmap(cell * 2, cell * 2)
        val dark = Classic.Black.copy(alpha = 0.75f).toArgb()
        for (x in 0 until cell * 2) {
            for (y in 0 until cell * 2) {
                val odd = (x / cell + y / cell) % 2 == 1
                bitmap[x, y] = if (odd) dark else 0
            }
        }
        ShaderBrush(ImageShader(bitmap.asImageBitmap(), TileMode.Repeated, TileMode.Repeated))
    }
}

/** A modal window in the middle of the screen. Touches never reach what lies behind it. */
@Composable
fun ClassicDialog(title: String, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .background(ditherBrush())
            .clickable(remember { MutableInteractionSource() }, null, onClick = {})
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(modifier.fillMaxWidth().background(Classic.Window).raised().padding(4.dp)) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .background(Brush.horizontalGradient(listOf(Classic.Title, Classic.TitleEnd)))
                    .padding(horizontal = 10.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Label(title, style = Classic.Strong.copy(color = Classic.White), maxLines = 1)
            }
            Column(
                Modifier.padding(horizontal = 12.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                content = content
            )
        }
    }
}

/** Two buttons of equal width at the bottom of a dialog. */
@Composable
fun DialogButtons(primary: String, onPrimary: () -> Unit, secondary: String, onSecondary: () -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        ClassicButton(secondary, onSecondary, Modifier.weight(1f))
        ClassicButton(primary, onPrimary, Modifier.weight(1f), primary = true)
    }
}

@Composable
fun WarningIcon(modifier: Modifier = Modifier) {
    Canvas(modifier.size(44.dp)) {
        val s = size.minDimension
        val triangle = Path().apply {
            moveTo(s / 2, s * 0.06f)
            lineTo(s * 0.97f, s * 0.92f)
            lineTo(s * 0.03f, s * 0.92f)
            close()
        }
        drawPath(triangle, Classic.Warning)
        drawPath(triangle, Classic.Black, style = Stroke(s * 0.04f))
        drawLine(Classic.Black, Offset(s / 2, s * 0.36f), Offset(s / 2, s * 0.64f), s * 0.09f)
        drawRect(Classic.Black, Offset(s * 0.455f, s * 0.7f), Size(s * 0.09f, s * 0.09f))
    }
}

@Composable
fun CheckBox(label: String, checked: Boolean, onChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp)
            .clickable(role = Role.Checkbox) { onChange(!checked) },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Canvas(Modifier.size(22.dp).background(Classic.White).sunken()) {
            if (!checked) return@Canvas
            val s = size.minDimension
            val path = Path().apply {
                moveTo(s * 0.22f, s * 0.5f)
                lineTo(s * 0.42f, s * 0.72f)
                lineTo(s * 0.78f, s * 0.28f)
            }
            drawPath(path, Classic.Black, style = Stroke(s * 0.13f))
        }
        Label(label, Modifier.weight(1f))
    }
}

@Composable
fun RadioButton(label: String, selected: Boolean, onSelect: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp)
            .clickable(role = Role.RadioButton, onClick = onSelect),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Canvas(Modifier.size(22.dp)) {
            val r = size.minDimension / 2
            val line = bevelLine()
            drawCircle(Classic.White, r)
            drawArc(Classic.Shadow, 135f, 180f, false, style = Stroke(line))
            drawArc(Classic.White, -45f, 180f, false, style = Stroke(line))
            drawCircle(Classic.DarkShadow, r - line * 1.5f, style = Stroke(line))
            if (selected) drawCircle(Classic.Black, r * 0.38f)
        }
        Label(label, Modifier.weight(1f))
    }
}

/** A classic scrollbar for a list with rows of equal height. The thumb can be dragged. */
@Composable
fun ClassicScrollbar(
    state: LazyListState,
    upDescription: String,
    downDescription: String,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val total by remember { derivedStateOf { state.layoutInfo.totalItemsCount } }
    val visible by remember { derivedStateOf { state.layoutInfo.visibleItemsInfo.size.coerceAtLeast(1) } }
    val share by remember(total) {
        derivedStateOf {
            if (total ==
                0
            ) {
                1f
            } else {
                (visible.toFloat() / total).coerceAtMost(1f)
            }
        }
    }
    val position by remember {
        derivedStateOf {
            val itemSize = state.layoutInfo.visibleItemsInfo.firstOrNull()?.size?.coerceAtLeast(1) ?: 1
            val range = (total - visible).coerceAtLeast(1)
            ((state.firstVisibleItemIndex + state.firstVisibleItemScrollOffset.toFloat() / itemSize) / range).coerceIn(
                0f,
                1f
            )
        }
    }
    Column(modifier.width(22.dp).fillMaxHeight()) {
        ScrollArrow(Glyph.UP, upDescription) {
            scope.launch { state.animateScrollToItem((state.firstVisibleItemIndex - 1).coerceAtLeast(0)) }
        }
        BoxWithConstraints(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Classic.Window)
                .pointerInput(total, visible) {
                    detectVerticalDragGestures { _, dragAmount ->
                        val itemSize = state.layoutInfo.visibleItemsInfo.firstOrNull()?.size ?: 1
                        val trackRows = size.height.toFloat() / (total.coerceAtLeast(1))
                        scope.launch { state.scrollBy(dragAmount / trackRows * itemSize) }
                    }
                }
        ) {
            val thumbHeight = (maxHeight * share).coerceAtLeast(32.dp)
            val travel = with(LocalDensity.current) { (maxHeight - thumbHeight).toPx() }
            Box(
                Modifier
                    .offset { IntOffset(0, (travel * position).roundToInt()) }
                    .fillMaxWidth()
                    .height(thumbHeight)
                    .background(Classic.Window)
                    .raised()
            )
        }
        ScrollArrow(Glyph.DOWN, downDescription) {
            scope.launch { state.animateScrollToItem(state.firstVisibleItemIndex + 1) }
        }
    }
}

@Composable
private fun ScrollArrow(glyph: Glyph, description: String, onClick: () -> Unit) {
    Box(
        Modifier
            .size(22.dp)
            .background(Classic.Window)
            .raised()
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center
    ) {
        GlyphIcon(glyph, Modifier.size(14.dp))
    }
}

@Composable
fun VerticalSpace(height: Dp) = Spacer(Modifier.height(height))
