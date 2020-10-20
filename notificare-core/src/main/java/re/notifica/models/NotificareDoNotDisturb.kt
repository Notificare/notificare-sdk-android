package re.notifica.models

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class NotificareDoNotDisturb(
    val start: NotificareTime,
    val end: NotificareTime
)
