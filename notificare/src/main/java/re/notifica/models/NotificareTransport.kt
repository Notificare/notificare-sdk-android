package re.notifica.models

import android.os.Parcelable
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.parcelize.Parcelize

@Parcelize
@JsonClass(generateAdapter = false)
public enum class NotificareTransport : Parcelable {
    @Json(name = "Notificare")
    NOTIFICARE,

    @Json(name = "GCM")
    GCM,

    @Json(name = "HMS")
    HMS
}
