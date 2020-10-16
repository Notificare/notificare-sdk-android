package re.notifica.internal.network.push.responses

import com.squareup.moshi.JsonClass
import re.notifica.models.NotificareApplication

@JsonClass(generateAdapter = true)
internal data class NotificareApplicationResponse(
    val application: NotificareApplication
)
