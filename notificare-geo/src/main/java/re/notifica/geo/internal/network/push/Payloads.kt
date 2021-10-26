package re.notifica.geo.internal.network.push

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class UpdateDeviceLocationPayload(
    val latitude: Double?,
    val longitude: Double?,
    val altitude: Double?,
    val locationAccuracy: Double?,
    val speed: Double?,
    val course: Double?,
    val country: String?,
    // val floor: Int?,
    val locationServicesAuthStatus: String?,
    val locationServicesAccuracyAuth: String?,
)

@JsonClass(generateAdapter = true)
internal data class UpdateBluetoothPayload(
    val bluetoothEnabled: Boolean,
)

@JsonClass(generateAdapter = true)
internal data class RegionTriggerPayload(
    val deviceID: String,
    val region: String,
)

@JsonClass(generateAdapter = true)
internal data class BeaconTriggerPayload(
    val deviceID: String,
    val beacon: String,
)
