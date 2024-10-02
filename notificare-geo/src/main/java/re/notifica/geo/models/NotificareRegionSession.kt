package re.notifica.geo.models

import android.os.Parcelable
import com.squareup.moshi.JsonClass
import java.util.Date
import kotlinx.parcelize.Parcelize

@Parcelize
@JsonClass(generateAdapter = true)
public data class NotificareRegionSession(
    val regionId: String,
    val start: Date,
    val end: Date?,
    val locations: MutableList<NotificareLocation>,
) : Parcelable {

    public companion object {
        public operator fun invoke(region: NotificareRegion): NotificareRegionSession {
            return NotificareRegionSession(
                regionId = region.id,
                start = Date(),
                end = null,
                locations = mutableListOf(),
            )
        }
    }
}
