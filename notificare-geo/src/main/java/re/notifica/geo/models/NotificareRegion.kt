package re.notifica.geo.models

import android.os.Parcelable
import com.squareup.moshi.JsonClass
import kotlinx.parcelize.Parcelize
import org.json.JSONObject
import re.notifica.Notificare
import re.notifica.internal.moshi

@Parcelize
@JsonClass(generateAdapter = true)
public data class NotificareRegion(
    val id: String,
    val name: String,
    val description: String?,
    val referenceKey: String?,
    val geometry: Geometry,
    val advancedGeometry: AdvancedGeometry?,
    val major: Int?,
    val distance: Double,
    val timeZone: String,
    val timeZoneOffset: Double,
) : Parcelable {

    public val isPolygon: Boolean
        get() = advancedGeometry != null

    public fun toJson(): JSONObject {
        val jsonStr = adapter.toJson(this)
        return JSONObject(jsonStr)
    }

    public companion object {
        private val adapter = Notificare.moshi.adapter(NotificareRegion::class.java)

        public fun fromJson(json: JSONObject): NotificareRegion {
            val jsonStr = json.toString()
            return requireNotNull(adapter.fromJson(jsonStr))
        }
    }

    @Parcelize
    @JsonClass(generateAdapter = true)
    public data class Geometry(
        val type: String,
        val coordinate: Coordinate,
    ) : Parcelable {

        public fun toJson(): JSONObject {
            val jsonStr = adapter.toJson(this)
            return JSONObject(jsonStr)
        }

        public companion object {
            private val adapter = Notificare.moshi.adapter(NotificareRegion.Geometry::class.java)

            public fun fromJson(json: JSONObject): Geometry {
                val jsonStr = json.toString()
                return requireNotNull(adapter.fromJson(jsonStr))
            }
        }
    }

    @Parcelize
    @JsonClass(generateAdapter = true)
    public data class AdvancedGeometry(
        val type: String,
        val coordinates: List<Coordinate>,
    ) : Parcelable {

        public fun toJson(): JSONObject {
            val jsonStr = adapter.toJson(this)
            return JSONObject(jsonStr)
        }

        public companion object {
            private val adapter = Notificare.moshi.adapter(AdvancedGeometry::class.java)

            public fun fromJson(json: JSONObject): AdvancedGeometry {
                val jsonStr = json.toString()
                return requireNotNull(adapter.fromJson(jsonStr))
            }
        }
    }

    @Parcelize
    @JsonClass(generateAdapter = true)
    public data class Coordinate(
        val latitude: Double,
        val longitude: Double,
    ) : Parcelable {

        public fun toJson(): JSONObject {
            val jsonStr = adapter.toJson(this)
            return JSONObject(jsonStr)
        }

        public companion object {
            private val adapter = Notificare.moshi.adapter(Coordinate::class.java)

            public fun fromJson(json: JSONObject): Coordinate {
                val jsonStr = json.toString()
                return requireNotNull(adapter.fromJson(jsonStr))
            }
        }
    }
}
