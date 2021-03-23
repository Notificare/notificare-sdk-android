package re.notifica.models

import android.content.Context
import android.os.Parcelable
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.WriteWith
import re.notifica.Notificare
import re.notifica.internal.parcelize.NotificationContentDataParceler
import re.notifica.internal.parcelize.NotificationExtraParceler
import java.util.*

@Parcelize
@JsonClass(generateAdapter = true)
data class NotificareNotification(
    @Json(name = "_id") val id: String,
    val partial: Boolean = false,
    val type: String,
    val time: Date,
    val title: String?,
    val subtitle: String?,
    val message: String,
    val content: List<Content> = listOf(),
    val actions: List<Action> = listOf(),
    val attachments: List<Attachment> = listOf(),
    val extra: @WriteWith<NotificationExtraParceler> Map<String, Any> = mapOf(),
) : Parcelable {

    @Parcelize
    @JsonClass(generateAdapter = true)
    data class Content(
        val type: String,
        val data: @WriteWith<NotificationContentDataParceler> Any,
    ) : Parcelable {

        companion object {
            const val TYPE_HTML = "re.notifica.content.HTML"
            const val TYPE_GOOGLE_PLAY_DETAILS = "re.notifica.content.GooglePlayDetails"
            const val TYPE_GOOGLE_PLAY_DEVELOPER = "re.notifica.content.GooglePlayDeveloper"
            const val TYPE_GOOGLE_PLAY_SEARCH = "re.notifica.content.GooglePlaySearch"
            const val TYPE_GOOGLE_PLAY_COLLECTION = "re.notifica.content.GooglePlayCollection"
            const val TYPE_APP_GALLERY_DETAILS = "re.notifica.content.AppGalleryDetails"
            const val TYPE_APP_GALLERY_SEARCH = "re.notifica.content.AppGallerySearch"
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
    }

    @Parcelize
    @JsonClass(generateAdapter = true)
    data class Attachment(
        val mimeType: String,
        val uri: String,
    ) : Parcelable


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
                    "re.notifica.notification.None" -> NONE
                    "re.notifica.notification.Alert" -> ALERT
                    "re.notifica.notification.WebView" -> WEB_VIEW
                    "re.notifica.notification.URL" -> URL
                    "re.notifica.notification.URLScheme" -> URL_SCHEME
                    "re.notifica.notification.Image" -> IMAGE
                    "re.notifica.notification.Video" -> VIDEO
                    "re.notifica.notification.Map" -> MAP
                    "re.notifica.notification.Rate" -> RATE
                    "re.notifica.notification.Passbook" -> PASSBOOK
                    "re.notifica.notification.Store" -> STORE
                    else -> null
                }
            }
        }
    }
}
