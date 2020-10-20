package re.notifica.internal.network.push.responses

import com.squareup.moshi.JsonClass
import re.notifica.models.NotificareApplication

@JsonClass(generateAdapter = true)
internal data class NotificareDeviceTagsResponse(
    val tags: List<String>
)
