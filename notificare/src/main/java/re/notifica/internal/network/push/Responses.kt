package re.notifica.internal.network.push

import androidx.annotation.RestrictTo
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import re.notifica.models.NotificareApplication
import re.notifica.models.NotificareDoNotDisturb
import re.notifica.models.NotificareDynamicLink
import re.notifica.models.NotificareNotification
import java.util.*

@JsonClass(generateAdapter = true)
internal data class ApplicationResponse(
    val application: Application
) {

    @JsonClass(generateAdapter = true)
    internal data class Application(
        @Json(name = "_id") val id: String,
        val name: String,
        val category: String,
        val services: Map<String, Boolean>,
        val inboxConfig: NotificareApplication.InboxConfig?,
        val regionConfig: NotificareApplication.RegionConfig?,
        val userDataFields: List<NotificareApplication.UserDataField>,
        val actionCategories: List<NotificareApplication.ActionCategory>
    ) {
        fun toModel(): NotificareApplication {
            return NotificareApplication(
                id,
                name,
                category,
                services,
                inboxConfig,
                regionConfig,
                userDataFields,
                actionCategories
            )
        }
    }
}

@JsonClass(generateAdapter = true)
internal data class DeviceDoNotDisturbResponse(
    val dnd: NotificareDoNotDisturb?,
)

@JsonClass(generateAdapter = true)
internal data class DeviceTagsResponse(
    val tags: List<String>
)

@JsonClass(generateAdapter = true)
internal data class DeviceUserDataResponse(
    val userData: Map<String, String?>?,
)

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@JsonClass(generateAdapter = true)
public data class NotificationResponse(
    val notification: Notification,
) {

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @JsonClass(generateAdapter = true)
    public data class Notification(
        @Json(name = "_id") val id: String,
        val partial: Boolean = false,
        val type: String,
        val time: Date,
        val title: String?,
        val subtitle: String?,
        val message: String,
        val content: List<NotificareNotification.Content> = listOf(),
        val actions: List<NotificareNotification.Action> = listOf(),
        val attachments: List<NotificareNotification.Attachment> = listOf(),
        val extra: Map<String, Any> = mapOf(),
    ) {
        public fun toModel(): NotificareNotification {
            return NotificareNotification(
                id,
                partial,
                type,
                time,
                title,
                subtitle,
                message,
                content,
                actions,
                attachments,
                extra
            )
        }
    }
}

@JsonClass(generateAdapter = true)
internal data class NotificareUploadResponse(
    val filename: String,
)

@JsonClass(generateAdapter = true)
internal data class DynamicLinkResponse(
    val link: NotificareDynamicLink,
)
