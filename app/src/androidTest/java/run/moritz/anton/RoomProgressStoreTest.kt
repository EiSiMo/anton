package run.moritz.anton

import android.database.sqlite.SQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import run.moritz.anton.progress.Progress
import run.moritz.anton.progress.ProgressEvent
import run.moritz.anton.progress.Rank
import run.moritz.anton.progress.RoomProgressStore

@RunWith(AndroidJUnit4::class)
class RoomProgressStoreTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun progressSurvivesASaveAndLoad(): Unit = runBlocking {
        val name = "roundtrip-test"
        context.deleteDatabase(name)
        val store = RoomProgressStore.create(context, name)
        val progress = Progress(
            rank = Rank.POLICE_MASTER,
            streak = 3
        ).after(ProgressEvent.Confirmed(listOf("Q")), 1_000)
        store.save(progress)
        assertEquals(progress, store.load())
        store.reset()
        assertEquals(Progress(), store.load())
    }

    @Test
    fun aVersionOneDatabaseMigratesAndEverythingPractisedIsDue(): Unit = runBlocking {
        val name = "migration-test"
        context.deleteDatabase(name)
        val file = context.getDatabasePath(name).apply { parentFile?.mkdirs() }
        SQLiteDatabase.openOrCreateDatabase(file, null).use { db ->
            db.execSQL(
                "CREATE TABLE player (id INTEGER NOT NULL PRIMARY KEY, rank TEXT NOT NULL, " +
                    "casesTowardNextRank INTEGER NOT NULL, streak INTEGER NOT NULL, bestStreak INTEGER NOT NULL, " +
                    "casesCompleted INTEGER NOT NULL)"
            )
            db.execSQL("CREATE TABLE mastery (symbol TEXT NOT NULL PRIMARY KEY, level INTEGER NOT NULL)")
            db.execSQL(
                "CREATE TABLE misrecognition (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, expected TEXT, " +
                    "heard TEXT NOT NULL, recordedAtMillis INTEGER NOT NULL)"
            )
            db.execSQL("INSERT INTO player VALUES (1, 'IN_TRAINING', 2, 2, 2, 2)")
            db.execSQL("INSERT INTO mastery VALUES ('Q', 3)")
            db.version = 1
        }
        val loaded = RoomProgressStore.create(context, name).load()
        assertEquals(Rank.POLICE_MASTER_CANDIDATE, loaded.rank)
        assertEquals(listOf("Q"), loaded.dueSymbols(0))
        assertEquals(3, loaded.masteryOf("Q"))
    }
}
