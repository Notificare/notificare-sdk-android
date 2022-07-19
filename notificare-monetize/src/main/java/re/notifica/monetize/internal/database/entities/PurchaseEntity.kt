package re.notifica.monetize.internal.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.*

@Entity(
    tableName = "purchases"
)
internal data class PurchaseEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "product_identifier") val productIdentifier: String,
    @ColumnInfo(name = "time") val time: Date,
    @ColumnInfo(name = "original_json") val originalJson: String,
    @ColumnInfo(name = "purchase_json") val purchaseJson: String,
)
