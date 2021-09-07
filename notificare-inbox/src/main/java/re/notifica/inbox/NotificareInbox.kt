package re.notifica.inbox

import android.os.Handler
import android.os.Looper
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.work.*
import kotlinx.coroutines.*
import re.notifica.Notificare
import re.notifica.NotificareCallback
import re.notifica.NotificareException
import re.notifica.inbox.internal.database.InboxDatabase
import re.notifica.inbox.internal.database.entities.InboxItemEntity
import re.notifica.inbox.internal.network.push.InboxResponse
import re.notifica.inbox.internal.workers.ExpireItemWorker
import re.notifica.inbox.models.NotificareInboxItem
import re.notifica.internal.NotificareLogger
import re.notifica.internal.network.NetworkException
import re.notifica.internal.network.request.NotificareRequest
import re.notifica.models.NotificareNotification
import re.notifica.modules.NotificareModule
import java.util.*
import java.util.concurrent.TimeUnit

public object NotificareInbox : NotificareModule() {

    public const val SDK_VERSION: String = BuildConfig.SDK_VERSION

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

        // Update observable items.
        val visibleItems = visibleItems
        _observableItems.postValue(visibleItems)

        // Update (observable) badge.
        _badge = visibleItems.count { !it.opened }
        _observableBadge.postValue(_badge)

        // Schedule the expiration task.
        scheduleExpirationTask(entities)
    }

    private val cachedEntities: SortedSet<InboxItemEntity> = sortedSetOf(
        Comparator { lhs, rhs -> rhs.time.compareTo(lhs.time) }
    )

    public val items: SortedSet<NotificareInboxItem>
        get() {
            val application = Notificare.application ?: run {
                NotificareLogger.warning("Notificare application is not yet available.")
                return sortedSetOf()
            }

            if (application.inboxConfig?.useInbox != true) {
                NotificareLogger.warning("Notificare inbox functionality is not enabled.")
                return sortedSetOf()
            }

            return visibleItems
        }

    private var _badge: Int = 0
    public val badge: Int
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

            return _badge
        }

    private val _observableItems = MutableLiveData<SortedSet<NotificareInboxItem>>(sortedSetOf())
    public val observableItems: LiveData<SortedSet<NotificareInboxItem>>
        get() = _observableItems

    private val _observableBadge = MutableLiveData(0)
    public val observableBadge: LiveData<Int>
        get() = _observableBadge

    private val visibleItems: SortedSet<NotificareInboxItem>
        get() {
            return cachedEntities
                .map { it.toInboxItem() }
                .filter { it.visible && !it.expired }
                .toSortedSet { lhs, rhs -> rhs.time.compareTo(lhs.time) }
        }

    override fun configure() {
        database = InboxDatabase.create(Notificare.requireContext())

        reloadLiveItems()
    }

    override suspend fun launch() {
        sync()
    }

    override suspend fun unlaunch() {
        clearLocalInbox()
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

    public fun refresh() {
        val application = Notificare.application ?: run {
            NotificareLogger.warning("Notificare application not yet available.")
            return
        }

        if (application.inboxConfig?.useInbox != true) {
            NotificareLogger.warning("Notificare inbox functionality is not enabled.")
            return
        }

        GlobalScope.launch {
            try {
                reloadInbox()
            } catch (e: Exception) {
                NotificareLogger.error("Failed to refresh the inbox.", e)
            }
        }
    }

    public suspend fun open(item: NotificareInboxItem): NotificareNotification = withContext(Dispatchers.IO) {
        val application = Notificare.application ?: run {
            NotificareLogger.warning("Notificare application not yet available.")
            throw NotificareException.NotReady()
        }

        if (application.inboxConfig?.useInbox != true) {
            NotificareLogger.warning("Notificare inbox functionality is not enabled.")
            throw NotificareInboxException.InboxUnavailable()
        }

        // Remove the item from the notification center.
        Notificare.removeNotificationFromNotificationCenter(item.notification)

        if (item.notification.partial) {
            item._notification = Notificare.fetchNotification(item.notification.id)

            try {
                database.inbox().update(InboxItemEntity.from(item))
            } catch (e: Exception) {
                NotificareLogger.error("Failed to update the item in the local database.", e)
                throw e
            }
        }

        // Mark the item as read & send a notification open event.
        markAsRead(item)

        return@withContext item.notification
    }

    public fun open(item: NotificareInboxItem, callback: NotificareCallback<NotificareNotification>) {
        GlobalScope.launch {
            try {
                val notification = open(item)
                callback.onSuccess(notification)
            } catch (e: Exception) {
                callback.onFailure(e)
            }
        }
    }

    public suspend fun markAsRead(item: NotificareInboxItem): Unit = withContext(Dispatchers.IO) {
        val application = Notificare.application ?: run {
            NotificareLogger.warning("Notificare application not yet available.")
            throw NotificareException.NotReady()
        }

        if (application.inboxConfig?.useInbox != true) {
            NotificareLogger.warning("Notificare inbox functionality is not enabled.")
            throw NotificareInboxException.InboxUnavailable()
        }

        // Send an event to mark the notification as read in the remote inbox.
        Notificare.eventsManager.logNotificationOpened(item.notification.id)

        // Mark the item as read in the local inbox.
        item._opened = true
        database.inbox().update(InboxItemEntity.from(item))

        // No need to keep the item in the notification center.
        Notificare.removeNotificationFromNotificationCenter(item.notification)
    }

    public fun markAsRead(item: NotificareInboxItem, callback: NotificareCallback<Unit>) {
        GlobalScope.launch {
            try {
                markAsRead(item)
                callback.onSuccess(Unit)
            } catch (e: Exception) {
                callback.onFailure(e)
            }
        }
    }

    public suspend fun markAllAsRead(): Unit = withContext(Dispatchers.IO) {
        val application = Notificare.application ?: run {
            NotificareLogger.warning("Notificare application not yet available.")
            throw NotificareException.NotReady()
        }

        val device = Notificare.deviceManager.currentDevice ?: run {
            NotificareLogger.warning("No device registered yet.")
            throw NotificareException.NotReady()
        }

        if (application.inboxConfig?.useInbox != true) {
            NotificareLogger.warning("Notificare inbox functionality is not enabled.")
            throw NotificareInboxException.InboxUnavailable()
        }

        // Mark all the items in the remote inbox.
        NotificareRequest.Builder()
            .put("/notification/inbox/fordevice/${device.id}", null)
            .response()

        // Mark all the items in the local inbox.
        database.inbox().updateAllAsRead()

        // Remove all items from the notification center.
        clearNotificationCenter()
    }

    public fun markAllAsRead(callback: NotificareCallback<Unit>) {
        GlobalScope.launch {
            try {
                markAllAsRead()
                callback.onSuccess(Unit)
            } catch (e: Exception) {
                callback.onFailure(e)
            }
        }
    }

    public suspend fun remove(item: NotificareInboxItem): Unit = withContext(Dispatchers.IO) {
        val application = Notificare.application ?: run {
            NotificareLogger.warning("Notificare application not yet available.")
            throw NotificareException.NotReady()
        }

        if (application.inboxConfig?.useInbox != true) {
            NotificareLogger.warning("Notificare inbox functionality is not enabled.")
            throw NotificareInboxException.InboxUnavailable()
        }

        // Remove the item from the API.
        NotificareRequest.Builder()
            .delete("/notification/inbox/${item.id}", null)
            .response()

        // Remove the item from the local inbox.
        database.inbox().remove(item.id)

        // Remove the item from the notification center.
        Notificare.removeNotificationFromNotificationCenter(item.notification)
    }

    public fun remove(item: NotificareInboxItem, callback: NotificareCallback<Unit>) {
        GlobalScope.launch {
            try {
                remove(item)
                callback.onSuccess(Unit)
            } catch (e: Exception) {
                callback.onFailure(e)
            }
        }
    }

    public suspend fun clear(): Unit = withContext(Dispatchers.IO) {
        val application = Notificare.application ?: run {
            NotificareLogger.warning("Notificare application not yet available.")
            throw NotificareException.NotReady()
        }

        val device = Notificare.deviceManager.currentDevice ?: run {
            NotificareLogger.warning("No device registered yet.")
            throw NotificareException.NotReady()
        }

        if (application.inboxConfig?.useInbox != true) {
            NotificareLogger.warning("Notificare inbox functionality is not enabled.")
            throw NotificareInboxException.InboxUnavailable()
        }

        NotificareRequest.Builder()
            .delete("/notification/inbox/fordevice/${device.id}", null)
            .response()

        clearLocalInbox()
        clearNotificationCenter()
    }

    public fun clear(callback: NotificareCallback<Unit>) {
        GlobalScope.launch {
            try {
                clear()
                callback.onSuccess(Unit)
            } catch (e: Exception) {
                callback.onFailure(e)
            }
        }
    }

    internal suspend fun addItem(item: NotificareInboxItem): Unit = withContext(Dispatchers.IO) {
        val entity = InboxItemEntity.from(item)
        addItem(entity)
    }

    internal fun handleExpiredItem(id: String) {
        val item = items.find { it.id == id }

        if (item != null) {
            Notificare.removeNotificationFromNotificationCenter(item.notification)
        }

        reloadLiveItems()
    }

    private suspend fun addItem(entity: InboxItemEntity): Unit = withContext(Dispatchers.IO) {
        database.inbox().insert(entity)
    }

    private suspend fun sync(): Unit = withContext(Dispatchers.IO) {
        val device = Notificare.deviceManager.currentDevice ?: run {
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
        val device = Notificare.deviceManager.currentDevice ?: run {
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

        if (earliestExpirationItem == null) {
            NotificareLogger.debug("Unable to determine the earliest item to expire. Skipping task...")
            return
        }

        val initialDelaySeconds = (checkNotNull(earliestExpirationItem.expires).time - now.time) / 1000L
        NotificareLogger.debug("Scheduling the next expiration in '$initialDelaySeconds' seconds.")

        val task = OneTimeWorkRequestBuilder<ExpireItemWorker>()
            .setInitialDelay(initialDelaySeconds, TimeUnit.SECONDS)
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
}
