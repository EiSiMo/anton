package run.moritz.anton.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import run.moritz.anton.R
import run.moritz.anton.cases.Sex
import run.moritz.anton.progress.Progress
import run.moritz.anton.progress.Rank
import run.moritz.anton.settings.Settings
import run.moritz.anton.spelling.SpellingEntry

/** The title of a rank in the form the player chose. */
@Composable
fun rankTitle(rank: Rank, form: Sex): String {
    val titles = stringArrayResource(if (form == Sex.FEMALE) R.array.rank_titles_female else R.array.rank_titles_male)
    return titles[rank.ordinal]
}

@Composable
fun StationScreen(model: AntonViewModel) {
    val progress = model.progress
    val form = model.settings.titleForm
    val title = rankTitle(progress.rank, form)
    Column(Modifier.fillMaxSize().background(Classic.Window)) {
        TitleBar(stringResource(R.string.station_title)) {
            TitleBarButton(
                Glyph.SETTINGS,
                stringResource(R.string.settings),
                Modifier.testTag("settings_open"),
                model::openSettings
            )
        }
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            GroupBox(stringResource(R.string.master_data)) {
                ShoulderBoard(
                    progress.rank.insignia,
                    stringResource(R.string.insignia_description, title),
                    Modifier.align(Alignment.CenterHorizontally).height(72.dp)
                )
                FormRow(stringResource(R.string.rank_label)) {
                    ValueField(title, Modifier.weight(1f).testTag("rank"), Classic.Strong)
                }
                FormRow(stringResource(R.string.next_rank_label)) {
                    val next = progress.rank.next
                    ValueField(
                        if (next !=
                            null
                        ) {
                            rankTitle(next, form)
                        } else {
                            stringResource(R.string.highest_rank)
                        },
                        Modifier.weight(1f)
                    )
                }
            }
            GroupBox(stringResource(R.string.work_status)) {
                val needed = progress.rank.casesToNext
                if (needed != null) {
                    Label(stringResource(R.string.cases_toward_rank, progress.casesTowardNextRank, needed))
                }
                BlockProgress(if (needed != null) progress.casesTowardNextRank.toFloat() / needed else 1f)
                FigureRow(stringResource(R.string.streak_label), progress.streak, large = true, tag = "streak")
                FigureRow(stringResource(R.string.best_streak), progress.bestStreak)
                FigureRow(stringResource(R.string.cases_completed), progress.casesCompleted)
            }
            GroupBox(stringResource(R.string.weak_letters)) {
                WeakLetters(model)
            }
        }
        Column(
            Modifier.padding(start = 12.dp, end = 12.dp, bottom = 10.dp, top = 4.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ClassicButton(
                stringResource(R.string.next_case),
                model::startNextCase,
                Modifier.fillMaxWidth().testTag("next_case"),
                primary = true,
                height = 64.dp,
                style = Classic.Strong.copy(fontSize = 17.sp)
            )
            ClassicButton(
                stringResource(R.string.spelling_table),
                model::openTable,
                Modifier.fillMaxWidth().testTag("open_table"),
                height = 48.dp
            )
        }
        StatusBar {
            StatusPane(stringResource(R.string.status_ready), Modifier.weight(1f))
            StatusPane(stringResource(R.string.status_streak, progress.streak), Modifier.width(96.dp))
        }
    }
}

@Composable
private fun FigureRow(label: String, value: Int, large: Boolean = false, tag: String = "") {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Label(label, Modifier.weight(1f))
        Label(
            value.toString(),
            Modifier.testTag(tag),
            if (large) Classic.Strong.copy(fontSize = 22.sp, lineHeight = 26.sp) else Classic.Strong
        )
    }
}

@Composable
private fun WeakLetters(model: AntonViewModel) {
    val weak = model.dueSymbols.take(MAX_WEAK_LETTERS)
    if (weak.isEmpty()) {
        Label(stringResource(R.string.weak_letters_empty))
        return
    }
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        weak.forEach { symbol ->
            Box(
                Modifier
                    .weight(1f)
                    .height(40.dp)
                    .background(Classic.White)
                    .thinSunken()
                    .clickable(role = Role.Button) { model.startDrill(symbol) },
                contentAlignment = Alignment.Center
            ) {
                Label(symbol, style = Classic.Tile.copy(color = Classic.Error), maxLines = 1)
            }
        }
    }
}

private const val WEAK_MASTERY = 2
private const val MAX_WEAK_LETTERS = 8

@Composable
fun SpellingTableScreen(model: AntonViewModel) {
    BackHandler(onBack = model::closeTable)
    val inCase = model.caseUi != null
    val entries = model.table.entries
    Column(Modifier.fillMaxSize().background(Classic.Window)) {
        TitleBar(stringResource(R.string.spelling_table)) {
            TitleBarButton(Glyph.CLOSE, stringResource(R.string.table_close), onClick = model::closeTable)
        }
        Box(Modifier.weight(1f).padding(horizontal = 12.dp, vertical = 10.dp)) {
            Column(Modifier.fillMaxSize().background(Classic.White).sunken().padding(2.dp)) {
                Row(Modifier.fillMaxWidth().background(Classic.Window)) {
                    HeaderCell(stringResource(R.string.table_column_letter), Modifier.width(LETTER_COLUMN))
                    HeaderCell(stringResource(R.string.table_column_word), Modifier.weight(1f))
                    HeaderCell(stringResource(R.string.table_column_mastery), Modifier.width(MASTERY_COLUMN))
                    Spacer(Modifier.width(22.dp).height(28.dp).background(Classic.Window).thinRaised())
                }
                val list = rememberLazyListState()
                Row(Modifier.weight(1f)) {
                    LazyColumn(Modifier.weight(1f).testTag("table_list"), state = list) {
                        items(entries, key = { it.symbol }) { entry ->
                            TableRow(entry, model.progress.masteryOf(entry.symbol), inCase) {
                                model.startDrill(entry.symbol)
                            }
                        }
                    }
                    ClassicScrollbar(list, stringResource(R.string.scroll_up), stringResource(R.string.scroll_down))
                }
            }
        }
        StatusBar {
            val text = if (inCase) R.string.table_status_case else R.string.table_status
            StatusPane(stringResource(text, entries.size), Modifier.weight(1f))
        }
    }
}

private val LETTER_COLUMN = 56.dp
private val MASTERY_COLUMN = 96.dp

@Composable
private fun HeaderCell(text: String, modifier: Modifier) {
    Box(modifier.height(28.dp).thinRaised().padding(horizontal = 6.dp), contentAlignment = Alignment.CenterStart) {
        Label(text, style = Classic.Small.copy(fontWeight = FontWeight.Bold), maxLines = 1)
    }
}

@Composable
private fun TableRow(entry: SpellingEntry, mastery: Int, inCase: Boolean, onClick: () -> Unit) {
    val description = stringResource(R.string.table_row_description, entry.symbol, entry.word, mastery)
    Row(
        Modifier
            .fillMaxWidth()
            .height(46.dp)
            .let { if (inCase) it else it.clickable(role = Role.Button, onClick = onClick) }
            .semantics(mergeDescendants = true) { contentDescription = description }
            .testTag("row_${entry.symbol}")
            .drawBehind {
                val dot = bevelLine()
                var x = 0f
                while (x < size.width) {
                    drawRect(Classic.Shadow, Offset(x, size.height - dot), Size(dot, dot))
                    x += dot * 2
                }
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Label(entry.symbol, Modifier.width(LETTER_COLUMN).padding(horizontal = 6.dp), Classic.Strong)
        Label(entry.word, Modifier.weight(1f).padding(horizontal = 6.dp))
        MasteryMarks(mastery, Modifier.width(MASTERY_COLUMN).padding(horizontal = 6.dp))
    }
}

@Composable
private fun MasteryMarks(mastery: Int, modifier: Modifier) {
    val color = if (mastery <= WEAK_MASTERY) Classic.Error else Classic.Title
    Canvas(modifier.height(14.dp)) {
        val gap = 3.dp.toPx()
        val box = (size.width - gap * (Progress.MAX_MASTERY - 1)) / Progress.MAX_MASTERY
        val square = minOf(box, size.height)
        repeat(Progress.MAX_MASTERY) { index ->
            val topLeft = Offset(index * (square + gap), (size.height - square) / 2)
            if (index < mastery) {
                drawRect(color, topLeft, Size(square, square))
            } else {
                drawRect(Classic.Shadow, topLeft, Size(square, square), style = Stroke(bevelLine()))
            }
        }
    }
}

@Composable
fun SettingsScreen(model: AntonViewModel) {
    BackHandler(onBack = model::closeSettings)
    var confirmingReset by rememberSaveable { mutableStateOf(false) }
    var showingAbout by rememberSaveable { mutableStateOf(false) }
    val settings = model.settings
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().background(Classic.Window)) {
            TitleBar(stringResource(R.string.settings)) {
                TitleBarButton(Glyph.CLOSE, stringResource(R.string.settings_close), onClick = model::closeSettings)
            }
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                GroupBox(stringResource(R.string.settings_group_radio)) {
                    CheckBox(stringResource(R.string.settings_volume_keys), settings.volumeKeysTalk, {
                        model.update(settings.copy(volumeKeysTalk = it))
                    }, Modifier.testTag("setting_volume_keys"))
                    CheckBox(stringResource(R.string.settings_sounds), settings.radioSounds, {
                        model.update(settings.copy(radioSounds = it))
                    })
                }
                TitleFormGroup(settings, model)
                GroupBox(stringResource(R.string.settings_group_data)) {
                    ClassicButton(
                        stringResource(R.string.settings_reset),
                        { confirmingReset = true },
                        Modifier.fillMaxWidth().testTag("reset"),
                        height = 48.dp
                    )
                    ClassicButton(stringResource(R.string.settings_about), {
                        showingAbout = true
                    }, Modifier.fillMaxWidth(), height = 48.dp)
                }
            }
            StatusBar { StatusPane(stringResource(R.string.status_ready), Modifier.weight(1f)) }
        }
        if (confirmingReset) ResetDialog(model) { confirmingReset = false }
        if (showingAbout) AboutDialog { showingAbout = false }
    }
}

@Composable
private fun TitleFormGroup(settings: Settings, model: AntonViewModel) {
    GroupBox(stringResource(R.string.settings_group_title)) {
        RadioButton(stringResource(R.string.settings_title_male), settings.titleForm == Sex.MALE, {
            model.update(settings.copy(titleForm = Sex.MALE))
        })
        RadioButton(stringResource(R.string.settings_title_female), settings.titleForm == Sex.FEMALE, {
            model.update(settings.copy(titleForm = Sex.FEMALE))
        })
    }
}

@Composable
private fun ResetDialog(model: AntonViewModel, onClose: () -> Unit) {
    BackHandler(onBack = onClose)
    ClassicDialog(stringResource(R.string.reset_title)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            WarningIcon()
            Label(stringResource(R.string.reset_body), Modifier.weight(1f))
        }
        DialogButtons(
            stringResource(R.string.reset_confirm),
            {
                model.resetProgress()
                onClose()
            },
            stringResource(R.string.cancel),
            onClose
        )
    }
}

@Composable
private fun AboutDialog(onClose: () -> Unit) {
    BackHandler(onBack = onClose)
    val context = LocalContext.current
    val version = remember(context) {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty()
    }
    ClassicDialog(stringResource(R.string.about_title)) {
        Label(stringResource(R.string.app_name), style = Classic.Strong.copy(fontSize = 22.sp, lineHeight = 26.sp))
        Label(stringResource(R.string.about_version, version))
        Label(stringResource(R.string.about_body))
        Label(stringResource(R.string.about_credits), style = Classic.Small)
        ClassicButton(stringResource(R.string.ok), onClose, Modifier.fillMaxWidth(), primary = true)
    }
}

/** Shown until the microphone is allowed and the speech model has loaded. */
@Composable
fun FirstStartScreen(speech: SpeechState, onAllow: () -> Unit, onOpenSystemSettings: () -> Unit) {
    Box(Modifier.fillMaxSize().background(Classic.Title).statusBarsPadding()) {
        when (speech) {
            SpeechState.NeedsPermission -> ClassicDialog(stringResource(R.string.first_start_title)) {
                Label(stringResource(R.string.first_start_heading), style = Classic.Strong)
                Label(stringResource(R.string.first_start_body))
                ClassicButton(
                    stringResource(R.string.first_start_allow),
                    onAllow,
                    Modifier.fillMaxWidth().testTag("allow"),
                    primary = true
                )
            }

            SpeechState.PermissionDenied -> ClassicDialog(stringResource(R.string.first_start_title)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    WarningIcon()
                    Label(stringResource(R.string.first_start_denied), Modifier.weight(1f))
                }
                ClassicButton(
                    stringResource(R.string.first_start_open_settings),
                    onOpenSystemSettings,
                    Modifier.fillMaxWidth(),
                    primary = true
                )
            }

            is SpeechState.Failed -> ClassicDialog(stringResource(R.string.model_loading_title)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    WarningIcon()
                    Label(stringResource(R.string.model_failed, speech.reason), Modifier.weight(1f))
                }
            }

            SpeechState.Loading, SpeechState.Ready -> ClassicDialog(stringResource(R.string.model_loading_title)) {
                Label(stringResource(R.string.model_loading))
                MarqueeProgress()
            }
        }
    }
}

@Composable
private fun MarqueeProgress() {
    val transition = rememberInfiniteTransition()
    val position by transition.animateFloat(
        0f,
        1f,
        infiniteRepeatable(
            tween(1600, easing = LinearEasing)
        )
    )
    Box(Modifier.fillMaxWidth().height(26.dp).background(Classic.White).sunken().padding(4.dp)) {
        Canvas(Modifier.fillMaxSize()) {
            val blocks = 20
            val gap = 2.dp.toPx()
            val width = (size.width - gap * (blocks - 1)) / blocks
            val head = (position * (blocks + 4)).toInt()
            for (index in head - 4 until head) {
                if (index in 0 until blocks) {
                    drawRect(Classic.Title, Offset(index * (width + gap), 0f), Size(width, size.height))
                }
            }
        }
    }
}
