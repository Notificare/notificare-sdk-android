package re.notifica.push.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class NotificareSystemNotification(
    val id: String,
    val type: String,
    val extra: Map<String, String?>,
) : Parcelable
