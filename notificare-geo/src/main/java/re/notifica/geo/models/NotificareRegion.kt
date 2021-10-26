package re.notifica.geo.models

import android.os.Parcelable
import com.squareup.moshi.JsonAdapter
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
    val timeZoneOffset: Int,
) : Parcelable {
    public companion object;

    public val isPolygon: Boolean
        get() = advancedGeometry != null

    @Parcelize
    @JsonClass(generateAdapter = true)
    public data class Geometry(
        val type: String,
        val coordinate: Coordinate,
    ) : Parcelable {
        public companion object
    }

    @Parcelize
    @JsonClass(generateAdapter = true)
    public data class AdvancedGeometry(
        val type: String,
        val coordinates: List<Coordinate>,
    ) : Parcelable {
        public companion object
    }

    @Parcelize
    @JsonClass(generateAdapter = true)
    public data class Coordinate(
        val latitude: Double,
        val longitude: Double,
    ) : Parcelable {
        public companion object
    }
}

// region JSON: NotificareRegion

public val NotificareRegion.Companion.adapter: JsonAdapter<NotificareRegion>
    get() = Notificare.moshi.adapter(NotificareRegion::class.java)

public fun NotificareRegion.Companion.fromJson(json: JSONObject): NotificareRegion {
    val jsonStr = json.toString()
    return requireNotNull(adapter.fromJson(jsonStr))
}

public fun NotificareRegion.toJson(): JSONObject {
    val jsonStr = NotificareRegion.adapter.toJson(this)
    return JSONObject(jsonStr)
}

// endregion

// region JSON: NotificareRegion.Geometry

public val NotificareRegion.Geometry.Companion.adapter: JsonAdapter<NotificareRegion.Geometry>
    get() = Notificare.moshi.adapter(NotificareRegion.Geometry::class.java)

public fun NotificareRegion.Geometry.Companion.fromJson(json: JSONObject): NotificareRegion.Geometry {
    val jsonStr = json.toString()
    return requireNotNull(adapter.fromJson(jsonStr))
}

public fun NotificareRegion.Geometry.toJson(): JSONObject {
    val jsonStr = NotificareRegion.Geometry.adapter.toJson(this)
    return JSONObject(jsonStr)
}

// endregion

// region JSON: NotificareRegion.AdvancedGeometry

public val NotificareRegion.AdvancedGeometry.Companion.adapter: JsonAdapter<NotificareRegion.AdvancedGeometry>
    get() = Notificare.moshi.adapter(NotificareRegion.AdvancedGeometry::class.java)

public fun NotificareRegion.AdvancedGeometry.Companion.fromJson(json: JSONObject): NotificareRegion.AdvancedGeometry {
    val jsonStr = json.toString()
    return requireNotNull(adapter.fromJson(jsonStr))
}

public fun NotificareRegion.AdvancedGeometry.toJson(): JSONObject {
    val jsonStr = NotificareRegion.AdvancedGeometry.adapter.toJson(this)
    return JSONObject(jsonStr)
}

// endregion

// region JSON: NotificareRegion.Coordinate

public val NotificareRegion.Coordinate.Companion.adapter: JsonAdapter<NotificareRegion.Coordinate>
    get() = Notificare.moshi.adapter(NotificareRegion.Coordinate::class.java)

public fun NotificareRegion.Coordinate.Companion.fromJson(json: JSONObject): NotificareRegion.Coordinate {
    val jsonStr = json.toString()
    return requireNotNull(adapter.fromJson(jsonStr))
}

public fun NotificareRegion.Coordinate.toJson(): JSONObject {
    val jsonStr = NotificareRegion.Coordinate.adapter.toJson(this)
    return JSONObject(jsonStr)
}

// endregion
