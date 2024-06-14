package re.notifica.push.models

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
    GCM;

    public val rawValue: String
        get() = when (this) {
            NOTIFICARE -> "Notificare"
            GCM -> "GCM"
        }
}
