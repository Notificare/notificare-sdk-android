package re.notifica.models

import android.os.Parcelable
import com.squareup.moshi.JsonClass
import kotlinx.parcelize.Parcelize
import org.json.JSONObject
import re.notifica.Notificare

@Parcelize
@JsonClass(generateAdapter = true)
data class NotificareDynamicLink(
    val target: String,
): Parcelable {

    fun toJson(): JSONObject {
        val jsonStr = adapter.toJson(this)
        return JSONObject(jsonStr)
    }

    companion object {
        private val adapter = Notificare.moshi.adapter(NotificareDynamicLink::class.java)

        fun fromJson(json: JSONObject): NotificareDynamicLink {
            val jsonStr = json.toString()
            return requireNotNull(adapter.fromJson(jsonStr))
        }
    }
}
