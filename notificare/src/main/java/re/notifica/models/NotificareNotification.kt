package re.notifica.models

import android.content.Context
import android.os.Parcelable
import com.squareup.moshi.JsonClass
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.WriteWith
import org.json.JSONObject
import re.notifica.Notificare
import re.notifica.internal.parcelize.NotificationContentDataParceler
import re.notifica.internal.parcelize.NotificareExtraParceler
import java.util.*

@Parcelize
@JsonClass(generateAdapter = true)
data class NotificareNotification(
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

    fun toJson(): JSONObject {
        val jsonStr = adapter.toJson(this)
        return JSONObject(jsonStr)
    }

    companion object {
        const val TYPE_NONE = "re.notifica.notification.None"
        const val TYPE_ALERT = "re.notifica.notification.Alert"
        const val TYPE_WEB_VIEW = "re.notifica.notification.WebView"
        const val TYPE_URL = "re.notifica.notification.URL"
        const val TYPE_URL_SCHEME = "re.notifica.notification.URLScheme"
        const val TYPE_IMAGE = "re.notifica.notification.Image"
        const val TYPE_VIDEO = "re.notifica.notification.Video"
        const val TYPE_MAP = "re.notifica.notification.Map"
        const val TYPE_RATE = "re.notifica.notification.Rate"
        const val TYPE_PASSBOOK = "re.notifica.notification.Passbook"
        const val TYPE_STORE = "re.notifica.notification.Store"

        private val adapter = Notificare.moshi.adapter(NotificareNotification::class.java)

        fun fromJson(json: JSONObject): NotificareNotification {
            val jsonStr = json.toString()
            return requireNotNull(adapter.fromJson(jsonStr))
        }
    }

    @Parcelize
    @JsonClass(generateAdapter = true)
    data class Content(
        val type: String,
        val data: @WriteWith<NotificationContentDataParceler> Any,
    ) : Parcelable {

        fun toJson(): JSONObject {
            val jsonStr = adapter.toJson(this)
            return JSONObject(jsonStr)
        }

        companion object {
            const val TYPE_HTML = "re.notifica.content.HTML"
            const val TYPE_GOOGLE_PLAY_DETAILS = "re.notifica.content.GooglePlayDetails"
            const val TYPE_GOOGLE_PLAY_DEVELOPER = "re.notifica.content.GooglePlayDeveloper"
            const val TYPE_GOOGLE_PLAY_SEARCH = "re.notifica.content.GooglePlaySearch"
            const val TYPE_GOOGLE_PLAY_COLLECTION = "re.notifica.content.GooglePlayCollection"
            const val TYPE_APP_GALLERY_DETAILS = "re.notifica.content.AppGalleryDetails"
            const val TYPE_APP_GALLERY_SEARCH = "re.notifica.content.AppGallerySearch"

            private val adapter = Notificare.moshi.adapter(Content::class.java)

            fun fromJson(json: JSONObject): Content {
                val jsonStr = json.toString()
                return requireNotNull(adapter.fromJson(jsonStr))
            }
        }
    }

    @Parcelize
    @JsonClass(generateAdapter = true)
    data class Action(
        val type: String,
        val label: String,
        val target: String?,
        val keyboard: Boolean,
        val camera: Boolean,
    ) : Parcelable {

        fun getLocalizedLabel(context: Context): String {
            val prefix = Notificare.options?.notificationActionLabelPrefix ?: ""
            val resourceName = "$prefix$label"
            val resource = context.resources.getIdentifier(resourceName, "string", context.packageName)

            return if (resource == 0) label else context.getString(resource)
        }

        fun toJson(): JSONObject {
            val jsonStr = adapter.toJson(this)
            return JSONObject(jsonStr)
        }

        companion object {
            const val TYPE_APP = "re.notifica.action.App"
            const val TYPE_BROWSER = "re.notifica.action.Browser"
            const val TYPE_CALLBACK = "re.notifica.action.Callback"
            const val TYPE_CUSTOM = "re.notifica.action.Custom"
            const val TYPE_MAIL = "re.notifica.action.Mail"
            const val TYPE_SMS = "re.notifica.action.SMS"
            const val TYPE_TELEPHONE = "re.notifica.action.Telephone"
            const val TYPE_WEB_VIEW = "re.notifica.action.WebView"

            private val adapter = Notificare.moshi.adapter(Action::class.java)

            fun fromJson(json: JSONObject): Action {
                val jsonStr = json.toString()
                return requireNotNull(adapter.fromJson(jsonStr))
            }
        }
    }

    @Parcelize
    @JsonClass(generateAdapter = true)
    data class Attachment(
        val mimeType: String,
        val uri: String,
    ) : Parcelable {

        fun toJson(): JSONObject {
            val jsonStr = adapter.toJson(this)
            return JSONObject(jsonStr)
        }

        companion object {
            private val adapter = Notificare.moshi.adapter(Attachment::class.java)

            fun fromJson(json: JSONObject): Attachment {
                val jsonStr = json.toString()
                return requireNotNull(adapter.fromJson(jsonStr))
            }
        }
    }

    enum class NotificationType {
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

        companion object {
            fun from(type: String): NotificationType? {
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
