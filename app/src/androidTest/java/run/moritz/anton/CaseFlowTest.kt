package run.moritz.anton

import android.Manifest
import android.graphics.Bitmap
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import run.moritz.anton.cases.Case
import run.moritz.anton.progress.Progress
import run.moritz.anton.progress.Rank
import run.moritz.anton.spelling.SpellingTable
import run.moritz.anton.ui.AntonViewModel

/** Plays through the real screens with scripted speech and files a screenshot of each state. */
@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class CaseFlowTest {
    @get:Rule
    val compose = createEmptyComposeRule()

    private val recognizer = ScriptedRecognizer()
    private lateinit var store: MemoryProgressStore
    private val instrumentation = InstrumentationRegistry.getInstrumentation()

    @Before
    fun grantMicrophone() {
        instrumentation.uiAutomation.grantRuntimePermission(
            instrumentation.targetContext.packageName,
            Manifest.permission.RECORD_AUDIO
        )
    }

    private fun launch(progress: Progress = Progress()) {
        store = MemoryProgressStore(progress)
        installScriptedApp(recognizer, store)
        ActivityScenario.launch(MainActivity::class.java)
        compose.waitUntilAtLeastOneExists(hasTestTag("next_case"), 10_000)
    }

    private fun tag(name: String): SemanticsNodeInteraction = compose.onNodeWithTag(name, useUnmergedTree = true)

    private fun transmit(heard: String) {
        recognizer.nextHeard = heard
        tag("talk").performTouchInput { down(center) }
        compose.waitForIdle()
        tag("talk").performTouchInput { up() }
        compose.waitForIdle()
    }

    private fun screenshot(name: String) {
        compose.waitForIdle()
        Thread.sleep(250)
        val bitmap = instrumentation.uiAutomation.takeScreenshot()
        val directory = File(instrumentation.targetContext.getExternalFilesDir(null), "screens").apply { mkdirs() }
        File(directory, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    /** Sends each open field correctly until the case is finished. */
    private fun finishCase(fields: () -> List<String>) = fields().forEach { transmit(spoken(it)) }

    private fun currentFields(): List<String> {
        compose.waitForIdle()
        return model().caseUi!!.case.fields.map { it.spoken }
    }

    private fun model(): AntonViewModel {
        lateinit var model: AntonViewModel
        instrumentation.runOnMainSync {
            val activity = ActivityLifecycleMonitorRegistry.getInstance()
                .getActivitiesInStage(Stage.RESUMED).first() as MainActivity
            model = activity.testModel
        }
        return model
    }

    @Test
    fun aPerfectCaseFillsTheBarAndExtendsTheStreak() {
        launch()
        screenshot("01_station_new")
        tag("next_case").performClick()
        screenshot("02_case_open")
        finishCase(::currentFields)
        tag("result").assertIsDisplayed()
        screenshot("03_case_result")
        assertEquals(1, store.progress.streak)
        assertEquals(1, store.progress.casesTowardNextRank)
    }

    @Test
    fun aWrongWordOpensThePlausibilityDialogAndCostsTheStreak() {
        launch(Progress(streak = 4, bestStreak = 4))
        tag("next_case").performClick()
        val field = currentFields().first()
        val wrong = if (field.first().equals('K', true)) "otto" else "kaufmann"
        transmit(wrong)
        tag("plausibility").assertIsDisplayed()
        screenshot("04_plausibility")
        assertEquals(4, store.progress.streak)
        compose.onNodeWithText("Wiederholen").performClick()
        compose.waitForIdle()
        assertEquals(0, store.progress.streak)
    }

    @Test
    fun dasWarRichtigKeepsTheStreakAndRecordsTheMisrecognition() {
        launch(Progress(streak = 4, bestStreak = 4))
        tag("next_case").performClick()
        val fields = currentFields()
        transmit("kaufmann otto zwei")
        compose.onNodeWithText("Das war richtig").performClick()
        compose.waitForIdle()
        assertEquals(1, store.misrecognitions.size)
        fields.drop(1).forEach { transmit(spoken(it)) }
        assertEquals(5, store.progress.streak)
    }

    @Test
    fun nothingHeardAsksToRepeatWithoutAnyPenalty() {
        launch(Progress(streak = 4, bestStreak = 4))
        tag("next_case").performClick()
        transmit("")
        compose.onNodeWithText("Nicht verstanden. Bitte wiederholen.").assertIsDisplayed()
        screenshot("05_not_understood")
        assertEquals(4, store.progress.streak)
    }

    @Test
    fun openingTheTableDuringACaseWarnsAndCostsTheStreak() {
        launch(Progress(streak = 4, bestStreak = 4))
        tag("next_case").performClick()
        tag("case_table").performClick()
        screenshot("06_table_warning")
        compose.onNodeWithText("Tafel öffnen").performClick()
        compose.waitForIdle()
        assertEquals(0, store.progress.streak)
        screenshot("07_table_in_case")
    }

    @Test
    fun reachingARankShowsTheCertificate() {
        launch(Progress(casesTowardNextRank = Rank.IN_TRAINING.casesToNext!! - 1, mastery = demoMastery()))
        screenshot("08_station_progressed")
        tag("next_case").performClick()
        finishCase(::currentFields)
        tag("certificate").assertIsDisplayed()
        screenshot("09_certificate")
        assertEquals(Rank.POLICE_MASTER, store.progress.rank)
    }

    @Test
    fun theTableStartsADrillThatLeavesTheStreakAlone() {
        launch(Progress(streak = 4, bestStreak = 4, mastery = demoMastery()))
        tag("open_table").performClick()
        screenshot("10_table")
        tag("row_A").performClick()
        screenshot("11_drill")
        finishCase(::currentFields)
        tag("drill_result").assertIsDisplayed()
        screenshot("12_drill_result")
        assertEquals(4, store.progress.streak)
    }

    @Test
    fun casesOfEveryKindRender() {
        launch(
            Progress(
                rank = Rank.CHIEF_INSPECTOR,
                casesTowardNextRank = 12,
                streak = 17,
                bestStreak = 31,
                casesCompleted = 88,
                mastery = demoMastery()
            )
        )
        screenshot("13_station_senior")
        tag("next_case").performClick()
        val model = model()
        var plates = 0
        var cards = 0
        var longNames = 0
        repeat(400) {
            when (val case = model.caseUi!!.case) {
                is Case.Plate -> if (plates < 3) screenshot("14_plate_${plates++}")

                is Case.IdCard -> {
                    if (cards < 6) screenshot("15_card_${cards++}")
                    if (case.person.surname.length > 18 && longNames < 2) screenshot("16_long_name_${longNames++}")
                }

                is Case.Drill -> Unit
            }
            instrumentation.runOnMainSync { model.startNextCase() }
            compose.waitForIdle()
        }
    }

    @Test
    fun settingsAndEveryRankRender() {
        launch(Progress(rank = Rank.PRESIDENT, mastery = demoMastery()))
        screenshot("17_station_top")
        tag("settings_open").performClick()
        screenshot("18_settings")
        tag("reset").performClick()
        screenshot("19_reset_dialog")
        compose.onNodeWithText("Zurücksetzen").performClick()
        compose.waitForIdle()
        assertEquals(Rank.IN_TRAINING, store.progress.rank)
    }

    private fun demoMastery(): Map<String, Int> {
        val entries = SpellingTable.classicGerman.entries
        return entries.mapIndexed { index, entry -> entry.symbol to (index * 7 % 6) }.toMap()
    }
}
