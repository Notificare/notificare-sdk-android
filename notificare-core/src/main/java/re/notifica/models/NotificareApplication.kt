package re.notifica.models

import android.os.Parcelable
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.parcelize.Parcelize
import org.json.JSONObject
import re.notifica.Notificare

@Parcelize
@JsonClass(generateAdapter = true)
data class NotificareApplication(
    @Json(name = "_id") val id: String,
    val name: String,
    val category: String,
    val services: Map<String, Boolean>,
    val inboxConfig: InboxConfig?,
    val regionConfig: RegionConfig?,
    val userDataFields: List<UserDataField>,
    val actionCategories: List<ActionCategory>
) : Parcelable {

    fun toJson(): JSONObject {
        val jsonStr = adapter.toJson(this)
        return JSONObject(jsonStr)
    }

    companion object {
        private val adapter = Notificare.moshi.adapter(NotificareApplication::class.java)

        fun fromJson(json: JSONObject): NotificareApplication {
            val jsonStr = json.toString()
            return requireNotNull(adapter.fromJson(jsonStr))
        }
    }

    @Parcelize
    @JsonClass(generateAdapter = true)
    data class InboxConfig(
        val useInbox: Boolean = false,
        val autoBadge: Boolean = false
    ) : Parcelable {

        fun toJson(): JSONObject {
            val jsonStr = adapter.toJson(this)
            return JSONObject(jsonStr)
        }

        companion object {
            private val adapter = Notificare.moshi.adapter(InboxConfig::class.java)

            fun fromJson(json: JSONObject): InboxConfig {
                val jsonStr = json.toString()
                return requireNotNull(adapter.fromJson(jsonStr))
            }
        }
    }

    @Parcelize
    @JsonClass(generateAdapter = true)
    data class RegionConfig(
        val proximityUUID: String?
    ) : Parcelable {

        fun toJson(): JSONObject {
            val jsonStr = adapter.toJson(this)
            return JSONObject(jsonStr)
        }

        companion object {
            private val adapter = Notificare.moshi.adapter(RegionConfig::class.java)

            fun fromJson(json: JSONObject): RegionConfig {
                val jsonStr = json.toString()
                return requireNotNull(adapter.fromJson(jsonStr))
            }
        }
    }

    @Parcelize
    @JsonClass(generateAdapter = true)
    data class UserDataField(
        val type: String,
        val key: String,
        val label: String
    ) : Parcelable {

        fun toJson(): JSONObject {
            val jsonStr = adapter.toJson(this)
            return JSONObject(jsonStr)
        }

        companion object {
            private val adapter = Notificare.moshi.adapter(UserDataField::class.java)

            fun fromJson(json: JSONObject): UserDataField {
                val jsonStr = json.toString()
                return requireNotNull(adapter.fromJson(jsonStr))
            }
        }
    }

    @Parcelize
    @JsonClass(generateAdapter = true)
    data class ActionCategory(
        val type: String,
        val name: String,
        val actions: List<NotificareNotification.Action>
    ) : Parcelable {

        fun toJson(): JSONObject {
            val jsonStr = adapter.toJson(this)
            return JSONObject(jsonStr)
        }

        companion object {
            private val adapter = Notificare.moshi.adapter(ActionCategory::class.java)

            fun fromJson(json: JSONObject): ActionCategory {
                val jsonStr = json.toString()
                return requireNotNull(adapter.fromJson(jsonStr))
            }
        }
    }

    @Suppress("unused")
    object ServiceKeys {
        const val OAUTH2 = "oauth2"
        const val RICH_PUSH = "richPush"
        const val LOCATION_SERVICES = "locationServices"
        const val APNS = "apns"
        const val GCM = "gcm"
        const val WEBSOCKETS = "websockets"
        const val PASSBOOK = "passbook"
        const val IN_APP_PURCHASE = "inAppPurchase"
        const val INBOX = "inbox"
        const val STORAGE = "storage"
    }
}
