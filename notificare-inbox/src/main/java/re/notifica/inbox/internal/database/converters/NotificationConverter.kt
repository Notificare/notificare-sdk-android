package re.notifica.inbox.internal.database.converters

import androidx.room.TypeConverter
import re.notifica.Notificare
import re.notifica.models.NotificareNotification

internal class NotificationConverter {

    @TypeConverter
    fun fromJson(str: String?): NotificareNotification? {
        if (str == null) return null

        val adapter = Notificare.moshi.adapter(NotificareNotification::class.java)
        return adapter.fromJson(str)
    }

    @TypeConverter
    fun toJson(notification: NotificareNotification?): String? {
        if (notification == null) return null

        val adapter = Notificare.moshi.adapter(NotificareNotification::class.java)
        return adapter.toJson(notification)
    }
}
