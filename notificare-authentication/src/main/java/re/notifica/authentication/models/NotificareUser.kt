package re.notifica.authentication.models

import android.os.Parcelable
import com.squareup.moshi.JsonClass
import kotlinx.parcelize.Parcelize
import org.json.JSONObject
import re.notifica.Notificare
import re.notifica.internal.moshi
import java.util.*

@Parcelize
@JsonClass(generateAdapter = true)
public data class NotificareUser(
    val id: String,
    val name: String,
    val pushEmailAddress: String?,
    val segments: List<String>,
    val registrationDate: Date,
    val lastActive: Date,
) : Parcelable {

    public fun toJson(): JSONObject {
        val jsonStr = adapter.toJson(this)
        return JSONObject(jsonStr)
    }

    public companion object {
        private val adapter = Notificare.moshi.adapter(NotificareUser::class.java)

        public fun fromJson(json: JSONObject): NotificareUser {
            val jsonStr = json.toString()
            return requireNotNull(adapter.fromJson(jsonStr))
        }
    }
}
