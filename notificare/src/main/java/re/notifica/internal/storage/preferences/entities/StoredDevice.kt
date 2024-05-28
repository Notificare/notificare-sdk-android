package re.notifica.internal.storage.preferences.entities

import com.squareup.moshi.JsonClass
import re.notifica.models.NotificareDoNotDisturb
import re.notifica.models.NotificareUserData

@JsonClass(generateAdapter = true)
internal data class StoredDevice(
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
    val transport: String? = null,
    val dnd: NotificareDoNotDisturb?,
    val userData: NotificareUserData,
) {

    val isLongLived: Boolean
        get() = transport == null
}
