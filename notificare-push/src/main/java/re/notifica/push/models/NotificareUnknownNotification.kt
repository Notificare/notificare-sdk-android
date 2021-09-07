package re.notifica.push.models

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
    val data: Map<String, String?>
): Parcelable
