package re.notifica.inbox.internal.database.converters

import androidx.room.TypeConverter
import java.util.*

internal class DateConverter {

    @TypeConverter
    fun fromTimestamp(timestamp: Long?): Date? {
        return timestamp?.let { Date(it) }
    }

    @TypeConverter
    fun toTimestamp(date: Date?): Long? {
        return date?.time
    }
}
