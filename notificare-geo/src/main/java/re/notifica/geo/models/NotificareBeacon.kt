package re.notifica.geo.models

import android.os.Parcelable
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.parcelize.Parcelize

@Parcelize
@JsonClass(generateAdapter = true)
public data class NotificareBeacon(
    val id: String,
    val name: String,
    val major: Int,
    val minor: Int?,
    val triggers: Boolean,
    var proximity: Proximity?,
) : Parcelable {
    public companion object {
        internal const val PROXIMITY_NEAR_DISTANCE = 0.2
        internal const val PROXIMITY_FAR_DISTANCE = 2.0
    }

    @Parcelize
    public enum class Proximity : Parcelable {
        @Json(name = "immediate")
        IMMEDIATE,

        @Json(name = "near")
        NEAR,

        @Json(name = "far")
        FAR;
    }
}
