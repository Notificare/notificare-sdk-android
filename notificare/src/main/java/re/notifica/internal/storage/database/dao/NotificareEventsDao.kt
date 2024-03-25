package re.notifica.internal.storage.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
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
