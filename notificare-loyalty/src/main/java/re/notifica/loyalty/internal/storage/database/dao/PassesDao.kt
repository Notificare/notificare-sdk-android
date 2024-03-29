package re.notifica.loyalty.internal.storage.database.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import re.notifica.loyalty.internal.storage.database.entities.PassEntity

@Dao
internal interface PassesDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(pass: PassEntity)

    @Update
    suspend fun update(pass: PassEntity)

    @Delete
    suspend fun remove(pass: PassEntity)

    @Query("DELETE FROM passes")
    suspend fun clear()

    @Query("SELECT * FROM passes")
    suspend fun getPasses(): List<PassEntity>

    @Query("SELECT * FROM passes")
    fun getObservablePasses(): LiveData<List<PassEntity>>
}
