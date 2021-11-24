package re.notifica.geo.models

import android.location.Location
import android.os.Build
import android.os.Parcelable
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonClass
import kotlinx.parcelize.Parcelize
import org.json.JSONObject
import re.notifica.Notificare
import re.notifica.internal.moshi
import java.util.*

@Parcelize
@JsonClass(generateAdapter = true)
public data class NotificareLocation(
    val latitude: Double,
    val longitude: Double,
    val altitude: Double,
    val course: Double,
    val speed: Double,
    // val floor: Int?,
    val horizontalAccuracy: Double,
    val verticalAccuracy: Double,
    val timestamp: Date,
) : Parcelable {

    public companion object {
        public operator fun invoke(location: Location): NotificareLocation {
            val verticalAccuracy: Double =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) location.verticalAccuracyMeters.toDouble()
                else 0.0

            return NotificareLocation(
                latitude = location.latitude,
                longitude = location.longitude,
                altitude = location.altitude,
                course = location.bearing.toDouble(),
                speed = location.speed.toDouble(),
                horizontalAccuracy = location.accuracy.toDouble(),
                verticalAccuracy = verticalAccuracy,
                timestamp = Date(location.time),
            )
        }
    }
}

// region JSON: NotificareLocation

public val NotificareLocation.Companion.adapter: JsonAdapter<NotificareLocation>
    get() = Notificare.moshi.adapter(NotificareLocation::class.java)

public fun NotificareLocation.Companion.fromJson(json: JSONObject): NotificareLocation {
    val jsonStr = json.toString()
    return requireNotNull(adapter.fromJson(jsonStr))
}

public fun NotificareLocation.toJson(): JSONObject {
    val jsonStr = NotificareLocation.adapter.toJson(this)
    return JSONObject(jsonStr)
}

// endregion
