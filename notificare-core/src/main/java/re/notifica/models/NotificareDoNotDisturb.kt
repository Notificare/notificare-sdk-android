package re.notifica.models

import android.os.Parcelable
import com.squareup.moshi.JsonClass
import kotlinx.parcelize.Parcelize

@Parcelize
@JsonClass(generateAdapter = true)
data class NotificareDoNotDisturb(
    val start: NotificareTime,
    val end: NotificareTime
): Parcelable
