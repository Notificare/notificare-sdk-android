package re.notifica.geo.models

import android.os.Parcelable
import com.squareup.moshi.Json
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonClass
import kotlinx.parcelize.Parcelize
import org.json.JSONObject
import re.notifica.Notificare
import re.notifica.internal.moshi

@Parcelize
@JsonClass(generateAdapter = true)
public data class NotificareBeacon(
    val id: String,
    val name: String,
    val major: Int,
    val minor: Int?,
    val triggers: Boolean = false,
    var proximity: Proximity = Proximity.UNKNOWN,
) : Parcelable {
    public companion object {
        internal const val PROXIMITY_NEAR_DISTANCE = 0.2
        internal const val PROXIMITY_FAR_DISTANCE = 2.0
    }

    @Parcelize
    public enum class Proximity : Parcelable {
        @Json(name = "unknown")
        UNKNOWN,

        @Json(name = "immediate")
        IMMEDIATE,

        @Json(name = "near")
        NEAR,

        @Json(name = "far")
        FAR;
    }
}

// region JSON: NotificareBeacon

public val NotificareBeacon.Companion.adapter: JsonAdapter<NotificareBeacon>
    get() = Notificare.moshi.adapter(NotificareBeacon::class.java)

public fun NotificareBeacon.Companion.fromJson(json: JSONObject): NotificareBeacon {
    val jsonStr = json.toString()
    return requireNotNull(adapter.fromJson(jsonStr))
}

public fun NotificareBeacon.toJson(): JSONObject {
    val jsonStr = NotificareBeacon.adapter.toJson(this)
    return JSONObject(jsonStr)
}

// endregion
