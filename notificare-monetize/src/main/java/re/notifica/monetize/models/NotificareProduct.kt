package re.notifica.monetize.models

import android.os.Parcelable
import com.squareup.moshi.JsonClass
import kotlinx.parcelize.Parcelize
import org.json.JSONObject
import re.notifica.Notificare
import re.notifica.internal.moshi

@Parcelize
@JsonClass(generateAdapter = true)
public data class NotificareProduct(
    val id: String,
    val identifier: String,
    val name: String,
    val type: String,
    val storeDetails: StoreDetails?,
) : Parcelable {

    public fun toJson(): JSONObject {
        val jsonStr = adapter.toJson(this)
        return JSONObject(jsonStr)
    }

    public companion object {
        private val adapter = Notificare.moshi.adapter(NotificareProduct::class.java)

        public const val TYPE_CONSUMABLE: String = "consumable"
        public const val TYPE_ONE_TIME: String = "onetime"

        public fun fromJson(json: JSONObject): NotificareProduct {
            val jsonStr = json.toString()
            return requireNotNull(adapter.fromJson(jsonStr))
        }
    }

    @Parcelize
    @JsonClass(generateAdapter = true)
    public data class StoreDetails(
        val name: String,
        val title: String,
        val description: String,
        val price: Double,
        val currencyCode: String,
    ) : Parcelable {

        public fun toJson(): JSONObject {
            val jsonStr = adapter.toJson(this)
            return JSONObject(jsonStr)
        }

        public companion object {
            private val adapter = Notificare.moshi.adapter(StoreDetails::class.java)

            public fun fromJson(json: JSONObject): StoreDetails {
                val jsonStr = json.toString()
                return requireNotNull(adapter.fromJson(jsonStr))
            }
        }
    }
}
