package re.notifica.models

import android.os.Parcelable
import com.squareup.moshi.JsonClass
import kotlinx.parcelize.Parcelize
import java.util.*

typealias NotificareUserData = Map<String, String>

@Parcelize
@JsonClass(generateAdapter = true)
data class NotificareDevice internal constructor(
    val id: String,
    val userId: String?,
    val userName: String?,
    val timeZoneOffset: Float,
    val osVersion: String,
    val sdkVersion: String,
    val appVersion: String,
    val deviceString: String,
    val language: String,
    val region: String,
    val transport: NotificareTransport,
    var dnd: NotificareDoNotDisturb?,
    var userData: NotificareUserData?,
    val lastRegistered: Date,
//    val location: Location?,
    // val locationServicesAuthStatus: String?,
    // val locationServicesAccuracyAuth: String?, // iOS
    // val registeredForNotifications: Boolean, // iOS
    // val allowedLocationServices: Boolean, // iOS
    var allowedUI: Boolean,
    // val backgroundAppRefresh: Boolean, // iOS
    val bluetoothEnabled: Boolean
) : Parcelable {

//    @Parcelize
//    @JsonClass(generateAdapter = true)
//    data class Location internal constructor(
//        val country: String?,
//        val latitude: Float?,
//        val longitude: Float?,
//        val altitude: Float?,
//        val accuracy: Float?,
//        val floor: Float?,
//        val speed: Float?,
//        val course: Float?,
//    ) : Parcelable
}
