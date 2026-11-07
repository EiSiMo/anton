package run.moritz.anton.progress

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Entity(tableName = "player")
internal data class PlayerEntity(
    @PrimaryKey val id: Int = SINGLE_PLAYER,
    val rank: String,
    val casesTowardNextRank: Int,
    val streak: Int,
    val bestStreak: Int,
    val casesCompleted: Int
) {
    companion object {
        const val SINGLE_PLAYER = 1
    }
}

@Entity(tableName = "mastery")
internal data class MasteryEntity(@PrimaryKey val symbol: String, val level: Int, val dueAtMillis: Long)

@Entity(tableName = "misrecognition")
internal data class MisrecognitionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val expected: String?,
    val heard: String,
    val recordedAtMillis: Long
)

@Dao
internal abstract class ProgressDao {
    @Query("SELECT * FROM player LIMIT 1")
    abstract suspend fun player(): PlayerEntity?

    @Query("SELECT * FROM mastery")
    abstract suspend fun mastery(): List<MasteryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun putPlayer(player: PlayerEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun putMastery(mastery: List<MasteryEntity>)

    @Insert
    abstract suspend fun addMisrecognition(misrecognition: MisrecognitionEntity)

    @Query("DELETE FROM player")
    abstract suspend fun clearPlayer()

    @Query("DELETE FROM mastery")
    abstract suspend fun clearMastery()

    @Query("DELETE FROM misrecognition")
    abstract suspend fun clearMisrecognitions()

    @Transaction
    open suspend fun save(player: PlayerEntity, mastery: List<MasteryEntity>) {
        putPlayer(player)
        putMastery(mastery)
    }

    @Transaction
    open suspend fun clear() {
        clearPlayer()
        clearMastery()
        clearMisrecognitions()
    }
}

@Database(
    entities = [PlayerEntity::class, MasteryEntity::class, MisrecognitionEntity::class],
    version = 2,
    exportSchema = false
)
internal abstract class ProgressDatabase : RoomDatabase() {
    abstract fun progressDao(): ProgressDao
}

class RoomProgressStore private constructor(private val dao: ProgressDao) : ProgressStore {
    override suspend fun load(): Progress {
        val symbols = dao.mastery()
        val mastery = symbols.associate { it.symbol to it.level }
        val dueAt = symbols.associate { it.symbol to it.dueAtMillis }
        val player = dao.player() ?: return Progress(mastery = mastery, dueAt = dueAt)
        return Progress(
            rank = Rank.valueOf(player.rank),
            casesTowardNextRank = player.casesTowardNextRank,
            streak = player.streak,
            bestStreak = player.bestStreak,
            casesCompleted = player.casesCompleted,
            mastery = mastery,
            dueAt = dueAt
        )
    }

    override suspend fun save(progress: Progress) {
        val player = PlayerEntity(
            rank = progress.rank.name,
            casesTowardNextRank = progress.casesTowardNextRank,
            streak = progress.streak,
            bestStreak = progress.bestStreak,
            casesCompleted = progress.casesCompleted
        )
        val mastery = progress.mastery.map { (symbol, level) ->
            MasteryEntity(symbol, level, progress.dueAt[symbol] ?: 0)
        }
        dao.save(player, mastery)
    }

    override suspend fun recordMisrecognition(expected: String?, heard: String) {
        dao.addMisrecognition(MisrecognitionEntity(expected = expected, heard = heard, recordedAtMillis = now()))
    }

    override suspend fun reset() = dao.clear()

    private fun now() = System.currentTimeMillis()

    companion object {
        const val DATABASE_NAME = "progress"

        /** Adds review dates, so everything practised so far is due, and renames the first rank. */
        private val ADD_REVIEW_DATES = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE mastery ADD COLUMN dueAtMillis INTEGER NOT NULL DEFAULT 0")
                db.execSQL("UPDATE player SET rank = 'POLICE_MASTER_CANDIDATE' WHERE rank = 'IN_TRAINING'")
            }
        }

        fun create(context: Context, name: String = DATABASE_NAME): RoomProgressStore {
            val database = Room.databaseBuilder(context, ProgressDatabase::class.java, name)
                .addMigrations(ADD_REVIEW_DATES)
                .build()
            return RoomProgressStore(database.progressDao())
        }
    }
}
