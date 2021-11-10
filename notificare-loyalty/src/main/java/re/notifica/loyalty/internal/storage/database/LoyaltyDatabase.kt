package re.notifica.loyalty.internal.storage.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import re.notifica.internal.room.NotificareDateTypeConverter
import re.notifica.loyalty.internal.storage.database.dao.PassesDao
import re.notifica.loyalty.internal.storage.database.entities.PassEntity

@Database(
    version = 1,
    entities = [
        PassEntity::class,
    ],
)
@TypeConverters(
    NotificareDateTypeConverter::class,
)
internal abstract class LoyaltyDatabase : RoomDatabase() {

    abstract fun passes(): PassesDao

    companion object {
        private const val DB_NAME = "notificare_loyalty.db"

        internal fun create(context: Context): LoyaltyDatabase {
            return Room.databaseBuilder(context, LoyaltyDatabase::class.java, DB_NAME)
                .fallbackToDestructiveMigration()
                .build()
        }
    }
}
