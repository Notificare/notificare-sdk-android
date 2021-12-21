package re.notifica.models

import android.os.Parcelable
import com.squareup.moshi.JsonClass
import kotlinx.parcelize.Parcelize
import org.json.JSONObject
import re.notifica.Notificare
import re.notifica.internal.moshi
import java.util.*

public typealias NotificareUserData = Map<String, String>

@Parcelize
@JsonClass(generateAdapter = true)
public data class NotificareDevice internal constructor(
    val id: String,
    val userId: String?,
    val userName: String?,
    val timeZoneOffset: Double,
    val osVersion: String,
    val sdkVersion: String,
    val appVersion: String,
    val deviceString: String,
    val language: String,
    val region: String,
    val transport: NotificareTransport,
    val dnd: NotificareDoNotDisturb?,
    val userData: NotificareUserData,
    val lastRegistered: Date,
) : Parcelable {

    public fun toJson(): JSONObject {
        val jsonStr = adapter.toJson(this)
        return JSONObject(jsonStr)
    }

    public companion object {
        private val adapter = Notificare.moshi.adapter(NotificareDevice::class.java)

        public fun fromJson(json: JSONObject): NotificareDevice {
            val jsonStr = json.toString()
            return requireNotNull(adapter.fromJson(jsonStr))
        }
    }
}
