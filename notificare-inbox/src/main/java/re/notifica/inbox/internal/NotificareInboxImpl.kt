package re.notifica.inbox.internal

import android.os.Handler
import android.os.Looper
import androidx.annotation.Keep
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.work.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import re.notifica.*
import re.notifica.inbox.NotificareInbox
import re.notifica.inbox.internal.database.InboxDatabase
import re.notifica.inbox.internal.database.entities.InboxItemEntity
import re.notifica.inbox.internal.network.push.InboxResponse
import re.notifica.inbox.internal.workers.ExpireItemWorker
import re.notifica.inbox.models.NotificareInboxItem
import re.notifica.internal.NotificareLogger
import re.notifica.internal.NotificareModule
import re.notifica.internal.ktx.coroutineScope
import re.notifica.internal.ktx.toCallbackFunction
import re.notifica.internal.network.NetworkException
import re.notifica.internal.network.request.NotificareRequest
import re.notifica.ktx.device
import re.notifica.ktx.events
import re.notifica.models.NotificareApplication
import re.notifica.models.NotificareNotification
import java.util.*
import java.util.concurrent.TimeUnit

@Keep
internal object NotificareInboxImpl : NotificareModule(), NotificareInbox {

    internal lateinit var database: InboxDatabase

    private var liveItems: LiveData<List<InboxItemEntity>>? = null
    private val liveItemsObserver = Observer<List<InboxItemEntity>> { entities ->
        NotificareLogger.debug("Received an inbox live data update.")
        if (entities == null) {
            NotificareLogger.debug("Inbox live data update was null. Skipping...")
            return@Observer
        }

        // Replace the cached copy with the new entities.
        cachedEntities.clear()
        cachedEntities.addAll(entities)

        // Update (observable) items.
        val items = items
        _observableItems.postValue(items)

        // Update (observable) badge.
        badge = items.count { !it.opened }
        _observableBadge.postValue(badge)

        // Schedule the expiration task.
        scheduleExpirationTask(entities)
    }

    private val cachedEntities: SortedSet<InboxItemEntity> = sortedSetOf(
        Comparator { lhs, rhs -> rhs.time.compareTo(lhs.time) }
    )

    private val _observableItems = MutableLiveData<SortedSet<NotificareInboxItem>>(sortedSetOf())
    private val _observableBadge = MutableLiveData(0)

    // region Notificare Module

    override fun configure() {
        database = InboxDatabase.create(Notificare.requireContext())

        reloadLiveItems()
    }

    override suspend fun launch() {
        sync()
    }

    override suspend fun unlaunch() {
        clearLocalInbox()
        clearNotificationCenter()
        clearRemoteInbox()
    }

    // endregion

    // region Notificare Inbox

    override val items: SortedSet<NotificareInboxItem>
        get() {
            val application = Notificare.application ?: run {
                NotificareLogger.warning("Notificare application is not yet available.")
                return sortedSetOf()
            }

            if (application.inboxConfig?.useInbox != true) {
                NotificareLogger.warning("Notificare inbox functionality is not enabled.")
                return sortedSetOf()
            }

            return cachedEntities
                .map { it.toInboxItem() }
                .toSortedSet { lhs, rhs -> rhs.time.compareTo(lhs.time) }
        }

    override var badge: Int = 0
        get() {
            val application = Notificare.application ?: run {
                NotificareLogger.warning("Notificare application is not yet available.")
                return 0
            }

            if (application.inboxConfig?.useInbox != true) {
                NotificareLogger.warning("Notificare inbox functionality is not enabled.")
                return 0
            }

            if (application.inboxConfig?.autoBadge != true) {
                NotificareLogger.warning("Notificare auto badge functionality is not enabled.")
                return 0
            }

            return field
        }

    override val observableItems: LiveData<SortedSet<NotificareInboxItem>> = _observableItems
    override val observableBadge: LiveData<Int> = _observableBadge

    override fun refresh() {
        val application = Notificare.application ?: run {
            NotificareLogger.warning("Notificare application not yet available.")
            return
        }

        if (application.inboxConfig?.useInbox != true) {
            NotificareLogger.warning("Notificare inbox functionality is not enabled.")
            return
        }

        Notificare.coroutineScope.launch {
            try {
                reloadInbox()
            } catch (e: Exception) {
                NotificareLogger.error("Failed to refresh the inbox.", e)
            }
        }
    }

    override suspend fun open(item: NotificareInboxItem): NotificareNotification = withContext(Dispatchers.IO) {
        checkPrerequisites()

        val notification =
            if (item.notification.partial) Notificare.fetchNotification(item.id)
            else item.notification

        if (item.notification.partial) {
            val entity = cachedEntities.find { it.id == item.id }
            if (entity == null) {
                NotificareLogger.warning("Unable to find item '${item.id}' in the local database.")
            } else {
                entity.notification = notification

                try {
                    database.inbox().update(entity)
                } catch (e: Exception) {
                    NotificareLogger.error("Failed to update the item in the local database.", e)
                    throw e
                }
            }
        }

        // Mark the item as read & send a notification open event.
        markAsRead(item)

        return@withContext notification
    }

    override fun open(item: NotificareInboxItem, callback: NotificareCallback<NotificareNotification>): Unit =
        toCallbackFunction(NotificareInboxImpl::open)(item, callback)

    override suspend fun markAsRead(item: NotificareInboxItem): Unit = withContext(Dispatchers.IO) {
        checkPrerequisites()

        // Send an event to mark the notification as read in the remote inbox.
        Notificare.events().logNotificationOpen(item.notification.id)

        // Mark the item as read in the local inbox.
        val entity = cachedEntities.find { it.id == item.id }
        if (entity == null) {
            NotificareLogger.warning("Unable to find item '${item.id}' in the local database.")
        } else {
            entity.opened = true
            database.inbox().update(entity)
        }

        // No need to keep the item in the notification center.
        Notificare.removeNotificationFromNotificationCenter(item.notification)
    }

    override fun markAsRead(item: NotificareInboxItem, callback: NotificareCallback<Unit>): Unit =
        toCallbackFunction(NotificareInboxImpl::markAsRead)(item, callback)

    override suspend fun markAllAsRead(): Unit = withContext(Dispatchers.IO) {
        checkPrerequisites()

        val device = checkNotNull(Notificare.device().currentDevice)

        // Mark all the items in the remote inbox.
        NotificareRequest.Builder()
            .put("/notification/inbox/fordevice/${device.id}", null)
            .response()

        // Mark all the items in the local inbox.
        database.inbox().updateAllAsRead()

        // Remove all items from the notification center.
        clearNotificationCenter()
    }

    override fun markAllAsRead(callback: NotificareCallback<Unit>): Unit =
        toCallbackFunction(NotificareInboxImpl::markAllAsRead)(callback)

    override suspend fun remove(item: NotificareInboxItem): Unit = withContext(Dispatchers.IO) {
        checkPrerequisites()

        // Remove the item from the API.
        NotificareRequest.Builder()
            .delete("/notification/inbox/${item.id}", null)
            .response()

        // Remove the item from the local inbox.
        database.inbox().remove(item.id)

        // Remove the item from the notification center.
        Notificare.removeNotificationFromNotificationCenter(item.notification)
    }

    override fun remove(item: NotificareInboxItem, callback: NotificareCallback<Unit>): Unit =
        toCallbackFunction(NotificareInboxImpl::remove)(item, callback)

    override suspend fun clear(): Unit = withContext(Dispatchers.IO) {
        checkPrerequisites()

        val device = checkNotNull(Notificare.device().currentDevice)

        NotificareRequest.Builder()
            .delete("/notification/inbox/fordevice/${device.id}", null)
            .response()

        clearLocalInbox()
        clearNotificationCenter()
    }

    override fun clear(callback: NotificareCallback<Unit>): Unit =
        toCallbackFunction(NotificareInboxImpl::clear)(callback)

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
    }

    internal suspend fun addItem(item: NotificareInboxItem, visible: Boolean): Unit = withContext(Dispatchers.IO) {
        val entity = InboxItemEntity.from(item, visible)
        addItem(entity)
    }

    internal fun handleExpiredItem(id: String) {
        val item = cachedEntities.find { it.id == id }?.toInboxItem()

        if (item != null) {
            Notificare.removeNotificationFromNotificationCenter(item.notification)
        }

        reloadLiveItems()
    }

    private fun reloadLiveItems() {
        Handler(Looper.getMainLooper()).post {
            // Cancel previous observer if applicable.
            liveItems?.removeObserver(liveItemsObserver)

            // Continuously observe inbox's live items.
            liveItems = database.inbox().getLiveItems().also {
                it.observeForever(liveItemsObserver)
            }
        }
    }

    private suspend fun addItem(entity: InboxItemEntity): Unit = withContext(Dispatchers.IO) {
        database.inbox().insert(entity)
    }

    private suspend fun sync(): Unit = withContext(Dispatchers.IO) {
        val device = Notificare.device().currentDevice ?: run {
            NotificareLogger.warning("No device registered yet. Skipping...")
            return@withContext
        }

        val mostRecentItem = database.inbox().findMostRecent() ?: run {
            NotificareLogger.debug("The local inbox contains no items. Checking remotely.")
            reloadInbox()
            return@withContext
        }

        try {
            NotificareLogger.debug("Checking if the inbox has been modified since ${mostRecentItem.time}.")
            NotificareRequest.Builder()
                .get("/notification/inbox/fordevice/${device.id}")
                .query("ifModifiedSince", mostRecentItem.time.time.toString())
                .responseDecodable(InboxResponse::class)

            NotificareLogger.info("The inbox has been modified. Performing a full sync.")
            reloadInbox()
        } catch (e: Exception) {
            if (e is NetworkException.ValidationException && e.response.code == 304) {
                NotificareLogger.debug("The inbox has not been modified. Proceeding with locally stored data.")
            } else {
                // Rethrow the exception to be handled by the caller.
                throw e
            }
        }
    }

    private suspend fun reloadInbox(): Unit = withContext(Dispatchers.IO) {
        clearLocalInbox()
        requestRemoteInboxItems()
    }

    private suspend fun clearLocalInbox(): Unit = withContext(Dispatchers.IO) {
        try {
            database.inbox().clear()
        } catch (e: Exception) {
            NotificareLogger.error("Failed to clear the local inbox.", e)
            throw e
        }
    }

    private suspend fun requestRemoteInboxItems(step: Int = 0): Unit = withContext(Dispatchers.IO) {
        val device = Notificare.device().currentDevice ?: run {
            NotificareLogger.warning("Notificare has not been configured yet.")
            return@withContext
        }

        val response = NotificareRequest.Builder()
            .get("/notification/inbox/fordevice/${device.id}")
            .query("skip", (step * 100).toString())
            .query("limit", "100")
            .responseDecodable(InboxResponse::class)

        // Add all items to the database.
        response.inboxItems.forEach {
            val entity = InboxItemEntity.from(it)
            database.inbox().insert(entity)
        }

        if (response.count > (step + 1) * 100) {
            NotificareLogger.debug("Loading more inbox items.")
            requestRemoteInboxItems(step = step + 1)
        } else {
            NotificareLogger.debug("Done loading inbox items.")
        }
    }

    private fun scheduleExpirationTask(items: List<InboxItemEntity>) {
        val now = Date()

        val earliestExpirationItem = items
            .filter { it.expires != null && it.expires.after(now) }
            .minByOrNull { checkNotNull(it.expires) }
            ?: return

        val initialDelayMilliseconds = checkNotNull(earliestExpirationItem.expires).time - now.time
        NotificareLogger.debug("Scheduling the next expiration in '$initialDelayMilliseconds' milliseconds.")

        val task = OneTimeWorkRequestBuilder<ExpireItemWorker>()
            .setInitialDelay(initialDelayMilliseconds, TimeUnit.MILLISECONDS)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .setInputData(
                workDataOf(
                    ExpireItemWorker.PARAM_ITEM_ID to earliestExpirationItem.id,
                )
            )
            .build()

        WorkManager.getInstance(Notificare.requireContext())
            .enqueueUniqueWork("re.notifica.task.inbox.Expire", ExistingWorkPolicy.REPLACE, task)
    }

    private fun clearNotificationCenter() {
        NotificareLogger.debug("Removing all messages from the notification center.")
        NotificationManagerCompat.from(Notificare.requireContext())
            .cancelAll()
    }

    private suspend fun clearRemoteInbox() = withContext(Dispatchers.IO) {
        val device = Notificare.device().currentDevice
            ?: throw NotificareDeviceUnavailableException()

        NotificareRequest.Builder()
            .delete("/notification/inbox/fordevice/${device.id}", null)
            .response()
    }
}
