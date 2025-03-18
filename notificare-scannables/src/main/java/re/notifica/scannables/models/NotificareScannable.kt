package re.notifica.scannables.models

import android.os.Parcelable
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

    public fun toJson(): JSONObject {
        val jsonStr = adapter.toJson(this)
        return JSONObject(jsonStr)
    }

    public companion object {
        private val adapter = Notificare.moshi.adapter(NotificareScannable::class.java)

        public fun fromJson(json: JSONObject): NotificareScannable {
            val jsonStr = json.toString()
            return requireNotNull(adapter.fromJson(jsonStr))
        }
    }
}
