package re.notifica.internal.room

import androidx.room.TypeConverter
import re.notifica.InternalNotificareApi
import java.util.*

@InternalNotificareApi
public class NotificareDateTypeConverter {

    @TypeConverter
    public fun fromTimestamp(timestamp: Long?): Date? {
        return timestamp?.let { Date(it) }
    }

    @TypeConverter
    public fun toTimestamp(date: Date?): Long? {
        return date?.time
    }
}
