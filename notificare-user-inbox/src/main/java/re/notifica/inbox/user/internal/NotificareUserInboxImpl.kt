package re.notifica.inbox.user.internal

import androidx.annotation.Keep
import com.squareup.moshi.Moshi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import re.notifica.Notificare
import re.notifica.NotificareApplicationUnavailableException
import re.notifica.NotificareCallback
import re.notifica.NotificareDeviceUnavailableException
import re.notifica.NotificareNotConfiguredException
import re.notifica.NotificareNotReadyException
import re.notifica.NotificareServiceUnavailableException
import re.notifica.inbox.user.NotificareUserInbox
import re.notifica.inbox.user.internal.moshi.UserInboxResponseAdapter
import re.notifica.inbox.user.models.NotificareUserInboxItem
import re.notifica.inbox.user.models.NotificareUserInboxResponse
import re.notifica.internal.NotificareLogger
import re.notifica.internal.NotificareModule
import re.notifica.internal.ktx.toCallbackFunction
import re.notifica.internal.moshi
import re.notifica.internal.network.push.NotificationResponse
import re.notifica.internal.network.request.NotificareRequest
import re.notifica.ktx.device
import re.notifica.ktx.events
import re.notifica.models.NotificareApplication
import re.notifica.models.NotificareNotification

@Keep
internal object NotificareUserInboxImpl : NotificareModule(), NotificareUserInbox {

    // region Notificare Module

    override fun moshi(builder: Moshi.Builder) {
        builder.add(UserInboxResponseAdapter())
    }

    // endregion

    // region Notificare User Inbox

    override fun parseResponse(json: String): NotificareUserInboxResponse {
        val adapter = Notificare.moshi.adapter(NotificareUserInboxResponse::class.java)
        return requireNotNull(adapter.nonNull().fromJson(json))
    }

    override fun parseResponse(json: JSONObject): NotificareUserInboxResponse {
        return parseResponse(json.toString())
    }

    override suspend fun open(item: NotificareUserInboxItem): NotificareNotification = withContext(Dispatchers.IO) {
        checkPrerequisites()

        // User inbox items are always partial.
        val notification = fetchUserInboxNotification(item)

        // Mark the item as read & send a notification open event.
        markAsRead(item)

        return@withContext notification
    }

    override fun open(
        item: NotificareUserInboxItem,
        callback: NotificareCallback<NotificareNotification>
    ): Unit = toCallbackFunction(::open)(item, callback)

    override suspend fun markAsRead(item: NotificareUserInboxItem): Unit = withContext(Dispatchers.IO) {
        checkPrerequisites()

        Notificare.events().logNotificationOpen(item.notification.id)
    }

    override fun markAsRead(
        item: NotificareUserInboxItem,
        callback: NotificareCallback<Unit>
    ): Unit = toCallbackFunction(::markAsRead)(item, callback)

    override suspend fun remove(item: NotificareUserInboxItem): Unit = withContext(Dispatchers.IO) {
        checkPrerequisites()

        removeUserInboxItem(item)
    }

    override fun remove(
        item: NotificareUserInboxItem,
        callback: NotificareCallback<Unit>
    ): Unit = toCallbackFunction(::remove)(item, callback)

    // endregion

    @Throws
    private fun checkPrerequisites() {
        if (!Notificare.isReady) {
            NotificareLogger.warning("Notificare is not ready yet.")
            throw NotificareNotReadyException()
        }

        val application = Notificare.application ?: run {
            NotificareLogger.warning("Notificare application is not yet available.")
            throw NotificareApplicationUnavailableException()
        }

        if (application.services[NotificareApplication.ServiceKeys.INBOX] != true) {
            NotificareLogger.warning("Notificare inbox functionality is not enabled.")
            throw NotificareServiceUnavailableException(service = NotificareApplication.ServiceKeys.INBOX)
        }

        if (application.inboxConfig?.useInbox != true) {
            NotificareLogger.warning("Notificare inbox functionality is not enabled.")
            throw NotificareServiceUnavailableException(service = NotificareApplication.ServiceKeys.INBOX)
        }

        if (application.inboxConfig?.useUserInbox != true) {
            NotificareLogger.warning("Notificare user inbox functionality is not enabled.")
            throw NotificareServiceUnavailableException(service = NotificareApplication.ServiceKeys.INBOX)
        }
    }

    private suspend fun fetchUserInboxNotification(
        item: NotificareUserInboxItem
    ): NotificareNotification = withContext(Dispatchers.IO) {
        if (!Notificare.isConfigured) throw NotificareNotConfiguredException()

        val device = Notificare.device().currentDevice
            ?: throw NotificareDeviceUnavailableException()

        NotificareRequest.Builder()
            .get("/notification/userinbox/${item.id}/fordevice/${device.id}")
            .responseDecodable(NotificationResponse::class)
            .notification
            .toModel()
    }

    private suspend fun removeUserInboxItem(item: NotificareUserInboxItem): Unit = withContext(Dispatchers.IO) {
        if (!Notificare.isConfigured) throw NotificareNotConfiguredException()

        val device = Notificare.device().currentDevice
            ?: throw NotificareDeviceUnavailableException()

        NotificareRequest.Builder()
            .delete("/notification/userinbox/${item.id}/fordevice/${device.id}", null)
            .response()
    }
}
