package re.notifica.internal.storage.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import re.notifica.internal.storage.database.dao.NotificareEventsDao
import re.notifica.internal.storage.database.entities.NotificareEventEntity

@Database(
    version = 2,
    entities = [
        NotificareEventEntity::class
    ],
)
internal abstract class NotificareDatabase : RoomDatabase() {

    abstract fun events(): NotificareEventsDao

    companion object {
        private const val DB_NAME = "notificare.db"

        internal fun create(context: Context): NotificareDatabase {
            return Room.databaseBuilder(
                context,
                NotificareDatabase::class.java,
                DB_NAME
            ).fallbackToDestructiveMigration().build()
        }
    }
}
