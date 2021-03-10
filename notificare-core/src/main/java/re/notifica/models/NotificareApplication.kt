package re.notifica.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

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
) {

    @JsonClass(generateAdapter = true)
    data class InboxConfig(
        val useInbox: Boolean = false,
        val autoBadge: Boolean = false
    )

    @JsonClass(generateAdapter = true)
    data class RegionConfig(
        val proximityUUID: String?
    )

    @JsonClass(generateAdapter = true)
    data class UserDataField(
        val type: String,
        val key: String,
        val label: String
    )

    @JsonClass(generateAdapter = true)
    data class ActionCategory(
        val type: String,
        val name: String,
        val actions: List<NotificareNotification.Action>
    )

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
