package re.notifica.internal.room

import androidx.room.TypeConverter
import re.notifica.InternalNotificareApi
import re.notifica.Notificare
import re.notifica.internal.moshi
import re.notifica.models.NotificareNotification

@InternalNotificareApi
public class NotificareNotificationConverter {

    @TypeConverter
    public fun fromJson(str: String?): NotificareNotification? {
        if (str == null) return null

        val adapter = Notificare.moshi.adapter(NotificareNotification::class.java)
        return adapter.fromJson(str)
    }

    @TypeConverter
    public fun toJson(notification: NotificareNotification?): String? {
        if (notification == null) return null

        val adapter = Notificare.moshi.adapter(NotificareNotification::class.java)
        return adapter.toJson(notification)
    }
}
