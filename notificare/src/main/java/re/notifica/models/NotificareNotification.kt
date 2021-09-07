package re.notifica.models

import android.content.Context
import android.os.Parcelable
import com.squareup.moshi.JsonClass
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.WriteWith
import org.json.JSONObject
import re.notifica.Notificare
import re.notifica.internal.moshi
import re.notifica.internal.parcelize.NotificationContentDataParceler
import re.notifica.internal.parcelize.NotificareExtraParceler
import java.util.*

@Parcelize
@JsonClass(generateAdapter = true)
public data class NotificareNotification(
    val id: String,
    val partial: Boolean = false,
    val type: String,
    val time: Date,
    val title: String?,
    val subtitle: String?,
    val message: String,
    val content: List<Content> = listOf(),
    val actions: List<Action> = listOf(),
    val attachments: List<Attachment> = listOf(),
    val extra: @WriteWith<NotificareExtraParceler> Map<String, Any> = mapOf(),
) : Parcelable {

    public fun toJson(): JSONObject {
        val jsonStr = adapter.toJson(this)
        return JSONObject(jsonStr)
    }

    public companion object {
        public const val TYPE_NONE: String = "re.notifica.notification.None"
        public const val TYPE_ALERT: String = "re.notifica.notification.Alert"
        public const val TYPE_WEB_VIEW: String = "re.notifica.notification.WebView"
        public const val TYPE_URL: String = "re.notifica.notification.URL"
        public const val TYPE_URL_SCHEME: String = "re.notifica.notification.URLScheme"
        public const val TYPE_IMAGE: String = "re.notifica.notification.Image"
        public const val TYPE_VIDEO: String = "re.notifica.notification.Video"
        public const val TYPE_MAP: String = "re.notifica.notification.Map"
        public const val TYPE_RATE: String = "re.notifica.notification.Rate"
        public const val TYPE_PASSBOOK: String = "re.notifica.notification.Passbook"
        public const val TYPE_STORE: String = "re.notifica.notification.Store"

        private val adapter = Notificare.moshi.adapter(NotificareNotification::class.java)

        public fun fromJson(json: JSONObject): NotificareNotification {
            val jsonStr = json.toString()
            return requireNotNull(adapter.fromJson(jsonStr))
        }
    }

    @Parcelize
    @JsonClass(generateAdapter = true)
    public data class Content(
        val type: String,
        val data: @WriteWith<NotificationContentDataParceler> Any,
    ) : Parcelable {

        public fun toJson(): JSONObject {
            val jsonStr = adapter.toJson(this)
            return JSONObject(jsonStr)
        }

        public companion object {
            public const val TYPE_HTML: String = "re.notifica.content.HTML"
            public const val TYPE_GOOGLE_PLAY_DETAILS: String = "re.notifica.content.GooglePlayDetails"
            public const val TYPE_GOOGLE_PLAY_DEVELOPER: String = "re.notifica.content.GooglePlayDeveloper"
            public const val TYPE_GOOGLE_PLAY_SEARCH: String = "re.notifica.content.GooglePlaySearch"
            public const val TYPE_GOOGLE_PLAY_COLLECTION: String = "re.notifica.content.GooglePlayCollection"
            public const val TYPE_APP_GALLERY_DETAILS: String = "re.notifica.content.AppGalleryDetails"
            public const val TYPE_APP_GALLERY_SEARCH: String = "re.notifica.content.AppGallerySearch"

            private val adapter = Notificare.moshi.adapter(Content::class.java)

            public fun fromJson(json: JSONObject): Content {
                val jsonStr = json.toString()
                return requireNotNull(adapter.fromJson(jsonStr))
            }
        }
    }

    @Parcelize
    @JsonClass(generateAdapter = true)
    public data class Action(
        val type: String,
        val label: String,
        val target: String?,
        val keyboard: Boolean,
        val camera: Boolean,
    ) : Parcelable {

        public fun getLocalizedLabel(context: Context): String {
            val prefix = Notificare.options?.notificationActionLabelPrefix ?: ""
            val resourceName = "$prefix$label"
            val resource = context.resources.getIdentifier(resourceName, "string", context.packageName)

            return if (resource == 0) label else context.getString(resource)
        }

        public fun toJson(): JSONObject {
            val jsonStr = adapter.toJson(this)
            return JSONObject(jsonStr)
        }

        public companion object {
            public const val TYPE_APP: String = "re.notifica.action.App"
            public const val TYPE_BROWSER: String = "re.notifica.action.Browser"
            public const val TYPE_CALLBACK: String = "re.notifica.action.Callback"
            public const val TYPE_CUSTOM: String = "re.notifica.action.Custom"
            public const val TYPE_MAIL: String = "re.notifica.action.Mail"
            public const val TYPE_SMS: String = "re.notifica.action.SMS"
            public const val TYPE_TELEPHONE: String = "re.notifica.action.Telephone"
            public const val TYPE_WEB_VIEW: String = "re.notifica.action.WebView"

            private val adapter = Notificare.moshi.adapter(Action::class.java)

            public fun fromJson(json: JSONObject): Action {
                val jsonStr = json.toString()
                return requireNotNull(adapter.fromJson(jsonStr))
            }
        }
    }

    @Parcelize
    @JsonClass(generateAdapter = true)
    public data class Attachment(
        val mimeType: String,
        val uri: String,
    ) : Parcelable {

        public fun toJson(): JSONObject {
            val jsonStr = adapter.toJson(this)
            return JSONObject(jsonStr)
        }

        public companion object {
            private val adapter = Notificare.moshi.adapter(Attachment::class.java)

            public fun fromJson(json: JSONObject): Attachment {
                val jsonStr = json.toString()
                return requireNotNull(adapter.fromJson(jsonStr))
            }
        }
    }

    public enum class NotificationType {
        NONE,
        ALERT,
        WEB_VIEW,
        URL,
        URL_SCHEME,
        IMAGE,
        VIDEO,
        MAP,
        RATE,
        PASSBOOK,
        STORE;

        public companion object {
            public fun from(type: String): NotificationType? {
                return when (type) {
                    TYPE_NONE -> NONE
                    TYPE_ALERT -> ALERT
                    TYPE_WEB_VIEW -> WEB_VIEW
                    TYPE_URL -> URL
                    TYPE_URL_SCHEME -> URL_SCHEME
                    TYPE_IMAGE -> IMAGE
                    TYPE_VIDEO -> VIDEO
                    TYPE_MAP -> MAP
                    TYPE_RATE -> RATE
                    TYPE_PASSBOOK -> PASSBOOK
                    TYPE_STORE -> STORE
                    else -> null
                }
            }
        }
    }
}
