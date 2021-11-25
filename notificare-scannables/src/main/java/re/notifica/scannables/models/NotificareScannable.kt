package re.notifica.scannables.models

import android.os.Parcelable
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonClass
import kotlinx.parcelize.Parcelize
import org.json.JSONObject
import re.notifica.Notificare
import re.notifica.internal.moshi
import re.notifica.models.NotificareNotification

@Parcelize
@JsonClass(generateAdapter = true)
public data class NotificareScannable(
    val id: String,
    val name: String,
    val tag: String,
    val type: String,
    val notification: NotificareNotification?,
) : Parcelable {
    public companion object
}

// region JSON: NotificareScannable

public val NotificareScannable.Companion.adapter: JsonAdapter<NotificareScannable>
    get() = Notificare.moshi.adapter(NotificareScannable::class.java)

public fun NotificareScannable.Companion.fromJson(json: JSONObject): NotificareScannable {
    val jsonStr = json.toString()
    return requireNotNull(adapter.fromJson(jsonStr))
}

public fun NotificareScannable.toJson(): JSONObject {
    val jsonStr = NotificareScannable.adapter.toJson(this)
    return JSONObject(jsonStr)
}

// endregion
