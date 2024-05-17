package re.notifica.push.internal

import android.Manifest
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
import java.net.URLEncoder
import java.util.Date
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.json.JSONObject
import re.notifica.Notificare
import re.notifica.NotificareCallback
import re.notifica.NotificareDeviceUnavailableException
import re.notifica.NotificareNotConfiguredException
import re.notifica.internal.NotificareLogger
import re.notifica.internal.NotificareModule
import re.notifica.internal.NotificareUtils
import re.notifica.internal.ktx.coroutineScope
import re.notifica.internal.ktx.parcelable
import re.notifica.internal.ktx.toCallbackFunction
import re.notifica.internal.network.request.NotificareRequest
import re.notifica.ktx.device
import re.notifica.ktx.events
import re.notifica.models.NotificareApplication
import re.notifica.models.NotificareNotification
import re.notifica.models.NotificareTransport
import re.notifica.push.NotificareInternalPush
import re.notifica.push.NotificarePush
import re.notifica.push.NotificarePushIntentReceiver
import re.notifica.push.R
import re.notifica.push.automaticDefaultChannelEnabled
import re.notifica.push.defaultChannelId
import re.notifica.push.internal.network.push.CreateLiveActivityPayload
import re.notifica.push.internal.network.push.DeviceUpdateNotificationSettingsPayload
import re.notifica.push.ktx.INTENT_ACTION_ACTION_OPENED
import re.notifica.push.ktx.INTENT_ACTION_LIVE_ACTIVITY_UPDATE
import re.notifica.push.ktx.INTENT_ACTION_NOTIFICATION_OPENED
import re.notifica.push.ktx.INTENT_ACTION_NOTIFICATION_RECEIVED
import re.notifica.push.ktx.INTENT_ACTION_QUICK_RESPONSE
import re.notifica.push.ktx.INTENT_ACTION_REMOTE_MESSAGE_OPENED
import re.notifica.push.ktx.INTENT_ACTION_SYSTEM_NOTIFICATION_RECEIVED
import re.notifica.push.ktx.INTENT_ACTION_TOKEN_CHANGED
import re.notifica.push.ktx.INTENT_ACTION_UNKNOWN_NOTIFICATION_RECEIVED
import re.notifica.push.ktx.INTENT_EXTRA_DELIVERY_MECHANISM
import re.notifica.push.ktx.INTENT_EXTRA_LIVE_ACTIVITY_UPDATE
import re.notifica.push.ktx.INTENT_EXTRA_REMOTE_MESSAGE
import re.notifica.push.ktx.INTENT_EXTRA_TEXT_RESPONSE
import re.notifica.push.ktx.INTENT_EXTRA_TOKEN
import re.notifica.push.ktx.deviceInternal
import re.notifica.push.ktx.logNotificationInfluenced
import re.notifica.push.ktx.logNotificationReceived
import re.notifica.push.ktx.logPushRegistration
import re.notifica.push.ktx.loyaltyIntegration
import re.notifica.push.models.NotificareLiveActivityUpdate
import re.notifica.push.models.NotificareNotificationDeliveryMechanism
import re.notifica.push.models.NotificareNotificationRemoteMessage
import re.notifica.push.models.NotificareRemoteMessage
import re.notifica.push.models.NotificareSystemNotification
import re.notifica.push.models.NotificareSystemRemoteMessage
import re.notifica.push.models.NotificareUnknownRemoteMessage
import re.notifica.push.notificationAccentColor
import re.notifica.push.notificationAutoCancel
import re.notifica.push.notificationLightsColor
import re.notifica.push.notificationLightsOff
import re.notifica.push.notificationLightsOn
import re.notifica.push.notificationSmallIcon

@Keep
internal object NotificarePushImpl : NotificareModule(), NotificarePush, NotificareInternalPush {

    internal const val DEFAULT_NOTIFICATION_CHANNEL_ID: String = "notificare_channel_default"

    private var postponedDeviceToken: String? = null
    private val notificationSequence = AtomicInteger()
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
                    NotificareLogger.error("Failed to migrate the 'allowedUI' property.", e)
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
        sharedPreferences = NotificareSharedPreferences(Notificare.requireContext())
        serviceManager = ServiceManager.create()

        checkPushPermissions()

        if (checkNotNull(Notificare.options).automaticDefaultChannelEnabled) {
            NotificareLogger.debug("Creating the default notifications channel.")
            createDefaultChannel()
        }

        // NOTE: The allowedUI is only gettable after the storage has been configured.
        _observableAllowedUI.postValue(allowedUI)

        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                onApplicationForeground()
            }
        })
    }

    override suspend fun launch() {
        val manager = serviceManager
        val token = postponedDeviceToken?.also {
            NotificareLogger.debug("Processing postponed push token during launch.")
            postponedDeviceToken = null
        }

        if (token != null && token != Notificare.device().currentDevice?.id) {
            if (sharedPreferences.remoteNotificationsEnabled) {
                if (manager != null) {
                    NotificareLogger.info("Found a postponed registration token. Performing a device registration.")
                    registerPushToken(manager.transport, token, performReadinessCheck = false)

                    // NOTE: the notification settings are updated after a push token registration.

                    Notificare.requireContext().sendBroadcast(
                        Intent(Notificare.requireContext(), intentReceiver)
                            .setAction(Notificare.INTENT_ACTION_TOKEN_CHANGED)
                            .putExtra(Notificare.INTENT_EXTRA_TOKEN, token)
                    )

                    return
                } else {
                    NotificareLogger.debug("Found a postponed registration token but no service manager.")
                }
            } else {
                NotificareLogger.debug(
                    "Processing a postponed push token before enableRemoteNotifications() has been called."
                )
            }
        }

        // Ensure the definitive allowedUI value has been communicated to the API.
        updateNotificationSettings()
    }

    override suspend fun postLaunch() {
        if (sharedPreferences.remoteNotificationsEnabled) {
            NotificareLogger.debug("Enabling remote notifications automatically.")
            updatePushSubscription()
        }
    }

    override suspend fun unlaunch() {
        sharedPreferences.remoteNotificationsEnabled = false
        sharedPreferences.firstRegistration = true

        // Update the local notification settings.
        // Registering a temporary device automatically reports the allowedUI to the API.
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

            NotificareLogger.warning("Calling this method requires Notificare to have been configured.")
            return false
        }

    override var allowedUI: Boolean
        get() {
            if (::sharedPreferences.isInitialized) {
                return sharedPreferences.allowedUI
            }

            NotificareLogger.warning("Calling this method requires Notificare to have been configured.")
            return false
        }
        private set(value) {
            if (::sharedPreferences.isInitialized) {
                sharedPreferences.allowedUI = value
                _observableAllowedUI.postValue(value)
                return
            }

            NotificareLogger.warning("Calling this method requires Notificare to have been configured.")
        }

    override val observableAllowedUI: LiveData<Boolean> = _observableAllowedUI

    override fun enableRemoteNotifications() {
        if (!Notificare.isReady) {
            NotificareLogger.warning("Notificare is not ready yet.")
            return
        }

        val application = Notificare.application ?: run {
            NotificareLogger.warning("Notificare is not ready yet.")
            return
        }

        if (application.services[NotificareApplication.ServiceKeys.GCM] != true) {
            NotificareLogger.warning("Push notifications service is not enabled.")
            return
        }

        // Keep track of the status in local storage.
        sharedPreferences.remoteNotificationsEnabled = true

        Notificare.coroutineScope.launch {
            try {
                updatePushSubscription()
            } catch (e: Exception) {
                NotificareLogger.debug("Failed to register the device with a push token.", e)
            }
        }
    }

    override fun disableRemoteNotifications() {
        Notificare.coroutineScope.launch {
            try {
                // Keep track of the status in local storage.
                sharedPreferences.remoteNotificationsEnabled = false

                Notificare.deviceInternal().registerTemporary()

                // Update the local notification settings.
                // Registering a temporary device automatically reports the allowedUI to the API.
                allowedUI = false

                NotificareLogger.info("Unregistered from push provider.")
            } catch (e: Exception) {
                NotificareLogger.error("Failed to register a temporary device.", e)
            }
        }
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

    override suspend fun registerLiveActivity(
        activityId: String,
        topics: List<String>
    ): Unit = withContext(Dispatchers.IO) {
        val device = Notificare.device().currentDevice
            ?: throw NotificareDeviceUnavailableException()

        // TODO: fetch current FCM token

        val payload = CreateLiveActivityPayload(
            activity = activityId,
            token = device.id,
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
    ): Unit = toCallbackFunction(::registerLiveActivity)(activityId, topics, callback)

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
    ): Unit = toCallbackFunction(::endLiveActivity)(activityId, callback)

    // endregion

    // region Notificare Push Internal

    override suspend fun registerPushToken(
        transport: NotificareTransport,
        token: String,
        performReadinessCheck: Boolean,
    ): Unit = withContext(Dispatchers.IO) {
        if (!Notificare.isReady && performReadinessCheck) {
            NotificareLogger.warning("Notificare is not ready. Postponing token registration...")
            postponedDeviceToken = token

            return@withContext
        }

        if (!sharedPreferences.remoteNotificationsEnabled) {
            NotificareLogger.debug("Received a push token before enableRemoteNotifications() has been called.")
            return@withContext
        }

        if (token != Notificare.device().currentDevice?.id) {
            Notificare.deviceInternal().registerPushToken(transport, token)

            Notificare.requireContext().sendBroadcast(
                Intent(Notificare.requireContext(), intentReceiver)
                    .setAction(Notificare.INTENT_ACTION_TOKEN_CHANGED)
                    .putExtra(Notificare.INTENT_EXTRA_TOKEN, token)
            )
        } else {
            NotificareLogger.debug("Received token has already been registered. Skipping the registration...")
        }

        try {
            updateNotificationSettings()

            if (allowedUI && sharedPreferences.firstRegistration) {
                Notificare.events().logPushRegistration()
                sharedPreferences.firstRegistration = false
            }
        } catch (e: Exception) {
            NotificareLogger.warning("Failed to update the device's notification settings.", e)
        }
    }

    override fun handleRemoteMessage(message: NotificareRemoteMessage) {
        if (!Notificare.isConfigured) {
            NotificareLogger.warning(
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
        Notificare.coroutineScope.launch {
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
                NotificareLogger.error("Failed to fetch notification.", e)
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
                NotificareLogger.warning("Could not find an activity with the '${notificationIntent.action}' action.")
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
            NotificareLogger.warning("Internet access permission is denied for this app.")
        }

        if (ContextCompat.checkSelfPermission(
                Notificare.requireContext(),
                Manifest.permission.ACCESS_NETWORK_STATE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            granted = false
            NotificareLogger.warning("Network state access permission is denied for this app.")
        }

        if (ContextCompat.checkSelfPermission(
                Notificare.requireContext(),
                "com.google.android.c2dm.permission.RECEIVE"
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            granted = false
            NotificareLogger.warning("Push notifications permission is denied for this app.")
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
            NotificareLogger.info("Processing system notification: ${message.type}")
            when (message.type) {
                "re.notifica.notification.system.Application" -> {
                    Notificare.fetchApplication(object : NotificareCallback<NotificareApplication> {
                        override fun onSuccess(result: NotificareApplication) {
                            NotificareLogger.debug("Updated cached application info.")
                        }

                        override fun onFailure(e: Exception) {
                            NotificareLogger.error("Failed to update cached application info.", e)
                        }
                    })
                }
                "re.notifica.notification.system.Passbook" -> {
                    Notificare.loyaltyIntegration()?.onPassbookSystemNotificationReceived()
                }
                "re.notifica.notification.system.Products" -> {
                    // TODO: handle Products system notifications
                }
                "re.notifica.notification.system.Inbox" -> InboxIntegration.reloadInbox()
                "re.notifica.notification.system.LiveActivity" -> {
                    val activity = message.extra["activity"] ?: run {
                        NotificareLogger.warning(
                            "Cannot parse a live activity system notification without the 'activity' property."
                        )
                        return
                    }

                    val content = try {
                        message.extra["content"]?.let { JSONObject(it) }
                    } catch (e: Exception) {
                        NotificareLogger.warning("Cannot parse the content of the live activity.", e)
                        return
                    }

                    val timestamp = message.extra["timestamp"]?.toLongOrNull() ?: run {
                        NotificareLogger.warning("Cannot parse the timestamp of the live activity.")
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
                else -> NotificareLogger.warning("Unhandled system notification: ${message.type}")
            }
        } else {
            NotificareLogger.info("Processing custom system notification.")
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
        Notificare.coroutineScope.launch {
            try {
                Notificare.events().logNotificationReceived(message.notificationId)

                val notification = try {
                    Notificare.fetchNotification(message.id)
                } catch (e: Exception) {
                    NotificareLogger.error("Failed to fetch notification.", e)
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
                NotificareLogger.error("Unable to process remote notification.", e)
            }
        }
    }

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

        NotificareLogger.debug("Sending notification to channel '$channel'.")

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
                message.attachment?.uri?.let { NotificareUtils.loadBitmap(it) }
            }
        } catch (e: Exception) {
            NotificareLogger.warning("Failed to load the attachment image.", e)
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
            NotificareLogger.debug("Notificare application was null when generation a remote notification.")
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
            NotificareLogger.debug("Trying to use sound '${message.sound}'.")
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
                NotificareLogger.warning("The color '$lightsColor' could not be parsed.")
            }
        }

        notificationManager.notify(message.notificationId, 0, builder.build())
    }

    private fun onApplicationForeground() {
        if (!Notificare.isReady) return

        Notificare.coroutineScope.launch {
            try {
                updateNotificationSettings()
            } catch (e: Exception) {
                // Silent failure.
            }
        }
    }

    private suspend fun updatePushSubscription(): Unit = withContext(Dispatchers.IO) {
        val serviceManager = checkNotNull(serviceManager)

        val token = serviceManager.getPushToken()
        registerPushToken(serviceManager.transport, token)
    }

    private suspend fun updateNotificationSettings(): Unit = withContext(Dispatchers.IO) {
        val granted = NotificationManagerCompat.from(Notificare.requireContext()).areNotificationsEnabled()
        updateNotificationSettings(granted)
    }

    private suspend fun updateNotificationSettings(granted: Boolean): Unit = withContext(Dispatchers.IO) {
        if (!Notificare.isConfigured) throw NotificareNotConfiguredException()

        val device = Notificare.device().currentDevice
            ?: throw NotificareDeviceUnavailableException()

        // The allowedUI is only true when the device has push capabilities and the user accepted the permission.
        val allowedUI = device.transport != NotificareTransport.NOTIFICARE && granted

        if (this@NotificarePushImpl.allowedUI == allowedUI) {
            NotificareLogger.debug("User notification settings update skipped, nothing changed.")
            return@withContext
        }

        NotificareRequest.Builder()
            .put(
                url = "/device/${device.id}",
                body = DeviceUpdateNotificationSettingsPayload(
                    allowedUI = allowedUI,
                ),
            )
            .response()

        NotificareLogger.debug("User notification settings updated.")

        // Update current device properties.
        this@NotificarePushImpl.allowedUI = allowedUI
    }
}
