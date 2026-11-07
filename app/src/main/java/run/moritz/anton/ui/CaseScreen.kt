package run.moritz.anton.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import java.time.LocalDate
import run.moritz.anton.R
import run.moritz.anton.cases.Case
import run.moritz.anton.cases.Field
import run.moritz.anton.cases.FieldKind
import run.moritz.anton.cases.Sex
import run.moritz.anton.progress.Progress
import run.moritz.anton.progress.Rank
import run.moritz.anton.spelling.Evaluation
import run.moritz.anton.spelling.SpellingTable

private const val TILES_PER_ROW = 11
private const val WRONG_WORD_MARK = "?"

@Composable
fun CaseScreen(model: AntonViewModel, onTalk: (Boolean) -> Unit) {
    val ui = model.caseUi ?: return
    BackHandler {
        when (ui.dialog) {
            is CaseDialog.Plausibility -> model.repeatField()
            CaseDialog.TableWarning, CaseDialog.LeaveWarning -> model.dismissDialog()
            is CaseDialog.Result, is CaseDialog.Certificate -> model.leaveCase()
            null -> model.requestLeaveCase()
        }
    }
    // Sounds would reach the open microphone, so a field confirmed mid-transmission is felt instead.
    val haptics = LocalHapticFeedback.current
    LaunchedEffect(ui.fieldIndex) {
        if (ui.fieldIndex > 0) haptics.performHapticFeedback(HapticFeedbackType.Confirm)
    }
    val drill = ui.case as? Case.Drill
    val title = if (drill != null) {
        stringResource(R.string.drill_title, drill.symbol)
    } else {
        stringResource(R.string.case_title, ui.number)
    }
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().background(Classic.Window)) {
            TitleBar(title) {
                TitleBarButton(Glyph.CLOSE, stringResource(R.string.close_case), onClick = model::requestLeaveCase)
            }
            WorkArea(
                Modifier.weight(1f),
                paper = {
                    Column(
                        Modifier.verticalScroll(rememberScrollState()).padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Document(ui)
                        RadioLog(ui, model.table)
                    }
                },
                talk = { TalkButton(ui, model.settings.volumeKeysTalk, onTalk) }
            )
            CaseStatusBar(ui, model)
        }
        ui.dialog?.let { CaseDialogHost(it, ui, model) }
    }
}

/** The paperwork scrolls when it must. The talk key takes all room that is left, so it is easy to hit. */
@Composable
private fun WorkArea(modifier: Modifier, paper: @Composable () -> Unit, talk: @Composable () -> Unit) {
    Layout({
        Box { paper() }
        Box { talk() }
    }, modifier) { (paperPart, talkPart), constraints ->
        val minTalk = TALK_MIN_HEIGHT.roundToPx()
        val height = constraints.maxHeight
        val paperPlaced = paperPart.measure(constraints.copy(minHeight = 0, maxHeight = height - minTalk))
        val talkHeight = height - paperPlaced.height
        val talkPlaced = talkPart.measure(constraints.copy(minHeight = talkHeight, maxHeight = talkHeight))
        layout(constraints.maxWidth, height) {
            paperPlaced.place(0, 0)
            talkPlaced.place(0, paperPlaced.height)
        }
    }
}

@Composable
private fun Document(ui: CaseUi) {
    when (val case = ui.case) {
        is Case.IdCard -> IdCard(case, ui.field.kind.takeUnless { ui.finished }, Modifier.testTag("id_card"))

        is Case.DrivingLicence -> DrivingLicence(
            case,
            ui.field.kind.takeUnless { ui.finished },
            Modifier.testTag("driving_licence")
        )

        is Case.Plate -> GroupBox(stringResource(R.string.group_plate)) {
            LicencePlate(case, Modifier.testTag("plate"))
        }

        is Case.Drill -> GroupBox(stringResource(R.string.group_drill)) {
            ValueField(
                ui.field.text,
                Modifier.fillMaxWidth().testTag("drill_sheet"),
                Classic.Radio.copy(fontSize = 26.sp, lineHeight = 32.sp),
                minHeight = 72.dp
            )
        }
    }
}

@Composable
private fun fieldLabel(field: Field, index: Int): String = when (field.kind) {
    FieldKind.SURNAME -> stringResource(R.string.field_surname)
    FieldKind.BIRTH_DATE -> stringResource(R.string.field_birth_date)
    FieldKind.FIRST_NAME -> stringResource(R.string.field_first_name)
    FieldKind.PLATE -> stringResource(R.string.field_plate)
    FieldKind.DRILL -> stringResource(R.string.field_drill, index + 1)
}

/** Fields as Windows tabs: done ones carry a tick and the open one sits in front. */
@Composable
private fun RadioLog(ui: CaseUi, table: SpellingTable) {
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(start = 3.dp).zIndex(1f), verticalAlignment = Alignment.Bottom) {
            ui.case.fields.forEachIndexed { index, field ->
                FieldTab(
                    fieldLabel(field, index),
                    active = index == ui.fieldIndex,
                    done = index < ui.fieldIndex,
                    compact = ui.case.fields.size > 3
                )
            }
        }
        Column(
            Modifier.fillMaxWidth().background(Classic.Window).raised().padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Tiles(ui, table)
            LogLine(ui, table)
        }
    }
}

@Composable
private fun FieldTab(label: String, active: Boolean, done: Boolean, compact: Boolean) {
    Row(
        Modifier
            .offset(y = if (active) 2.dp else 0.dp)
            .heightIn(min = if (active) 36.dp else 32.dp)
            .background(Classic.Window)
            .drawWithContent {
                drawContent()
                val line = bevelLine()
                drawRect(Classic.White, Offset.Zero, Size(size.width, line))
                drawRect(Classic.White, Offset.Zero, Size(line, size.height))
                drawRect(Classic.DarkShadow, Offset(size.width - line, 0f), Size(line, size.height))
                drawRect(Classic.Shadow, Offset(size.width - line * 2, line), Size(line, size.height))
            }
            .padding(horizontal = if (compact) 6.dp else 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        if (done) {
            Canvas(Modifier.size(12.dp)) {
                val s = size.minDimension
                val tick = Path().apply {
                    moveTo(s * 0.1f, s * 0.55f)
                    lineTo(s * 0.4f, s * 0.85f)
                    lineTo(s * 0.92f, s * 0.15f)
                }
                drawPath(tick, Classic.Confirmed, style = Stroke(s * 0.2f))
            }
        }
        Label(label, style = if (active) Classic.Strong.copy(fontSize = 14.sp) else Classic.Small, maxLines = 1)
    }
}

@Composable
private fun Tiles(ui: CaseUi, table: SpellingTable) {
    if (ui.finished) return
    val target = printed(ui.field.spoken).toList()
    val known = table.entries.map { it.symbol }
    val sent = printed(ui.sentSymbols.joinToString("") { if (it in known) it else WRONG_WORD_MARK }).toList()
    Column(Modifier.testTag("tiles"), verticalArrangement = Arrangement.spacedBy(3.dp)) {
        target.chunked(TILES_PER_ROW).forEachIndexed { row, cells ->
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                cells.indices.forEach { column ->
                    val index = row * TILES_PER_ROW + column
                    LetterCell(
                        sent.getOrNull(index)?.toString().orEmpty(),
                        Classic.White,
                        Classic.Black,
                        Modifier.weight(1f)
                    )
                }
                repeat(TILES_PER_ROW - cells.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun LetterCell(letter: String, background: Color, color: Color, modifier: Modifier) {
    Box(modifier.height(34.dp).background(background).thinSunken(), contentAlignment = Alignment.Center) {
        Label(letter, style = Classic.Tile.copy(color = color), maxLines = 1)
    }
}

@Composable
private fun LogLine(ui: CaseUi, table: SpellingTable) {
    val words = ui.sentSymbols.map { symbol -> table.entries.firstOrNull { it.symbol == symbol }?.word ?: symbol }
    val text = when {
        ui.notice == LogNotice.NOT_UNDERSTOOD -> stringResource(R.string.log_not_understood)

        ui.notice == LogNotice.MICROPHONE_FAILED -> stringResource(R.string.log_microphone_failed)

        words.isNotEmpty() -> (if (words.size > 3) "… " else "") + words.takeLast(3).joinToString(", ")

        ui.notice == LogNotice.CONFIRMED && !ui.finished ->
            stringResource(R.string.log_confirmed) + " " +
                stringResource(R.string.log_waiting, fieldLabel(ui.field, ui.fieldIndex))

        ui.finished -> stringResource(R.string.log_confirmed)

        else -> stringResource(R.string.log_waiting, fieldLabel(ui.field, ui.fieldIndex))
    }
    val failed = ui.notice == LogNotice.NOT_UNDERSTOOD || ui.notice == LogNotice.MICROPHONE_FAILED
    ValueField(
        text,
        Modifier.fillMaxWidth().testTag("log"),
        Classic.Radio.copy(color = if (failed) Classic.Error else Classic.Black, fontSize = 14.sp)
    )
}

/** Push-to-talk: transmits for as long as the finger stays down. Built like the front of a radio. */
@Composable
private fun TalkButton(ui: CaseUi, keysTalk: Boolean, onTalk: (Boolean) -> Unit) {
    val sending = ui.phase == TransmissionPhase.SENDING
    val evaluating = ui.phase == TransmissionPhase.EVALUATING
    val nudge = if (sending) 2.dp else 0.dp
    Box(Modifier.fillMaxSize().padding(start = 12.dp, end = 12.dp, bottom = 10.dp)) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Classic.Black)
                .padding(1.dp)
                .background(Classic.Window)
                .let { if (sending) it.sunken() else it.raised() }
                .semantics { role = Role.Button }
                .testTag("talk")
                .pointerInput(onTalk) {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false)
                        onTalk(true)
                        waitForUpOrCancellation()
                        onTalk(false)
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            SpeakerGrille(Modifier.fillMaxSize().padding(14.dp).offset(nudge, nudge))
            Column(
                Modifier
                    .offset(nudge, nudge)
                    .background(Classic.Window)
                    .raised()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .width(264.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Lamp(sending)
                    val label = when {
                        sending -> R.string.talk_sending
                        evaluating -> R.string.talk_evaluating
                        else -> R.string.talk
                    }
                    Label(stringResource(label), style = Classic.Strong.copy(fontSize = 21.sp, lineHeight = 26.sp))
                    Spacer(Modifier.size(LAMP_SIZE))
                }
                val hint = if (keysTalk) R.string.talk_hint else R.string.talk_hint_no_keys
                Label(
                    stringResource(hint),
                    style = Classic.Small.copy(fontSize = 14.sp, lineHeight = 18.sp),
                    maxLines = 1
                )
            }
        }
    }
}

private val LAMP_SIZE = 18.dp
private val TALK_MIN_HEIGHT = 138.dp

/** The perforated metal in front of a radio speaker. */
@Composable
private fun SpeakerGrille(modifier: Modifier) {
    Canvas(modifier) {
        val pitch = 11.dp.toPx()
        val radius = 2.6.dp.toPx()
        val highlight = bevelLine()
        val columns = (size.width / pitch).toInt()
        val rows = (size.height / pitch).toInt()
        val left = (size.width - (columns - 1) * pitch) / 2
        val top = (size.height - (rows - 1) * pitch) / 2
        for (row in 0 until rows) {
            for (column in 0 until columns) {
                val hole = Offset(left + column * pitch, top + row * pitch)
                drawCircle(Classic.White, radius, hole + Offset(highlight, highlight))
                drawCircle(Classic.DarkShadow, radius, hole)
            }
        }
    }
}

/** The send lamp of the radio. */
@Composable
private fun Lamp(on: Boolean) {
    Canvas(Modifier.size(LAMP_SIZE).testTag(if (on) "lamp_on" else "lamp_off")) {
        val r = size.minDimension / 2
        drawCircle(Classic.DarkShadow, r)
        drawCircle(if (on) Color(0xFFFF2A1A) else Color(0xFF6E2A24), r * 0.78f)
        drawCircle(Color.White.copy(alpha = if (on) 0.8f else 0.25f), r * 0.22f, center - Offset(r * 0.28f, r * 0.28f))
    }
}

@Composable
private fun CaseStatusBar(ui: CaseUi, model: AntonViewModel) {
    StatusBar {
        val status = when {
            ui.phase == TransmissionPhase.SENDING -> R.string.status_sending
            ui.phase == TransmissionPhase.EVALUATING -> R.string.status_evaluating
            ui.notice == LogNotice.NOT_UNDERSTOOD -> R.string.status_not_understood
            else -> R.string.status_ready
        }
        StatusPane(stringResource(status), Modifier.weight(1f))
        val total = ui.case.fields.size
        StatusPane(
            stringResource(R.string.status_field, (ui.fieldIndex + 1).coerceAtMost(total), total),
            Modifier.width(76.dp)
        )
        if (ui.case !is Case.Drill) {
            StatusPane(
                stringResource(R.string.status_table),
                Modifier.width(64.dp).testTag("case_table"),
                onClick = model::requestTableFromCase,
                color = Classic.Title
            )
        }
    }
}

@Composable
private fun CaseDialogHost(dialog: CaseDialog, ui: CaseUi, model: AntonViewModel) {
    when (dialog) {
        is CaseDialog.Plausibility -> PlausibilityDialog(dialog.wrong, ui, model)

        CaseDialog.TableWarning -> ConfirmDialog(
            stringResource(R.string.table_warning_title),
            stringResource(R.string.table_warning_body),
            stringResource(R.string.table_warning_confirm),
            model::confirmTable,
            model::dismissDialog
        )

        CaseDialog.LeaveWarning -> ConfirmDialog(
            stringResource(R.string.leave_warning_title),
            stringResource(R.string.leave_warning_body),
            stringResource(R.string.leave_warning_confirm),
            model::leaveCase,
            model::dismissDialog
        )

        is CaseDialog.Result -> ResultDialog(dialog, ui, model)

        is CaseDialog.Certificate -> CertificateDialog(dialog.rank, model)
    }
}

@Composable
private fun ConfirmDialog(title: String, body: String, confirm: String, onConfirm: () -> Unit, onCancel: () -> Unit) {
    ClassicDialog(title) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            WarningIcon()
            Label(body, Modifier.weight(1f))
        }
        DialogButtons(confirm, onConfirm, stringResource(R.string.cancel), onCancel)
    }
}

@Composable
private fun PlausibilityDialog(wrong: Evaluation.Wrong, ui: CaseUi, model: AntonViewModel) {
    val spoken = printed(ui.field.spoken).toList()
    ClassicDialog(stringResource(R.string.plausibility_title), Modifier.testTag("plausibility")) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            WarningIcon()
            Column(Modifier.weight(1f)) {
                Label(
                    stringResource(R.string.plausibility_heading, fieldLabel(ui.field, ui.fieldIndex)),
                    style = Classic.Strong
                )
                Label(
                    if (wrong.position < spoken.size) {
                        stringResource(R.string.plausibility_position, wrong.position + 1, spoken.size)
                    } else {
                        stringResource(R.string.plausibility_extra)
                    }
                )
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            spoken.chunked(TILES_PER_ROW).forEachIndexed { row, cells ->
                Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    cells.forEachIndexed { column, letter ->
                        val index = row * TILES_PER_ROW + column
                        val wrongCell = index == wrong.position
                        LetterCell(
                            if (index <= wrong.position) letter.toString() else "",
                            if (wrongCell) Classic.Error else Classic.White,
                            if (wrongCell) Classic.White else Classic.Black,
                            Modifier.weight(1f)
                        )
                    }
                    repeat(TILES_PER_ROW - cells.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
        val radio = Classic.Radio
        FormRow(stringResource(R.string.plausibility_sent), labelWidth = 96.dp) {
            ValueField(
                wrong.sentWord,
                Modifier.weight(1f).testTag("sent_word"),
                radio.copy(textDecoration = TextDecoration.LineThrough)
            )
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Label(stringResource(R.string.plausibility_expected), Modifier.width(96.dp), Classic.Strong)
            ValueField(
                wrong.expectedWord ?: stringResource(R.string.plausibility_end_of_field),
                Modifier.weight(1f).testTag("expected_word"),
                radio
            )
        }
        DialogButtons(
            stringResource(R.string.repeat),
            model::repeatField,
            stringResource(R.string.that_was_right),
            model::overruleField
        )
    }
}

@Composable
private fun ResultDialog(result: CaseDialog.Result, ui: CaseUi, model: AntonViewModel) {
    val drill = ui.case as? Case.Drill
    if (drill != null) {
        ClassicDialog(stringResource(R.string.drill_result_title), Modifier.testTag("drill_result")) {
            Label(stringResource(R.string.drill_result_body, drill.symbol))
            MasteryLine(model.progress.masteryOf(drill.symbol))
            DialogButtons(
                stringResource(R.string.drill_again),
                model::drillAgain,
                stringResource(R.string.to_table),
                model::drillFinishedToTable
            )
        }
        return
    }
    val progress = model.progress
    ClassicDialog(stringResource(R.string.result_title), Modifier.testTag("result")) {
        Label(
            stringResource(if (result.errorFree) R.string.result_error_free else R.string.result_with_errors),
            style = Classic.Strong.copy(color = if (result.errorFree) Classic.Confirmed else Classic.Black)
        )
        FormRow(stringResource(R.string.result_streak)) {
            ValueField(progress.streak.toString(), Modifier.weight(1f), Classic.Strong)
        }
        val next = progress.rank.next
        val needed = progress.rank.casesToNext
        if (next != null && needed != null) {
            Label(
                stringResource(
                    R.string.result_until_rank,
                    rankTitle(next, model.settings.titleForm),
                    needed - progress.casesTowardNextRank
                )
            )
            BlockProgress(progress.casesTowardNextRank.toFloat() / needed)
        }
        DialogButtons(
            stringResource(R.string.next_case),
            model::startNextCase,
            stringResource(R.string.to_station),
            model::leaveCase
        )
    }
}

@Composable
private fun MasteryLine(mastery: Int) {
    Label(
        "■".repeat(mastery) + "□".repeat(Progress.MAX_MASTERY - mastery),
        style = Classic.Radio.copy(fontSize = 22.sp, color = Classic.Title)
    )
}

@Composable
private fun CertificateDialog(rank: Rank, model: AntonViewModel) {
    val form = model.settings.titleForm
    val title = rankTitle(rank, form)
    val serif = TextStyle(fontFamily = Classic.Serif, color = Classic.Black, textAlign = TextAlign.Center)
    ClassicDialog(stringResource(R.string.certificate_title), Modifier.testTag("certificate")) {
        Column(
            Modifier.fillMaxWidth().background(Classic.White).sunken().padding(horizontal = 18.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Label(
                stringResource(R.string.certificate_heading),
                style = serif.copy(fontSize = 26.sp, fontWeight = FontWeight.Bold, letterSpacing = 3.sp)
            )
            ShoulderBoard(rank.insignia, stringResource(R.string.insignia_description, title), Modifier.height(60.dp))
            val body = if (form == Sex.FEMALE) R.string.certificate_body_female else R.string.certificate_body_male
            Label(stringResource(body), style = serif.copy(fontSize = 18.sp, lineHeight = 25.sp))
            Label(
                title,
                Modifier.wrapContentHeight(),
                serif.copy(fontSize = 26.sp, lineHeight = 30.sp, fontWeight = FontWeight.Bold)
            )
            val today = remember { LocalDate.now().format(Case.DATE_FORMAT) }
            Label(stringResource(R.string.certificate_date, today), style = serif.copy(fontSize = 15.sp))
        }
        Label(unlockText(rank))
        DialogButtons(
            stringResource(R.string.next_case),
            model::startNextCase,
            stringResource(R.string.to_station),
            model::leaveCase
        )
    }
}

@Composable
private fun unlockText(rank: Rank): String = stringResource(
    when {
        !rank.unlocksMaterial -> R.string.unlock_more
        rank.materialLevel == 2 -> R.string.unlock_traps
        rank.materialLevel == 3 -> R.string.unlock_international
        else -> R.string.unlock_rare
    }
)
