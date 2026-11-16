package app.keyhole.privacy.data.db

import android.content.Context
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

/*
 * Everything Keyhole keeps (CLAUDE.md › Behaviour rules): reports, what they found, their photos, and the devices
 * marked as known. On this phone only; no backup (manifest), deleted with the app. Schemas in app/schemas.
 */

/** One report: a visit to a place, saved when its sweep ended. Lists are stored as comma-joined enum names. */
@Entity(tableName = "reports")
data class ReportEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** The user's name for it ("Sea View Hotel, room 304"); empty until they give one. */
    val name: String = "",
    val place: String,
    @ColumnInfo(name = "started_at") val startedAt: Long,
    @ColumnInfo(name = "saved_at") val savedAt: Long,
    /** The checks the place had, in order (the report's check row). */
    val planned: String,
    /** The checks that ran. */
    val ran: String,
    @ColumnInfo(name = "wifi_name") val wifiName: String? = null,
    /** "Couldn't check" lines as codes (`ReportCodes`). */
    val gaps: String,
)

/** One finding of a report. See `domain.Finding`. */
@Entity(
    tableName = "findings",
    foreignKeys = [ForeignKey(entity = ReportEntity::class, parentColumns = ["id"], childColumns = ["report_id"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("report_id")],
)
data class FindingEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "report_id") val reportId: Long,
    val source: String,
    val title: String,
    val detail: String,
    val risk: String,
    /** Sign codes, joined with a line break (a maker's name may hold a comma). */
    val signs: String,
    val label: String? = null,
    @ColumnInfo(name = "mark_key") val markKey: String? = null,
    @ColumnInfo(name = "known_as") val knownAs: String? = null,
    val note: String = "",
    val photos: Int = 0,
    /** Order in the report. */
    val position: Int,
)

/** A proof photo of a report: the file is in app storage and is deleted with the report. */
@Entity(
    tableName = "photos",
    foreignKeys = [ForeignKey(entity = ReportEntity::class, parentColumns = ["id"], childColumns = ["report_id"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("report_id")],
)
data class PhotoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "report_id") val reportId: Long,
    val path: String,
    val position: Int,
)

/** A device the user said isn't a camera ("Not a camera?"). Kept across reports; More › Marked as known lists them. */
@Entity(tableName = "marks")
data class MarkEntity(
    @PrimaryKey val key: String,
    /** "MINE" or "ROOM". */
    @ColumnInfo(name = "known_as") val knownAs: String,
    /** What it is called, for the list in More. */
    val label: String,
    /** WIFI, HOTSPOTS or BLUETOOTH. */
    val check: String,
    val at: Long,
)

@Dao
interface ReportDao {
    @Query("SELECT * FROM reports ORDER BY saved_at DESC")
    fun reports(): Flow<List<ReportEntity>>

    @Query("SELECT * FROM reports WHERE id = :id")
    fun report(id: Long): Flow<ReportEntity?>

    @Query("SELECT * FROM findings WHERE report_id = :id ORDER BY position")
    fun findings(id: Long): Flow<List<FindingEntity>>

    @Query("SELECT * FROM photos WHERE report_id = :id ORDER BY position")
    fun photos(id: Long): Flow<List<PhotoEntity>>

    /** Per report, how many findings are still to look at (not marked as known). */
    @Query("SELECT report_id AS reportId, COUNT(*) AS count FROM findings WHERE known_as IS NULL GROUP BY report_id")
    fun toLookAt(): Flow<List<ReportCount>>

    @Insert
    suspend fun insert(report: ReportEntity): Long

    @Insert
    suspend fun insertFindings(findings: List<FindingEntity>)

    @Insert
    suspend fun insertPhotos(photos: List<PhotoEntity>)

    @Query("UPDATE reports SET name = :name WHERE id = :id")
    suspend fun rename(id: Long, name: String)

    @Query("SELECT * FROM findings WHERE id = :id")
    suspend fun finding(id: Long): FindingEntity?

    @Query("DELETE FROM findings WHERE id = :id")
    suspend fun deleteFinding(id: Long)

    @Insert
    suspend fun insertFinding(finding: FindingEntity)

    @Query("UPDATE findings SET known_as = :knownAs WHERE id = :id")
    suspend fun setKnownAs(id: Long, knownAs: String?)

    @Query("SELECT path FROM photos WHERE report_id = :id")
    suspend fun photoPaths(id: Long): List<String>

    @Query("SELECT path FROM photos")
    suspend fun allPhotoPaths(): List<String>

    @Query("DELETE FROM reports WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM reports")
    suspend fun deleteAll()
}

data class ReportCount(val reportId: Long, val count: Int)

@Dao
interface MarkDao {
    @Query("SELECT * FROM marks ORDER BY at DESC")
    fun marks(): Flow<List<MarkEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun put(mark: MarkEntity)

    @Query("DELETE FROM marks WHERE `key` = :key")
    suspend fun remove(key: String)

    @Query("DELETE FROM marks")
    suspend fun clear()
}

@Database(entities = [ReportEntity::class, FindingEntity::class, PhotoEntity::class, MarkEntity::class], version = 1, exportSchema = true)
abstract class KeyholeDatabase : RoomDatabase() {
    abstract fun reports(): ReportDao
    abstract fun marks(): MarkDao

    companion object {
        fun open(context: Context): KeyholeDatabase =
            Room.databaseBuilder(context.applicationContext, KeyholeDatabase::class.java, "keyhole.db").build()
    }
}
