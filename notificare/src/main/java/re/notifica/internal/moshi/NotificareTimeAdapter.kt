package re.notifica.internal.moshi

import com.squareup.moshi.FromJson
import com.squareup.moshi.ToJson
import re.notifica.models.NotificareTime

internal class NotificareTimeAdapter {

    @ToJson
    fun toJson(time: NotificareTime): String = time.format()

    @FromJson
    fun fromJson(time: String): NotificareTime = NotificareTime(time)
}
