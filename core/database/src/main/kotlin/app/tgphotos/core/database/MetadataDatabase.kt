package app.tgphotos.core.database

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase

/** Opaque IDs plus authenticated encrypted metadata only; never raw paths, names, EXIF or keys. */
@Entity(tableName = "encrypted_records")
data class EncryptedRecord(
    @PrimaryKey val opaqueId: String,
    val schemaVersion: Int,
    val metadataEnvelope: ByteArray,
)

@Dao
interface EncryptedRecordDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(record: EncryptedRecord)
    @Query("SELECT * FROM encrypted_records WHERE opaqueId = :id")
    suspend fun find(id: String): EncryptedRecord?
}

// Schema foundation only. Instantiate only after encrypted storage integration and migration review.
@Database(entities = [EncryptedRecord::class], version = 1, exportSchema = true)
abstract class MetadataDatabase : RoomDatabase() {
    abstract fun records(): EncryptedRecordDao
}
