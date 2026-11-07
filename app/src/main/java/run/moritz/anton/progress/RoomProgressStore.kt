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
internal data class MasteryEntity(@PrimaryKey val symbol: String, val level: Int)

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
    version = 1,
    exportSchema = false
)
internal abstract class ProgressDatabase : RoomDatabase() {
    abstract fun progressDao(): ProgressDao
}

class RoomProgressStore private constructor(private val dao: ProgressDao) : ProgressStore {
    override suspend fun load(): Progress {
        val mastery = dao.mastery().associate { it.symbol to it.level }
        val player = dao.player() ?: return Progress(mastery = mastery)
        return Progress(
            rank = Rank.valueOf(player.rank),
            casesTowardNextRank = player.casesTowardNextRank,
            streak = player.streak,
            bestStreak = player.bestStreak,
            casesCompleted = player.casesCompleted,
            mastery = mastery
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
        dao.save(player, progress.mastery.map { (symbol, level) -> MasteryEntity(symbol, level) })
    }

    override suspend fun recordMisrecognition(expected: String?, heard: String) {
        dao.addMisrecognition(MisrecognitionEntity(expected = expected, heard = heard, recordedAtMillis = now()))
    }

    override suspend fun reset() = dao.clear()

    private fun now() = System.currentTimeMillis()

    companion object {
        fun create(context: Context): RoomProgressStore {
            val database = Room.databaseBuilder(context, ProgressDatabase::class.java, "progress").build()
            return RoomProgressStore(database.progressDao())
        }
    }
}
