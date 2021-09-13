package re.notifica.internal.storage.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "events")
internal data class NotificareEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val type: String,
    val timestamp: Long,
    @ColumnInfo(name = "device_id") val deviceId: String?,
    @ColumnInfo(name = "session_id") val sessionId: String?,
    @ColumnInfo(name = "notification_id") val notificationId: String?,
    @ColumnInfo(name = "user_id") val userId: String?,
    val data: String?,
    val ttl: Int,
    var retries: Int,
)
