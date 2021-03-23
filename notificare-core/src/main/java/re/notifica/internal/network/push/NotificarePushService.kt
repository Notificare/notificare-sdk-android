package re.notifica.internal.network.push

import re.notifica.models.NotificareDoNotDisturb
import re.notifica.models.NotificareEvent
import re.notifica.models.NotificareUserData
import retrofit2.http.*

internal interface NotificarePushService {

    @GET("/application/info")
    suspend fun fetchApplication(): ApplicationResponse

    @POST("/device")
    suspend fun createDevice(
        @Body registration: DeviceRegistrationPayload
    )

    @PUT("/device/{id}")
    suspend fun updateDevice(
        @Path("id") deviceId: String,
        @Body payload: DeviceUpdateLanguagePayload
    )

    @PUT("/device/{id}")
    suspend fun updateDevice(
        @Path("id") deviceId: String,
        @Body payload: DeviceUpdateTimeZonePayload
    )

    @PUT("/device/{id}")
    suspend fun updateDevice(
        @Path("id") deviceId: String,
        @Body payload: DeviceUpdateNotificationSettingsPayload
    )

    // TODO update device calls

    @GET("/device/{id}/tags")
    suspend fun getDeviceTags(
        @Path("id") deviceId: String
    ): DeviceTagsResponse

    @PUT("/device/{id}/addtags")
    suspend fun addDeviceTags(
        @Path("id") deviceId: String,
        @Body payload: DeviceTagsPayload
    )

    @PUT("/device/{id}/removetags")
    suspend fun removeDeviceTags(
        @Path("id") deviceId: String,
        @Body payload: DeviceTagsPayload
    )

    @PUT("/device/{id}/cleartags")
    suspend fun clearDeviceTags(
        @Path("id") deviceId: String
    )

    @GET("/device/{id}/dnd")
    suspend fun getDeviceDoNotDisturb(
        @Path("id") deviceId: String
    ): DeviceDoNotDisturbResponse

    @PUT("/device/{id}/dnd")
    suspend fun updateDeviceDoNotDisturb(
        @Path("id") deviceId: String,
        @Body payload: NotificareDoNotDisturb
    )

    @PUT("/device/{id}/cleardnd")
    suspend fun clearDeviceDoNotDisturb(
        @Path("id") deviceId: String
    )

    @GET("/device/{id}/userdata")
    suspend fun getDeviceUserData(
        @Path("id") deviceId: String
    ): DeviceUserDataResponse

    @PUT("/device/{id}/userdata")
    suspend fun updateDeviceUserData(
        @Path("id") deviceId: String,
        @Body payload: NotificareUserData
    )

    @POST("/event")
    suspend fun createEvent(@Body event: NotificareEvent)

    @GET("/notification/{id}")
    suspend fun fetchNotification(
        @Path("id") notificationId: String,
    ): NotificationResponse
}
