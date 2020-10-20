package re.notifica.internal.network.push.responses

import com.squareup.moshi.JsonClass
import re.notifica.models.NotificareApplication
import re.notifica.models.NotificareDoNotDisturb

@JsonClass(generateAdapter = true)
internal data class NotificareDeviceDoNotDisturbResponse(
    val dnd: NotificareDoNotDisturb?,
)
