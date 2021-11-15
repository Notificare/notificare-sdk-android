package re.notifica.models

import android.content.Context
import android.os.Parcelable
import com.squareup.moshi.JsonClass
import kotlinx.parcelize.Parcelize
import org.json.JSONObject
import re.notifica.Notificare
import re.notifica.internal.moshi

@Parcelize
@JsonClass(generateAdapter = true)
public data class NotificareApplication(
    val id: String,
    val name: String,
    val category: String,
    val services: Map<String, Boolean>,
    val inboxConfig: InboxConfig?,
    val regionConfig: RegionConfig?,
    val userDataFields: List<UserDataField>,
    val actionCategories: List<ActionCategory>
) : Parcelable {

    public fun toJson(): JSONObject {
        val jsonStr = adapter.toJson(this)
        return JSONObject(jsonStr)
    }

    public companion object {
        private val adapter = Notificare.moshi.adapter(NotificareApplication::class.java)

        public fun fromJson(json: JSONObject): NotificareApplication {
            val jsonStr = json.toString()
            return requireNotNull(adapter.fromJson(jsonStr))
        }
    }

    @Parcelize
    @JsonClass(generateAdapter = true)
    public data class InboxConfig(
        val useInbox: Boolean = false,
        val autoBadge: Boolean = false
    ) : Parcelable {

        public fun toJson(): JSONObject {
            val jsonStr = adapter.toJson(this)
            return JSONObject(jsonStr)
        }

        public companion object {
            private val adapter = Notificare.moshi.adapter(InboxConfig::class.java)

            public fun fromJson(json: JSONObject): InboxConfig {
                val jsonStr = json.toString()
                return requireNotNull(adapter.fromJson(jsonStr))
            }
        }
    }

    @Parcelize
    @JsonClass(generateAdapter = true)
    public data class RegionConfig(
        val proximityUUID: String?
    ) : Parcelable {

        public fun toJson(): JSONObject {
            val jsonStr = adapter.toJson(this)
            return JSONObject(jsonStr)
        }

        public companion object {
            private val adapter = Notificare.moshi.adapter(RegionConfig::class.java)

            public fun fromJson(json: JSONObject): RegionConfig {
                val jsonStr = json.toString()
                return requireNotNull(adapter.fromJson(jsonStr))
            }
        }
    }

    @Parcelize
    @JsonClass(generateAdapter = true)
    public data class UserDataField(
        val type: String,
        val key: String,
        val label: String
    ) : Parcelable {

        public fun toJson(): JSONObject {
            val jsonStr = adapter.toJson(this)
            return JSONObject(jsonStr)
        }

        public companion object {
            private val adapter = Notificare.moshi.adapter(UserDataField::class.java)

            public fun fromJson(json: JSONObject): UserDataField {
                val jsonStr = json.toString()
                return requireNotNull(adapter.fromJson(jsonStr))
            }
        }
    }

    @Parcelize
    @JsonClass(generateAdapter = true)
    public data class ActionCategory(
        val type: String,
        val name: String,
        val description: String?,
        val actions: List<Action>
    ) : Parcelable {

        public fun toJson(): JSONObject {
            val jsonStr = adapter.toJson(this)
            return JSONObject(jsonStr)
        }

        public companion object {
            private val adapter = Notificare.moshi.adapter(ActionCategory::class.java)

            public fun fromJson(json: JSONObject): ActionCategory {
                val jsonStr = json.toString()
                return requireNotNull(adapter.fromJson(jsonStr))
            }
        }

        @Parcelize
        @JsonClass(generateAdapter = true)
        public data class Action(
            val type: String,
            val label: String,
            val target: String?,
            val camera: Boolean,
            val keyboard: Boolean,
            val destructive: Boolean,
            val icon: Icon?,
        ) : Parcelable {

            public fun getLocalizedLabel(context: Context): String {
                val prefix = Notificare.options?.notificationActionLabelPrefix ?: ""
                val resourceName = "$prefix$label"
                val resource = context.resources.getIdentifier(resourceName, "string", context.packageName)

                return if (resource == 0) label else context.getString(resource)
            }

            public fun getIconResource(context: Context): Int {
                val icon = icon?.android ?: return 0

                return context.resources.getIdentifier(icon, "drawable", "android")
            }

            public fun toJson(): JSONObject {
                val jsonStr = adapter.toJson(this)
                return JSONObject(jsonStr)
            }

            public companion object {
                private val adapter = Notificare.moshi.adapter(Action::class.java)

                public fun fromJson(json: JSONObject): Action {
                    val jsonStr = json.toString()
                    return requireNotNull(adapter.fromJson(jsonStr))
                }
            }

            @Parcelize
            @JsonClass(generateAdapter = true)
            public data class Icon(
                val android: String?,
                val ios: String?,
                val web: String?,
            ) : Parcelable {

                public fun toJson(): JSONObject {
                    val jsonStr = adapter.toJson(this)
                    return JSONObject(jsonStr)
                }

                public companion object {
                    private val adapter = Notificare.moshi.adapter(Icon::class.java)

                    public fun fromJson(json: JSONObject): Icon {
                        val jsonStr = json.toString()
                        return requireNotNull(adapter.fromJson(jsonStr))
                    }
                }
            }
        }
    }

    public object ServiceKeys {
        public const val OAUTH2: String = "oauth2"
        public const val RICH_PUSH: String = "richPush"
        public const val LOCATION_SERVICES: String = "locationServices"
        public const val APNS: String = "apns"
        public const val GCM: String = "gcm"
        public const val WEBSOCKETS: String = "websockets"
        public const val PASSBOOK: String = "passbook"
        public const val IN_APP_PURCHASE: String = "inAppPurchase"
        public const val INBOX: String = "inbox"
        public const val STORAGE: String = "storage"
    }
}
