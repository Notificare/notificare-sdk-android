package re.notifica.monetize.internal.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import re.notifica.internal.room.NotificareDateTypeConverter
import re.notifica.monetize.internal.database.dao.PurchasesDao
import re.notifica.monetize.internal.database.entities.PurchaseEntity

@Database(
    version = 1,
    entities = [
        PurchaseEntity::class,
    ],
)
@TypeConverters(
    NotificareDateTypeConverter::class,
)
internal abstract class MonetizeDatabase : RoomDatabase() {

    abstract fun purchases(): PurchasesDao

    companion object {
        private const val DB_NAME = "notificare_monetize.db"

        internal fun create(context: Context): MonetizeDatabase {
            return Room.databaseBuilder(context, MonetizeDatabase::class.java, DB_NAME)
                .fallbackToDestructiveMigration()
                .build()
        }
    }
}
