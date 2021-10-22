package re.notifica.models

import com.squareup.moshi.Json

public enum class NotificareTransport {
    @Json(name = "Notificare")
    NOTIFICARE,

    @Json(name = "GCM")
    GCM,

    @Json(name = "HMS")
    HMS
}
