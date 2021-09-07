package re.notifica.scannables.models

import android.os.Parcelable
import com.squareup.moshi.JsonClass
import kotlinx.parcelize.Parcelize
import re.notifica.models.NotificareNotification

@Parcelize
@JsonClass(generateAdapter = true)
public data class NotificareScannable(
    val id: String,
    val name: String,
    val tag: String,
    val type: String,
    val notification: NotificareNotification?,
) : Parcelable
