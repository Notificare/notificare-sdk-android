package re.notifica.monetize.internal.database.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import re.notifica.monetize.internal.database.entities.PurchaseEntity

@Dao
internal interface PurchasesDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(purchase: PurchaseEntity)

    @Query("SELECT * FROM purchases WHERE order_id = :orderId LIMIT 1")
    suspend fun getPurchaseByOrderId(orderId: String): PurchaseEntity?

    @Query("SELECT * FROM purchases")
    fun getObservablePurchases(): LiveData<List<PurchaseEntity>>
}
