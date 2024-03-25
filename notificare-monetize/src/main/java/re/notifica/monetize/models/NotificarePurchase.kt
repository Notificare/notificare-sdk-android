package re.notifica.monetize.models

import android.os.Parcelable
import com.squareup.moshi.JsonClass
import java.util.Date
import kotlinx.parcelize.Parcelize
import org.json.JSONObject
import re.notifica.Notificare
import re.notifica.internal.moshi

@Parcelize
@JsonClass(generateAdapter = true)
public data class NotificarePurchase(
    val id: String,
    val productIdentifier: String,
    val originalJson: String,
    val packageName: String?,
    val time: Date,
    val token: String,
    val signature: String,
    val isAcknowledged: Boolean,
) : Parcelable {

    public fun toJson(): JSONObject {
        val jsonStr = adapter.toJson(this)
        return JSONObject(jsonStr)
    }

    public companion object {
        private val adapter = Notificare.moshi.adapter(NotificarePurchase::class.java)

        public fun fromJson(json: JSONObject): NotificarePurchase {
            val jsonStr = json.toString()
            return requireNotNull(adapter.fromJson(jsonStr))
        }
    }
}
