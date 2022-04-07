package re.notifica.push.models

import android.net.Uri
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
public data class NotificareUnknownNotification(
    val messageId: String?,
    val messageType: String?,
    val senderId: String?,
    val collapseKey: String?,
    val from: String?,
    val to: String?,
    val sentTime: Long,
    val ttl: Long,
    val priority: Int,
    val originalPriority: Int,
    val notification: Notification?,
    val data: Map<String, String?>,
) : Parcelable {

    @Parcelize
    public data class Notification(
        val title: String?,
        val titleLocalizationKey: String?,
        val titleLocalizationArgs: List<String>?,
        val body: String?,
        val bodyLocalizationKey: String?,
        val bodyLocalizationArgs: List<String>?,
        val icon: String?,
        val imageUrl: Uri?,
        val sound: String?,
        val tag: String?,
        val color: String?,
        val clickAction: String?,
        val channelId: String?,
        val link: Uri?,
        val ticker: String?,
        val sticky: Boolean,
        val localOnly: Boolean,
        val defaultSound: Boolean,
        val defaultVibrateSettings: Boolean,
        val defaultLightSettings: Boolean,
        val notificationPriority: Int?,
        val visibility: Int?,
        val notificationCount: Int?,
        val eventTime: Long?,
        val lightSettings: List<Int>?,
        val vibrateSettings: List<Long>?,
    ) : Parcelable
}
