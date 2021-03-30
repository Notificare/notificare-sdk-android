package re.notifica.inbox.internal.database.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import re.notifica.inbox.internal.database.entities.InboxItemEntity

@Dao
internal interface InboxDao {

    @Query("SELECT * FROM inbox")
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
