package re.notifica.loyalty.internal.network.push

import com.squareup.moshi.JsonClass
import re.notifica.models.NotificareTransport

@JsonClass(generateAdapter = true)
internal data class RegisterPassPayload(
    val transport: NotificareTransport,
)
