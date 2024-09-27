package re.notifica.push.internal

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Parcelable
import androidx.annotation.Keep
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.RemoteInput
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ProcessLifecycleOwner
import com.google.firebase.messaging.RemoteMessage
import java.net.URLEncoder
import java.util.Date
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.json.JSONObject
import re.notifica.Notificare
import re.notifica.NotificareApplicationUnavailableException
import re.notifica.NotificareCallback
import re.notifica.NotificareDeviceUnavailableException
import re.notifica.NotificareNotReadyException
import re.notifica.NotificareServiceUnavailableException
import re.notifica.internal.NotificareModule
import re.notifica.utilities.coroutines.notificareCoroutineScope
import re.notifica.utilities.parcel.parcelable
import re.notifica.utilities.coroutines.toCallbackFunction
import re.notifica.internal.network.request.NotificareRequest
import re.notifica.ktx.device
import re.notifica.ktx.events
import re.notifica.models.NotificareApplication
import re.notifica.models.NotificareNotification
import re.notifica.push.NotificareInternalPush
import re.notifica.push.NotificarePush
import re.notifica.push.NotificarePushIntentReceiver
import re.notifica.push.NotificareSubscriptionUnavailable
import re.notifica.push.R
import re.notifica.push.automaticDefaultChannelEnabled
import re.notifica.push.defaultChannelId
import re.notifica.push.internal.network.push.CreateLiveActivityPayload
import re.notifica.push.internal.network.push.UpdateDeviceNotificationSettingsPayload
import re.notifica.push.internal.network.push.UpdateDeviceSubscriptionPayload
import re.notifica.push.ktx.INTENT_ACTION_ACTION_OPENED
import re.notifica.push.ktx.INTENT_ACTION_LIVE_ACTIVITY_UPDATE
import re.notifica.push.ktx.INTENT_ACTION_NOTIFICATION_OPENED
import re.notifica.push.ktx.INTENT_ACTION_NOTIFICATION_RECEIVED
import re.notifica.push.ktx.INTENT_ACTION_QUICK_RESPONSE
import re.notifica.push.ktx.INTENT_ACTION_REMOTE_MESSAGE_OPENED
import re.notifica.push.ktx.INTENT_ACTION_SUBSCRIPTION_CHANGED
import re.notifica.push.ktx.INTENT_ACTION_SYSTEM_NOTIFICATION_RECEIVED
import re.notifica.push.ktx.INTENT_ACTION_TOKEN_CHANGED
import re.notifica.push.ktx.INTENT_ACTION_UNKNOWN_NOTIFICATION_RECEIVED
import re.notifica.push.ktx.INTENT_EXTRA_DELIVERY_MECHANISM
import re.notifica.push.ktx.INTENT_EXTRA_LIVE_ACTIVITY_UPDATE
import re.notifica.push.ktx.INTENT_EXTRA_REMOTE_MESSAGE
import re.notifica.push.ktx.INTENT_EXTRA_SUBSCRIPTION
import re.notifica.push.ktx.INTENT_EXTRA_TEXT_RESPONSE
import re.notifica.push.ktx.INTENT_EXTRA_TOKEN
import re.notifica.push.ktx.logNotificationInfluenced
import re.notifica.push.ktx.logNotificationReceived
import re.notifica.push.ktx.logPushRegistration
import re.notifica.push.models.NotificareLiveActivityUpdate
import re.notifica.push.models.NotificareNotificationActionOpenedIntentResult
import re.notifica.push.models.NotificareNotificationDeliveryMechanism
import re.notifica.push.models.NotificareNotificationOpenedIntentResult
import re.notifica.push.models.NotificareNotificationRemoteMessage
import re.notifica.push.models.NotificarePushSubscription
import re.notifica.push.models.NotificareRemoteMessage
import re.notifica.push.models.NotificareSystemNotification
import re.notifica.push.models.NotificareSystemRemoteMessage
import re.notifica.push.models.NotificareTransport
import re.notifica.push.models.NotificareUnknownRemoteMessage
import re.notifica.push.notificationAccentColor
import re.notifica.push.notificationAutoCancel
import re.notifica.push.notificationLightsColor
import re.notifica.push.notificationLightsOff
import re.notifica.push.notificationLightsOn
import re.notifica.push.notificationSmallIcon
import re.notifica.utilities.image.loadBitmap

@Keep
internal object NotificarePushImpl : NotificareModule(), NotificarePush, NotificareInternalPush {

    internal const val DEFAULT_NOTIFICATION_CHANNEL_ID: String = "notificare_channel_default"

    private val notificationSequence = AtomicInteger()
    private val _observableSubscription = MutableLiveData<NotificarePushSubscription?>()
    private val _observableAllowedUI = MutableLiveData<Boolean>()

    internal lateinit var sharedPreferences: NotificareSharedPreferences
        private set

    internal var serviceManager: ServiceManager? = null
        private set

    // region Notificare Module

    override fun migrate(savedState: SharedPreferences, settings: SharedPreferences) {
        val preferences = NotificareSharedPreferences(Notificare.requireContext())

        if (savedState.contains("registeredDevice")) {
            val jsonStr = savedState.getString("registeredDevice", null)
            if (jsonStr != null) {
                try {
                    val json = JSONObject(jsonStr)

                    preferences.allowedUI = if (!json.isNull("allowedUI")) json.getBoolean("allowedUI") else false
                } catch (e: Exception) {
                    logger.error("Failed to migrate the 'allowedUI' property.", e)
                }
            }
        }

        if (settings.contains("notifications")) {
            val enabled = settings.getBoolean("notifications", false)
            sharedPreferences.remoteNotificationsEnabled = enabled

            if (enabled) {
                // Prevent the lib from sending the push registration event for existing devices.
                sharedPreferences.firstRegistration = false
            }
        }
    }

    override fun configure() {
        logger.hasDebugLoggingEnabled = checkNotNull(Notificare.options).debugLoggingEnabled

        sharedPreferences = NotificareSharedPreferences(Notificare.requireContext())
        serviceManager = ServiceManager.create()

        checkPushPermissions()

        if (checkNotNull(Notificare.options).automaticDefaultChannelEnabled) {
            logger.debug("Creating the default notifications channel.")
            createDefaultChannel()
        }

        if (!hasIntentFilter(Notificare.requireContext(), Notificare.INTENT_ACTION_REMOTE_MESSAGE_OPENED)) {
            @Suppress("detekt:MaxLineLength", "ktlint:standard:argument-list-wrapping")
            NotificareLogger.warning("Could not find an activity with the '${Notificare.INTENT_ACTION_REMOTE_MESSAGE_OPENED}' action. Notification opens won't work without handling the trampoline intent.")
        }

        // NOTE: The allowedUI is only gettable after the storage has been configured.
        _observableAllowedUI.postValue(allowedUI)

        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                onApplicationForeground()
            }
        })
    }

    override suspend fun clearStorage() {
        sharedPreferences.clear()

        _observableAllowedUI.postValue(allowedUI)
    }

    override suspend fun postLaunch() {
        if (sharedPreferences.remoteNotificationsEnabled) {
            logger.debug("Enabling remote notifications automatically.")
            updateDeviceSubscription()
        }
    }

    override suspend fun unlaunch() {
        sharedPreferences.remoteNotificationsEnabled = false
        sharedPreferences.firstRegistration = true

        transport = null
        subscription = null
        allowedUI = false
    }

    // endregion

    // region Notificare Push

    override var intentReceiver: Class<out NotificarePushIntentReceiver> = NotificarePushIntentReceiver::class.java

    override val hasRemoteNotificationsEnabled: Boolean
        get() {
            if (::sharedPreferences.isInitialized) {
                return sharedPreferences.remoteNotificationsEnabled
            }

            logger.warning("Calling this method requires Notificare to have been configured.")
            return false
        }

    override var transport: NotificareTransport?
        get() {
            if (::sharedPreferences.isInitialized) {
                return sharedPreferences.transport
            }

            logger.warning("Calling this method requires Notificare to have been configured.")
            return null
        }
        set(value) {
            if (::sharedPreferences.isInitialized) {
                sharedPreferences.transport = value
                return
            }

            logger.warning("Calling this method requires Notificare to have been configured.")
        }

    override var subscription: NotificarePushSubscription?
        get() {
            if (::sharedPreferences.isInitialized) {
                return sharedPreferences.subscription
            }

            logger.warning("Calling this method requires Notificare to have been configured.")
            return null
        }
        set(value) {
            if (::sharedPreferences.isInitialized) {
                sharedPreferences.subscription = value
                _observableSubscription.postValue(value)
                return
            }

            logger.warning("Calling this method requires Notificare to have been configured.")
        }

    override val observableSubscription: LiveData<NotificarePushSubscription?>
        get() = _observableSubscription

    override var allowedUI: Boolean
        get() {
            if (::sharedPreferences.isInitialized) {
                return sharedPreferences.allowedUI
            }

            logger.warning("Calling this method requires Notificare to have been configured.")
            return false
        }
        private set(value) {
            if (::sharedPreferences.isInitialized) {
                sharedPreferences.allowedUI = value
                _observableAllowedUI.postValue(value)
                return
            }

            logger.warning("Calling this method requires Notificare to have been configured.")
        }

    override val observableAllowedUI: LiveData<Boolean> = _observableAllowedUI

    override suspend fun enableRemoteNotifications(): Unit = withContext(Dispatchers.IO) {
        if (!Notificare.isReady) {
            logger.warning("Notificare is not ready yet.")
            throw NotificareNotReadyException()
        }

        val application = Notificare.application ?: run {
            logger.warning("Notificare is not ready yet.")
            throw NotificareApplicationUnavailableException()
        }

        if (application.services[NotificareApplication.ServiceKeys.GCM] != true) {
            logger.warning("Push notifications service is not enabled.")
            throw NotificareServiceUnavailableException(service = NotificareApplication.ServiceKeys.GCM)
        }

        // Keep track of the status in local storage.
        sharedPreferences.remoteNotificationsEnabled = true

        updateDeviceSubscription()
    }

    override fun enableRemoteNotifications(
        callback: NotificareCallback<Unit>
    ): Unit = toCallbackFunction(::enableRemoteNotifications)(callback::onSuccess, callback::onFailure)

    override suspend fun disableRemoteNotifications(): Unit = withContext(Dispatchers.IO) {
        // Keep track of the status in local storage.
        sharedPreferences.remoteNotificationsEnabled = false

        updateDeviceSubscription(
            transport = NotificareTransport.NOTIFICARE,
            token = null
        )

        logger.info("Unregistered from push provider.")
    }

    override fun disableRemoteNotifications(
        callback: NotificareCallback<Unit>
    ): Unit = toCallbackFunction(::disableRemoteNotifications)(callback::onSuccess, callback::onFailure)

    override fun isNotificareNotification(remoteMessage: RemoteMessage): Boolean {
        return remoteMessage.data["x-sender"] == "notificare"
    }

    override fun handleTrampolineIntent(intent: Intent): Boolean {
        if (intent.action != Notificare.INTENT_ACTION_REMOTE_MESSAGE_OPENED) {
            return false
        }

        handleTrampolineMessage(
            message = requireNotNull(intent.parcelable(Notificare.INTENT_EXTRA_REMOTE_MESSAGE)),
            notification = requireNotNull(intent.parcelable(Notificare.INTENT_EXTRA_NOTIFICATION)),
            action = intent.parcelable(Notificare.INTENT_EXTRA_ACTION)
        )

        return true
    }

    override fun parseNotificationOpenedIntent(intent: Intent): NotificareNotificationOpenedIntentResult? {
        if (intent.action != Notificare.INTENT_ACTION_NOTIFICATION_OPENED) {
            return null
        }

        return NotificareNotificationOpenedIntentResult(
            notification = requireNotNull(intent.parcelable(Notificare.INTENT_EXTRA_NOTIFICATION)),
        )
    }

    override fun parseNotificationActionOpenedIntent(intent: Intent): NotificareNotificationActionOpenedIntentResult? {
        if (intent.action != Notificare.INTENT_ACTION_ACTION_OPENED) {
            return null
        }

        return NotificareNotificationActionOpenedIntentResult(
            notification = requireNotNull(intent.parcelable(Notificare.INTENT_EXTRA_NOTIFICATION)),
            action = requireNotNull(intent.parcelable(Notificare.INTENT_EXTRA_ACTION)),
        )
    }

    override suspend fun registerLiveActivity(
        activityId: String,
        topics: List<String>
    ): Unit = withContext(Dispatchers.IO) {
        val device = Notificare.device().currentDevice
            ?: throw NotificareDeviceUnavailableException()

        val token = subscription?.token
            ?: throw NotificareSubscriptionUnavailable()

        val payload = CreateLiveActivityPayload(
            activity = activityId,
            token = token,
            deviceID = device.id,
            topics = topics,
        )

        NotificareRequest.Builder()
            .post("/live-activity", payload)
            .response()
    }

    override fun registerLiveActivity(
        activityId: String,
        topics: List<String>,
        callback: NotificareCallback<Unit>,
    ): Unit = toCallbackFunction(::registerLiveActivity)(activityId, topics, callback::onSuccess, callback::onFailure)

    override suspend fun endLiveActivity(activityId: String): Unit = withContext(Dispatchers.IO) {
        val device = Notificare.device().currentDevice
            ?: throw NotificareDeviceUnavailableException()

        val encodedActivityId = withContext(Dispatchers.IO) {
            URLEncoder.encode(activityId, "UTF-8")
        }

        val encodedDeviceId = withContext(Dispatchers.IO) {
            URLEncoder.encode(device.id, "UTF-8")
        }

        NotificareRequest.Builder()
            .delete("/live-activity/$encodedActivityId/$encodedDeviceId", null)
            .response()
    }

    override fun endLiveActivity(
        activityId: String,
        callback: NotificareCallback<Unit>
    ): Unit = toCallbackFunction(::endLiveActivity)(activityId, callback::onSuccess, callback::onFailure)

    // endregion

    // region Notificare Push Internal

    override fun handleNewToken(transport: NotificareTransport, token: String) {
        logger.info("Received a new push token.")

        if (!Notificare.isReady) {
            logger.debug("Notificare is not ready. Postponing token registration...")
            return
        }

        if (!sharedPreferences.remoteNotificationsEnabled) {
            logger.debug("Received a push token before enableRemoteNotifications() has been called.")
            return
        }

        notificareCoroutineScope.launch {
            try {
                updateDeviceSubscription(transport, token)
            } catch (e: Exception) {
                logger.debug("Failed to update the push subscription.", e)
            }
        }
    }

    override fun handleRemoteMessage(message: NotificareRemoteMessage) {
        if (!Notificare.isConfigured) {
            logger.warning(
                "Cannot process remote messages before Notificare is configured. Invoke Notificare.configure() when the application starts."
            )
            return
        }

        when (message) {
            is NotificareSystemRemoteMessage -> handleSystemNotification(message)
            is NotificareNotificationRemoteMessage -> handleNotification(message)
            is NotificareUnknownRemoteMessage -> {
                val notification = message.toNotification()

                Notificare.requireContext().sendBroadcast(
                    Intent(Notificare.requireContext(), intentReceiver)
                        .setAction(Notificare.INTENT_ACTION_UNKNOWN_NOTIFICATION_RECEIVED)
                        .putExtra(Notificare.INTENT_EXTRA_NOTIFICATION, notification)
                )
            }
        }
    }

    // endregion

    private fun handleTrampolineMessage(
        message: NotificareNotificationRemoteMessage,
        notification: NotificareNotification,
        action: NotificareNotification.Action?
    ) {
        notificareCoroutineScope.launch {
            // Log the notification open event.
            Notificare.events().logNotificationOpen(notification.id)
            Notificare.events().logNotificationInfluenced(notification.id)

            // Notify the inbox to mark the item as read.
            InboxIntegration.markItemAsRead(message)

            @Suppress("NAME_SHADOWING")
            val notification: NotificareNotification = try {
                if (notification.partial) {
                    Notificare.fetchNotification(message.id)
                } else {
                    notification
                }
            } catch (e: Exception) {
                logger.error("Failed to fetch notification.", e)
                return@launch
            }

            // Notify the consumer's intent receiver.
            if (action == null) {
                Notificare.requireContext().sendBroadcast(
                    Intent(Notificare.requireContext(), intentReceiver)
                        .setAction(Notificare.INTENT_ACTION_NOTIFICATION_OPENED)
                        .putExtra(Notificare.INTENT_EXTRA_NOTIFICATION, notification)
                )
            } else {
                Notificare.requireContext().sendBroadcast(
                    Intent(Notificare.requireContext(), intentReceiver)
                        .setAction(Notificare.INTENT_ACTION_ACTION_OPENED)
                        .putExtra(Notificare.INTENT_EXTRA_NOTIFICATION, notification)
                        .putExtra(Notificare.INTENT_EXTRA_ACTION, action)
                )
            }

            // Notify the consumer's custom activity about the notification open event.
            val notificationIntent = Intent()
                .setAction(
                    if (action == null) Notificare.INTENT_ACTION_NOTIFICATION_OPENED
                    else Notificare.INTENT_ACTION_ACTION_OPENED
                )
                .putExtra(Notificare.INTENT_EXTRA_NOTIFICATION, notification)
                .putExtra(Notificare.INTENT_EXTRA_ACTION, action)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .setPackage(Notificare.requireContext().packageName)

            if (notificationIntent.resolveActivity(Notificare.requireContext().packageManager) != null) {
                // Notification handled by custom activity in package
                Notificare.requireContext().startActivity(notificationIntent)
            } else if (intentReceiver.simpleName == NotificarePushIntentReceiver::class.java.simpleName) {
                logger.warning("Could not find an activity with the '${notificationIntent.action}' action.")
            }
        }
    }

    private fun checkPushPermissions(): Boolean {
        var granted = true

        if (ContextCompat.checkSelfPermission(
                Notificare.requireContext(),
                Manifest.permission.INTERNET
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            granted = false
            logger.warning("Internet access permission is denied for this app.")
        }

        if (ContextCompat.checkSelfPermission(
                Notificare.requireContext(),
                Manifest.permission.ACCESS_NETWORK_STATE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            granted = false
            logger.warning("Network state access permission is denied for this app.")
        }

        if (ContextCompat.checkSelfPermission(
                Notificare.requireContext(),
                "com.google.android.c2dm.permission.RECEIVE"
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            granted = false
            logger.warning("Push notifications permission is denied for this app.")
        }

        return granted
    }

    private fun createDefaultChannel() {
        if (Build.VERSION.SDK_INT < 26) return

        val notificationManager =
            Notificare.requireContext().getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return

        val defaultChannel = NotificationChannel(
            DEFAULT_NOTIFICATION_CHANNEL_ID,
            Notificare.requireContext().getString(R.string.notificare_default_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT
        )

        defaultChannel.description =
            Notificare.requireContext().getString(R.string.notificare_default_channel_description)
        defaultChannel.setShowBadge(true)
        notificationManager.createNotificationChannel(defaultChannel)
    }

    private fun createUniqueNotificationId(): Int {
        return notificationSequence.incrementAndGet()
    }

    private fun handleSystemNotification(message: NotificareSystemRemoteMessage) {
        if (message.type.startsWith("re.notifica.")) {
            logger.info("Processing system notification: ${message.type}")
            when (message.type) {
                "re.notifica.notification.system.Application" -> {
                    Notificare.fetchApplication(object : NotificareCallback<NotificareApplication> {
                        override fun onSuccess(result: NotificareApplication) {
                            logger.debug("Updated cached application info.")
                        }

                        override fun onFailure(e: Exception) {
                            logger.error("Failed to update cached application info.", e)
                        }
                    })
                }

                "re.notifica.notification.system.Inbox" -> InboxIntegration.reloadInbox()
                "re.notifica.notification.system.LiveActivity" -> {
                    val activity = message.extra["activity"] ?: run {
                        logger.warning(
                            "Cannot parse a live activity system notification without the 'activity' property."
                        )
                        return
                    }

                    val content = try {
                        message.extra["content"]?.let { JSONObject(it) }
                    } catch (e: Exception) {
                        logger.warning("Cannot parse the content of the live activity.", e)
                        return
                    }

                    val timestamp = message.extra["timestamp"]?.toLongOrNull() ?: run {
                        logger.warning("Cannot parse the timestamp of the live activity.")
                        return
                    }

                    val dismissalDateTimestamp = message.extra["dismissalDate"]?.toLongOrNull()

                    val update = NotificareLiveActivityUpdate(
                        activity = activity,
                        title = message.extra["title"],
                        subtitle = message.extra["subtitle"],
                        message = message.extra["message"],
                        content = content,
                        final = message.extra["final"]?.toBooleanStrictOrNull() ?: false,
                        dismissalDate = dismissalDateTimestamp?.let { Date(it) },
                        timestamp = Date(timestamp),
                    )

                    Notificare.requireContext().sendBroadcast(
                        Intent(Notificare.requireContext(), intentReceiver)
                            .setAction(Notificare.INTENT_ACTION_LIVE_ACTIVITY_UPDATE)
                            .putExtra(Notificare.INTENT_EXTRA_LIVE_ACTIVITY_UPDATE, update)
                    )
                }

                else -> logger.warning("Unhandled system notification: ${message.type}")
            }
        } else {
            logger.info("Processing custom system notification.")
            val notification = NotificareSystemNotification(
                id = requireNotNull(message.id),
                type = message.type,
                extra = message.extra,
            )

            Notificare.requireContext().sendBroadcast(
                Intent(Notificare.requireContext(), intentReceiver)
                    .setAction(Notificare.INTENT_ACTION_SYSTEM_NOTIFICATION_RECEIVED)
                    .putExtra(Notificare.INTENT_EXTRA_NOTIFICATION, notification)
            )
        }
    }

    private fun handleNotification(message: NotificareNotificationRemoteMessage) {
        notificareCoroutineScope.launch {
            try {
                Notificare.events().logNotificationReceived(message.notificationId)

                val notification = try {
                    Notificare.fetchNotification(message.id)
                } catch (e: Exception) {
                    logger.error("Failed to fetch notification.", e)
                    message.toNotification()
                }

                if (message.notify) {
                    generateNotification(
                        message = message,
                        notification = notification,
                    )
                }

                // Attempt to place the item in the inbox.
                InboxIntegration.addItemToInbox(message, notification)

                val deliveryMechanism = when {
                    message.notify -> NotificareNotificationDeliveryMechanism.STANDARD
                    else -> NotificareNotificationDeliveryMechanism.SILENT
                }

                Notificare.requireContext().sendBroadcast(
                    Intent(Notificare.requireContext(), intentReceiver)
                        .setAction(Notificare.INTENT_ACTION_NOTIFICATION_RECEIVED)
                        .putExtra(Notificare.INTENT_EXTRA_NOTIFICATION, notification)
                        .putExtra(Notificare.INTENT_EXTRA_DELIVERY_MECHANISM, deliveryMechanism as Parcelable)
                )
            } catch (e: Exception) {
                logger.error("Unable to process remote notification.", e)
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun generateNotification(
        message: NotificareNotificationRemoteMessage,
        notification: NotificareNotification
    ) {
        val extras = bundleOf(
            Notificare.INTENT_EXTRA_REMOTE_MESSAGE to message,
            Notificare.INTENT_EXTRA_NOTIFICATION to notification,
        )

        val openIntent = PendingIntent.getActivity(
            Notificare.requireContext(),
            createUniqueNotificationId(),
            Intent().apply {
                action = Notificare.INTENT_ACTION_REMOTE_MESSAGE_OPENED
                setPackage(Notificare.requireContext().packageName)
                putExtras(extras)
            },
            PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationManager = NotificationManagerCompat.from(Notificare.requireContext())

        val channel = message.notificationChannel
            ?: Notificare.options?.defaultChannelId
            ?: DEFAULT_NOTIFICATION_CHANNEL_ID

        val smallIcon = checkNotNull(Notificare.options).notificationSmallIcon
            ?: Notificare.requireContext().applicationInfo.icon

        logger.debug("Sending notification to channel '$channel'.")

        val builder = NotificationCompat.Builder(Notificare.requireContext(), channel)
            .setAutoCancel(checkNotNull(Notificare.options).notificationAutoCancel)
            .setSmallIcon(smallIcon)
            .setContentTitle(message.alertTitle)
            .setContentText(message.alert)
            .setTicker(message.alert)
            .setWhen(message.sentTime)
            .setContentIntent(openIntent)
//            .setDeleteIntent(deleteIntent)

        if (message.notificationGroup != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val notificationService =
                Notificare.requireContext().getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            if (notificationService != null) {
                val hasGroupSummary = notificationService.activeNotifications
                    .any { it.groupKey != null && it.groupKey == message.notificationGroup }

                if (!hasGroupSummary) {
                    val summary = NotificationCompat.Builder(Notificare.requireContext(), channel)
                        .setAutoCancel(checkNotNull(Notificare.options).notificationAutoCancel)
                        .setSmallIcon(smallIcon)
                        .setContentText(
                            Notificare.requireContext().getString(R.string.notificare_notification_group_summary)
                        )
                        .setWhen(message.sentTime)
                        .setGroup(message.notificationGroup)
                        .setGroupSummary(true)
                        .build()

                    notificationManager.notify(message.notificationGroup, 1, summary)
                }

                builder.setGroup(message.notificationGroup)
            }
        }

        val notificationAccentColor = checkNotNull(Notificare.options).notificationAccentColor
        if (notificationAccentColor != null) {
            builder.color = ContextCompat.getColor(Notificare.requireContext(), notificationAccentColor)
        }

        val attachmentImage = try {
            runBlocking {
                message.attachment?.uri?.let { loadBitmap(Notificare.requireContext(), it) }
            }
        } catch (e: Exception) {
            logger.warning("Failed to load the attachment image.", e)
            null
        }

        if (attachmentImage != null) {
            // Show the attachment image when the notification is collapse but make it null in the BigPictureStyle
            // to hide it when the notification gets expanded.
            builder.setLargeIcon(attachmentImage)

            builder.setStyle(
                NotificationCompat.BigPictureStyle()
                    .setSummaryText(message.alert)
                    .bigPicture(attachmentImage)
                    .bigLargeIcon(null as Bitmap?)
            )
        } else {
            builder.setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(message.alert)
                    .setSummaryText(message.alertSubtitle)
            )
        }

        // Extend for Android Wear
        val wearableExtender = NotificationCompat.WearableExtender()

        // Handle action category
        val application = Notificare.application ?: run {
            logger.debug("Notificare application was null when generation a remote notification.")
            null
        }

        if (message.actionCategory != null && application != null) {
            val category = application.actionCategories.firstOrNull { it.name == message.actionCategory }
            category?.actions?.forEach { action ->
                val useQuickResponse = action.type == NotificareNotification.Action.TYPE_CALLBACK &&
                    !action.camera && (!action.keyboard || Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)

                val useRemoteInput = useQuickResponse && action.keyboard && !action.camera

                val actionIntent = if (useQuickResponse) {
                    Intent(Notificare.requireContext(), NotificarePushSystemIntentReceiver::class.java).apply {
                        setAction(Notificare.INTENT_ACTION_QUICK_RESPONSE)
                        setPackage(Notificare.requireContext().packageName)

                        putExtras(extras)
                        putExtra(Notificare.INTENT_EXTRA_ACTION, action)
                    }
                } else {
                    Intent().apply {
                        setAction(Notificare.INTENT_ACTION_REMOTE_MESSAGE_OPENED)
                        setPackage(Notificare.requireContext().packageName)

                        putExtras(extras)
                        putExtra(Notificare.INTENT_EXTRA_ACTION, action)
                    }
                }

                builder.addAction(
                    NotificationCompat.Action.Builder(
                        action.getIconResource(Notificare.requireContext()),
                        action.getLocalizedLabel(Notificare.requireContext()),
                        if (useQuickResponse) {
                            PendingIntent.getBroadcast(
                                Notificare.requireContext(),
                                createUniqueNotificationId(),
                                actionIntent,
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                    if (useRemoteInput) {
                                        PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_MUTABLE
                                    } else {
                                        PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
                                    }
                                } else {
                                    PendingIntent.FLAG_CANCEL_CURRENT
                                }
                            )
                        } else {
                            PendingIntent.getActivity(
                                Notificare.requireContext(),
                                createUniqueNotificationId(),
                                actionIntent,
                                PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
                            )
                        }
                    ).apply {
                        if (useRemoteInput) {
                            addRemoteInput(
                                RemoteInput.Builder(Notificare.INTENT_EXTRA_TEXT_RESPONSE)
                                    .setLabel(action.getLocalizedLabel(Notificare.requireContext()))
                                    .build()
                            )
                        }
                    }.build()
                )

                wearableExtender.addAction(
                    NotificationCompat.Action.Builder(
                        action.getIconResource(Notificare.requireContext()),
                        action.getLocalizedLabel(Notificare.requireContext()),
                        PendingIntent.getBroadcast(
                            Notificare.requireContext(),
                            createUniqueNotificationId(),
                            actionIntent,
                            PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )
                    ).build()
                )
            }

            // If there are more than 2 category actions, add a more button
            // TODO check this functionality in v2
//            if (Notificare.shared().getShowMoreActionsButton() && categoryActions.length() > 2) {
//                builder.addAction(
//                    0,
//                    Notificare.shared().getApplicationContext().getString(re.notifica.R.string.notificare_action_title_intent_more),
//                    broadcast
//                )
//            }
        }

        builder.extend(wearableExtender)

        if (message.sound != null) {
            logger.debug("Trying to use sound '${message.sound}'.")
            if (message.sound == "default") {
                builder.setDefaults(Notification.DEFAULT_SOUND)
            } else {
                val identifier = Notificare.requireContext().resources.getIdentifier(
                    message.sound,
                    "raw",
                    Notificare.requireContext().packageName
                )

                if (identifier != 0) {
                    builder.setSound(
                        Uri.parse("android.resource://${Notificare.requireContext().packageName}/$identifier")
                    )
                }
            }
        }

        val lightsColor = message.lightsColor ?: checkNotNull(Notificare.options).notificationLightsColor
        if (lightsColor != null) {
            try {
                val color = Color.parseColor(lightsColor)
                val onMs = message.lightsOn ?: checkNotNull(Notificare.options).notificationLightsOn
                val offMs = message.lightsOff ?: checkNotNull(Notificare.options).notificationLightsOff

                builder.setLights(color, onMs, offMs)
            } catch (e: IllegalArgumentException) {
                logger.warning("The color '$lightsColor' could not be parsed.")
            }
        }

        notificationManager.notify(message.notificationId, 0, builder.build())
    }

    private fun onApplicationForeground() {
        if (!Notificare.isReady) return

        notificareCoroutineScope.launch {
            try {
                updateDeviceNotificationSettings()
            } catch (e: Exception) {
                logger.debug("Failed to update user notification settings.", e)
            }
        }
    }

    private suspend fun updateDeviceSubscription(): Unit = withContext(Dispatchers.IO) {
        val serviceManager = checkNotNull(serviceManager)

        val transport = serviceManager.transport
        val token = serviceManager.getPushToken()

        updateDeviceSubscription(transport, token)
    }

    private suspend fun updateDeviceSubscription(
        transport: NotificareTransport,
        token: String?,
    ): Unit = withContext(Dispatchers.IO) {
        logger.debug("Updating push subscription.")

        val device = checkNotNull(Notificare.device().currentDevice)

        val previousTransport = this@NotificarePushImpl.transport
        val previousSubscription = this@NotificarePushImpl.subscription

        if (previousTransport == transport && previousSubscription?.token == token) {
            logger.debug("Push subscription unmodified. Updating notification settings instead.")
            updateDeviceNotificationSettings()
            return@withContext
        }

        val isPushCapable = transport != NotificareTransport.NOTIFICARE
        val allowedUI = isPushCapable && hasNotificationPermission(Notificare.requireContext())

        val payload = UpdateDeviceSubscriptionPayload(
            transport = transport,
            subscriptionId = token,
            allowedUI = allowedUI,
        )

        NotificareRequest.Builder()
            .put("/push/${device.id}", payload)
            .response()

        val subscription = token?.let { NotificarePushSubscription(token = it) }

        this@NotificarePushImpl.transport = transport
        this@NotificarePushImpl.subscription = subscription
        this@NotificarePushImpl.allowedUI = allowedUI

        Notificare.requireContext().sendBroadcast(
            Intent(Notificare.requireContext(), intentReceiver)
                .setAction(Notificare.INTENT_ACTION_SUBSCRIPTION_CHANGED)
                .putExtra(Notificare.INTENT_EXTRA_SUBSCRIPTION, subscription)
        )

        if (token != null && previousSubscription?.token != token) {
            Notificare.requireContext().sendBroadcast(
                Intent(Notificare.requireContext(), intentReceiver)
                    .setAction(Notificare.INTENT_ACTION_TOKEN_CHANGED)
                    .putExtra(Notificare.INTENT_EXTRA_TOKEN, token)
            )
        }

        ensureLoggedPushRegistration()
    }

    private suspend fun updateDeviceNotificationSettings(): Unit = withContext(Dispatchers.IO) {
        logger.debug("Updating user notification settings.")

        val device = checkNotNull(Notificare.device().currentDevice)

        val previousAllowedUI = this@NotificarePushImpl.allowedUI

        val transport = transport
        val isPushCapable = transport != null && transport != NotificareTransport.NOTIFICARE
        val allowedUI = isPushCapable && hasNotificationPermission(Notificare.requireContext())

        if (previousAllowedUI != allowedUI) {
            val payload = UpdateDeviceNotificationSettingsPayload(
                allowedUI = allowedUI,
            )

            NotificareRequest.Builder()
                .put("/push/${device.id}", payload)
                .response()

            logger.debug("User notification settings updated.")
            this@NotificarePushImpl.allowedUI = allowedUI
        } else {
            logger.debug("User notification settings update skipped, nothing changed.")
        }

        ensureLoggedPushRegistration()
    }

    private suspend fun ensureLoggedPushRegistration(): Unit = withContext(Dispatchers.IO) {
        if (allowedUI && sharedPreferences.firstRegistration) {
            try {
                Notificare.events().logPushRegistration()
                sharedPreferences.firstRegistration = false
            } catch (e: Exception) {
                logger.warning("Failed to log the push registration event.", e)
            }
        }
    }

    private fun hasNotificationPermission(context: Context): Boolean {
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    private fun hasIntentFilter(context: Context, intentAction: String): Boolean {
        val intent = Intent()
            .setAction(intentAction)
            .setPackage(context.packageName)

        return intent.resolveActivity(Notificare.requireContext().packageManager) != null
    }
}
