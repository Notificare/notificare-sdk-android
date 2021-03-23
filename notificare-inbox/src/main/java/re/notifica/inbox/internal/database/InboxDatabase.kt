package re.notifica.inbox.internal.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import re.notifica.inbox.internal.database.converters.DateConverter
import re.notifica.inbox.internal.database.converters.NotificationConverter
import re.notifica.inbox.internal.database.dao.InboxDao
import re.notifica.inbox.internal.database.entities.InboxItemEntity

@Database(
    version = 1,
    entities = [
        InboxItemEntity::class,
    ],
)
@TypeConverters(
    DateConverter::class,
    NotificationConverter::class,
)
internal abstract class InboxDatabase : RoomDatabase() {

    abstract fun inbox(): InboxDao

    companion object {
        private const val DB_NAME = "notificare_inbox.db"

        internal fun create(context: Context): InboxDatabase {
            return Room.databaseBuilder(
                context,
                InboxDatabase::class.java,
                DB_NAME
            ).fallbackToDestructiveMigration().build()
        }
    }
}
