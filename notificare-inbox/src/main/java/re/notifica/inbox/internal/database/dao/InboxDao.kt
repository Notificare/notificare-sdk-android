package re.notifica.inbox.internal.database.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import re.notifica.inbox.internal.database.entities.InboxItemEntity

@Dao
internal interface InboxDao {

    @Query(
        "SELECT * FROM inbox WHERE (visible IS NULL OR visible == 1) AND (expires IS NULL OR expires > strftime('%s', 'now') || substr(strftime('%f', 'now'), 4))"
    )
    fun getLiveItems(): LiveData<List<InboxItemEntity>>

    @Query("SELECT * FROM inbox WHERE id = :id LIMIT 1")
    suspend fun findById(id: String): InboxItemEntity?

    @Query("SELECT * FROM inbox ORDER BY time DESC LIMIT 1")
    suspend fun findMostRecent(): InboxItemEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: InboxItemEntity)

    @Update
    suspend fun update(item: InboxItemEntity)

    @Query("UPDATE inbox SET opened = 1")
    suspend fun updateAllAsRead()

    @Query("DELETE FROM inbox WHERE id = :id")
    suspend fun remove(id: String)

    @Query("DELETE FROM inbox")
    suspend fun clear()
}
