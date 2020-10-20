package re.notifica.internal.network.push.responses

import com.squareup.moshi.JsonClass
import re.notifica.models.NotificareApplication
import re.notifica.models.NotificareDoNotDisturb
import re.notifica.models.NotificareUserData

@JsonClass(generateAdapter = true)
internal data class NotificareDeviceUserDataResponse(
    val userData: NotificareUserData?,
)
