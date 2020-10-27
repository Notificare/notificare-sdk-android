package re.notifica.internal.storage.database.dao

import androidx.room.*
import re.notifica.internal.storage.database.entities.NotificareEventEntity

@Dao
internal interface NotificareEventsDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(event: NotificareEventEntity)

    @Update
    suspend fun update(event: NotificareEventEntity)

    @Query("SELECT * FROM events")
    suspend fun find(): List<NotificareEventEntity>

    @Delete
    suspend fun delete(event: NotificareEventEntity)
}
