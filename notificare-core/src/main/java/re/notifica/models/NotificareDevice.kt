package re.notifica.models

import com.squareup.moshi.JsonClass
import java.util.*

typealias NotificareUserData = Map<String, String>

@JsonClass(generateAdapter = true)
data class NotificareDevice internal constructor(
    val deviceId: String,
    val userId: String?,
    val userName: String?,
    val timeZoneOffset: Float,
    val osVersion: String,
    val sdkVersion: String,
    val appVersion: String,
    val deviceString: String,
    val country: String?,
    val countryCode: String?, // TODO check this property: doesn't exist in v2
    val language: String,
    val region: String,
    val transport: NotificareTransport,
    var dnd: NotificareDoNotDisturb?,
    var userData: NotificareUserData?,
    val latitude: Float?,
    val longitude: Float?,
    val altitude: Float?,
    val accuracy: Float?,
    val floor: Float?,
    val speed: Float?,
    val course: Float?,
    val lastRegistered: Date,
    val locationServicesAuthStatus: String?,
    // val locationServicesAccuracyAuth: String?, // iOS
    // val registeredForNotifications: Boolean, // iOS
    // val allowedLocationServices: Boolean, // iOS
    val allowedUI: Boolean,
    // val backgroundAppRefresh: Boolean, // iOS
    val bluetoothEnabled: Boolean
)
