package re.notifica.authentication.models

import android.os.Parcelable
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.parcelize.Parcelize
import org.json.JSONObject
import re.notifica.Notificare

@Parcelize
@JsonClass(generateAdapter = true)
data class NotificareUserPreference(
    val id: String,
    val label: String,
    val type: Type,
    val options: List<Option>,
    val position: Int,
) : Parcelable {

    fun toJson(): JSONObject {
        val jsonStr = adapter.toJson(this)
        return JSONObject(jsonStr)
    }

    companion object {
        private val adapter = Notificare.moshi.adapter(NotificareUserPreference::class.java)

        fun fromJson(json: JSONObject): NotificareUserPreference {
            val jsonStr = json.toString()
            return requireNotNull(adapter.fromJson(jsonStr))
        }
    }

    @Parcelize
    enum class Type : Parcelable {
        @Json(name = "single")
        SINGLE,

        @Json(name = "choice")
        CHOICE,

        @Json(name = "select")
        SELECT;

        fun toJson(): String {
            return adapter.toJsonValue(this) as String
        }

        companion object {
            private val adapter = Notificare.moshi.adapter(Type::class.java)

            fun fromJson(json: String): Type {
                return requireNotNull(adapter.fromJsonValue(json))
            }
        }
    }

    @Parcelize
    @JsonClass(generateAdapter = true)
    data class Option(
        val label: String,
        val segmentId: String,
        val selected: Boolean,
    ) : Parcelable {

        fun toJson(): JSONObject {
            val jsonStr = adapter.toJson(this)
            return JSONObject(jsonStr)
        }

        companion object {
            private val adapter = Notificare.moshi.adapter(Option::class.java)

            fun fromJson(json: JSONObject): Option {
                val jsonStr = json.toString()
                return requireNotNull(adapter.fromJson(jsonStr))
            }
        }
    }
}
