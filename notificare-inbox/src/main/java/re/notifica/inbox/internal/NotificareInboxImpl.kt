package re.notifica.inbox.internal

import androidx.annotation.Keep
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import java.util.Date
import java.util.SortedSet
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import re.notifica.Notificare
import re.notifica.NotificareApplicationUnavailableException
import re.notifica.NotificareCallback
import re.notifica.NotificareDeviceUnavailableException
import re.notifica.NotificareNotReadyException
import re.notifica.NotificareServiceUnavailableException
import re.notifica.inbox.NotificareInbox
import re.notifica.inbox.internal.database.InboxDatabase
import re.notifica.inbox.internal.database.entities.InboxItemEntity
import re.notifica.inbox.internal.network.push.InboxResponse
import re.notifica.inbox.internal.workers.ExpireItemWorker
import re.notifica.inbox.models.NotificareInboxItem
import re.notifica.internal.NotificareModule
import re.notifica.utilities.coroutines.notificareCoroutineScope
import re.notifica.utilities.coroutines.toCallbackFunction
import re.notifica.internal.network.NetworkException
import re.notifica.internal.network.request.NotificareRequest
import re.notifica.ktx.device
import re.notifica.ktx.events
import re.notifica.models.NotificareApplication
import re.notifica.models.NotificareNotification

@Keep
internal object NotificareInboxImpl : NotificareModule(), NotificareInbox {
    internal lateinit var database: InboxDatabase

    private var itemsCollectJob: Job? = null
    private val itemsFlowCollector = FlowCollector<List<InboxItemEntity>> { entities ->
        logger.debug("Received an inbox flow update.")

        // Replace the cached copy with the new entities.
        cachedEntities.clear()
        cachedEntities.addAll(entities)

        // Update (observable) items.
        val items = items
        _itemsStream.value = items

        // Update (observable) badge.
        badge = items.count { !it.opened }
        _badgeStream.value = badge

        // Schedule the expiration task.
        scheduleExpirationTask(entities)
    }

    private val cachedEntities: MutableList<InboxItemEntity> = mutableListOf()

    private val _itemsStream = MutableStateFlow<SortedSet<NotificareInboxItem>>(sortedSetOf())
    private val _badgeStream = MutableStateFlow(0)

    // region Notificare Module

    override fun configure() {
        logger.hasDebugLoggingEnabled = checkNotNull(Notificare.options).debugLoggingEnabled

        database = InboxDatabase.create(Notificare.requireContext())

        reloadLiveItems()
    }

    override suspend fun clearStorage() {
        database.inbox().clear()
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
                logger.warning("Notificare application is not yet available.")
                return sortedSetOf()
            }

            if (application.inboxConfig?.useInbox != true) {
                logger.warning("Notificare inbox functionality is not enabled.")
                return sortedSetOf()
            }

            return cachedEntities
                .map { it.toInboxItem() }
                .toSortedSet(
                    compareByDescending<NotificareInboxItem> { it.time }
                        .thenBy { it.opened }
                )
        }

    override var badge: Int = 0
        get() {
            val application = Notificare.application ?: run {
                logger.warning("Notificare application is not yet available.")
                return 0
            }

            if (application.inboxConfig?.useInbox != true) {
                logger.warning("Notificare inbox functionality is not enabled.")
                return 0
            }

            if (application.inboxConfig?.autoBadge != true) {
                logger.warning("Notificare auto badge functionality is not enabled.")
                return 0
            }

            return field
        }

    override val observableItems: LiveData<SortedSet<NotificareInboxItem>> = _itemsStream.asLiveData()
    override val itemsStream: StateFlow<SortedSet<NotificareInboxItem>> = _itemsStream

    override val observableBadge: LiveData<Int> = _badgeStream.asLiveData()
    override val badgeStream: StateFlow<Int> = _badgeStream

    override fun refresh() {
        val application = Notificare.application ?: run {
            logger.warning("Notificare application not yet available.")
            return
        }

        if (application.inboxConfig?.useInbox != true) {
            logger.warning("Notificare inbox functionality is not enabled.")
            return
        }

        notificareCoroutineScope.launch {
            try {
                reloadInbox()
            } catch (e: Exception) {
                logger.error("Failed to refresh the inbox.", e)
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
                logger.warning("Unable to find item '${item.id}' in the local database.")
            } else {
                entity.notification = notification

                try {
                    database.inbox().update(entity)
                } catch (e: Exception) {
                    logger.error("Failed to update the item in the local database.", e)
                    throw e
                }
            }
        }

        // Mark the item as read & send a notification open event.
        markAsRead(item)

        return@withContext notification
    }

    override fun open(item: NotificareInboxItem, callback: NotificareCallback<NotificareNotification>): Unit =
        toCallbackFunction(NotificareInboxImpl::open)(item, callback::onSuccess, callback::onFailure)

    override suspend fun markAsRead(item: NotificareInboxItem): Unit = withContext(Dispatchers.IO) {
        checkPrerequisites()

        // Send an event to mark the notification as read in the remote inbox.
        Notificare.events().logNotificationOpen(item.notification.id)

        // Mark the item as read in the local inbox.
        val entity = cachedEntities.find { it.id == item.id }
        if (entity == null) {
            logger.warning("Unable to find item '${item.id}' in the local database.")
        } else {
            entity.opened = true
            database.inbox().update(entity)
        }

        // No need to keep the item in the notification center.
        Notificare.removeNotificationFromNotificationCenter(item.notification)
    }

    override fun markAsRead(item: NotificareInboxItem, callback: NotificareCallback<Unit>): Unit =
        toCallbackFunction(NotificareInboxImpl::markAsRead)(item, callback::onSuccess, callback::onFailure)

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
        toCallbackFunction(NotificareInboxImpl::markAllAsRead)(callback::onSuccess, callback::onFailure)

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
        toCallbackFunction(NotificareInboxImpl::remove)(item, callback::onSuccess, callback::onFailure)

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
        toCallbackFunction(NotificareInboxImpl::clear)(callback::onSuccess, callback::onFailure)

    // endregion

    @Throws
    private fun checkPrerequisites() {
        if (!Notificare.isReady) {
            logger.warning("Notificare is not ready yet.")
            throw NotificareNotReadyException()
        }

        val application = Notificare.application ?: run {
            logger.warning("Notificare application is not yet available.")
            throw NotificareApplicationUnavailableException()
        }

        if (application.services[NotificareApplication.ServiceKeys.INBOX] != true) {
            logger.warning("Notificare inbox functionality is not enabled.")
            throw NotificareServiceUnavailableException(service = NotificareApplication.ServiceKeys.INBOX)
        }

        if (application.inboxConfig?.useInbox != true) {
            logger.warning("Notificare inbox functionality is not enabled.")
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
        itemsCollectJob?.cancel()

        database.inbox().getItemsStream().also { itemsStream ->
            itemsCollectJob = notificareCoroutineScope.launch {
                itemsStream
                    .catch { e ->
                        logger.error("Failed to collect inbox items entities flow from the database.", e)
                    }
                    .collect(itemsFlowCollector)
            }
        }
    }

    private suspend fun addItem(entity: InboxItemEntity): Unit = withContext(Dispatchers.IO) {
        database.inbox().insert(entity)
    }

    private suspend fun sync(): Unit = withContext(Dispatchers.IO) {
        val device = Notificare.device().currentDevice ?: run {
            logger.warning("No device registered yet. Skipping...")
            return@withContext
        }

        val mostRecentItem = database.inbox().findMostRecent() ?: run {
            logger.debug("The local inbox contains no items. Checking remotely.")
            reloadInbox()
            return@withContext
        }

        try {
            logger.debug("Checking if the inbox has been modified since ${mostRecentItem.time}.")
            NotificareRequest.Builder()
                .get("/notification/inbox/fordevice/${device.id}")
                .query("ifModifiedSince", mostRecentItem.time.time.toString())
                .responseDecodable(InboxResponse::class)

            logger.info("The inbox has been modified. Performing a full sync.")
            reloadInbox()
        } catch (e: Exception) {
            if (e is NetworkException.ValidationException && e.response.code == 304) {
                logger.debug("The inbox has not been modified. Proceeding with locally stored data.")
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
            logger.error("Failed to clear the local inbox.", e)
            throw e
        }
    }

    private suspend fun requestRemoteInboxItems(step: Int = 0): Unit = withContext(Dispatchers.IO) {
        val device = Notificare.device().currentDevice ?: run {
            logger.warning("Notificare has not been configured yet.")
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
            logger.debug("Loading more inbox items.")
            requestRemoteInboxItems(step = step + 1)
        } else {
            logger.debug("Done loading inbox items.")
        }
    }

    private fun scheduleExpirationTask(items: List<InboxItemEntity>) {
        val now = Date()

        val earliestExpirationItem = items
            .filter { it.expires != null && it.expires.after(now) }
            .minByOrNull { checkNotNull(it.expires) }
            ?: return

        val initialDelayMilliseconds = checkNotNull(earliestExpirationItem.expires).time - now.time
        logger.debug("Scheduling the next expiration in '$initialDelayMilliseconds' milliseconds.")

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
        logger.debug("Removing all messages from the notification center.")
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
