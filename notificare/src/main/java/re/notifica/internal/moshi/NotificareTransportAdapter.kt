package re.notifica.internal.moshi

import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.ToJson
import re.notifica.models.NotificareTransport

internal class NotificareTransportAdapter {

    @ToJson
    fun toJson(transport: NotificareTransport): String {
        return when (transport) {
            NotificareTransport.GCM -> "GCM"
            NotificareTransport.HMS -> "HMS"
            NotificareTransport.NOTIFICARE -> "Notificare"
        }
    }

    @FromJson
    fun fromJson(transport: String): NotificareTransport {
        return when (transport) {
            "GCM" -> NotificareTransport.GCM
            "HMS" -> NotificareTransport.HMS
            "Notificare" -> NotificareTransport.NOTIFICARE
            else -> throw JsonDataException("unknown transport: $transport")
        }
    }
}
