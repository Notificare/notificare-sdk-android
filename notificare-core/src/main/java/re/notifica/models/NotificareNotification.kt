package re.notifica.models

import android.os.Parcelable
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.WriteWith
import re.notifica.internal.parcelize.NotificationContentDataParceler
import re.notifica.internal.parcelize.NotificationExtraParceler
import java.util.*

@Parcelize
@JsonClass(generateAdapter = true)
data class NotificareNotification(
    @Json(name = "_id") val id: String,
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
    ) : Parcelable

    @Parcelize
    @JsonClass(generateAdapter = true)
    data class Action(
        val type: String,
        val label: String,
        val target: String?,
        val keyboard: Boolean,
        val camera: Boolean,
    ) : Parcelable

    @Parcelize
    @JsonClass(generateAdapter = true)
    data class Attachment(
        val mimeType: String,
        val uri: String,
    ) : Parcelable
}
