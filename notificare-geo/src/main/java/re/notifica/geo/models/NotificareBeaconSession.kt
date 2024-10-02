package re.notifica.geo.models

import android.os.Parcelable
import com.squareup.moshi.JsonClass
import java.util.Date
import kotlinx.parcelize.Parcelize

@Parcelize
@JsonClass(generateAdapter = true)
public data class NotificareBeaconSession(
    val regionId: String,
    val start: Date,
    val end: Date?,
    val beacons: MutableList<Beacon>,
) : Parcelable {

    @Parcelize
    @JsonClass(generateAdapter = true)
    public data class Beacon(
        val proximity: Int,
        val major: Int,
        val minor: Int,
        val location: Location?,
        val timestamp: Date,
    ) : Parcelable {

        @Parcelize
        @JsonClass(generateAdapter = true)
        public data class Location(
            val latitude: Double,
            val longitude: Double,
        ) : Parcelable
    }

    public companion object {
        public operator fun invoke(region: NotificareRegion): NotificareBeaconSession {
            return NotificareBeaconSession(
                regionId = region.id,
                start = Date(),
                end = null,
                beacons = mutableListOf(),
            )
        }
    }
}
