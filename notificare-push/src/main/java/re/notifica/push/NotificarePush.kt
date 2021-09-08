package re.notifica.push

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import androidx.annotation.RestrictTo
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.RemoteInput
import androidx.core.content.ContextCompat
import androidx.core.os.BuildCompat
import androidx.core.os.bundleOf
import kotlinx.coroutines.*
import org.json.JSONObject
import re.notifica.Notificare
import re.notifica.NotificareCallback
import re.notifica.NotificareException
import re.notifica.internal.NotificareLogger
import re.notifica.internal.NotificareUtils
import re.notifica.internal.network.request.NotificareRequest
import re.notifica.models.NotificareApplication
import re.notifica.models.NotificareNotification
import re.notifica.models.NotificareTransport
import re.notifica.modules.NotificareModule
import re.notifica.push.internal.InboxIntegration
import re.notifica.push.internal.NotificarePushSystemIntentReceiver
import re.notifica.push.internal.NotificareSharedPreferences
import re.notifica.push.internal.ServiceManager
import re.notifica.push.internal.network.push.DeviceUpdateNotificationSettingsPayload
import re.notifica.push.models.*
import java.util.concurrent.atomic.AtomicInteger

public object NotificarePush : NotificareModule() {

    public const val SDK_VERSION: String = BuildConfig.SDK_VERSION

    public const val DEFAULT_NOTIFICATION_CHANNEL_ID: String = "notificare_channel_default"

    // Intent actions
    public const val INTENT_ACTION_REMOTE_MESSAGE_OPENED: String = "re.notifica.intent.action.RemoteMessageOpened"
    public const val INTENT_ACTION_NOTIFICATION_RECEIVED: String = "re.notifica.intent.action.NotificationReceived"
    public const val INTENT_ACTION_SYSTEM_NOTIFICATION_RECEIVED: String = "re.notifica.intent.action.SystemNotificationReceived"
    public const val INTENT_ACTION_UNKNOWN_NOTIFICATION_RECEIVED: String = "re.notifica.intent.action.UnknownNotificationReceived"
    public const val INTENT_ACTION_NOTIFICATION_OPENED: String = "re.notifica.intent.action.NotificationOpened"
    public const val INTENT_ACTION_ACTION_OPENED: String = "re.notifica.intent.action.ActionOpened"

    // Intent extras
    public const val INTENT_EXTRA_REMOTE_MESSAGE: String = "re.notifica.intent.extra.RemoteMessage"
    public const val INTENT_EXTRA_TEXT_RESPONSE: String = "re.notifica.intent.extra.TextResponse"

    private val notificationSequence = AtomicInteger()

    internal lateinit var sharedPreferences: NotificareSharedPreferences
        private set

    internal var serviceManager: ServiceManager? = null
        private set

    public var intentReceiver: Class<out NotificarePushIntentReceiver> = NotificarePushIntentReceiver::class.java

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public var postponedDeviceToken: String? = null

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
            sharedPreferences.remoteNotificationsEnabled = settings.getBoolean("notifications", false)
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
    }

    override suspend fun launch() {
        val token = postponedDeviceToken ?: return
        val manager = serviceManager ?: run {
            NotificareLogger.debug("Found a postponed registration token but no service manager.")
            return
        }

        NotificareLogger.info("Found a postponed registration token. Performing a device registration.")
        registerPushToken(manager.transport, token)
    }

    override suspend fun unlaunch() {}

    public val isRemoteNotificationsEnabled: Boolean
        get() {
            if (::sharedPreferences.isInitialized) {
                return sharedPreferences.remoteNotificationsEnabled
            }

            NotificareLogger.warning("Calling this method requires Notificare to have been configured.")
            return false
        }

    public var allowedUI: Boolean
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
            }

            NotificareLogger.warning("Calling this method requires Notificare to have been configured.")
        }

    public fun enableRemoteNotifications() {
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

        val manager = serviceManager ?: run {
            NotificareLogger.warning("No push dependencies have been detected. Please include one of the platform-specific push packages.")
            return
        }

        // Keep track of the status in local storage.
        sharedPreferences.remoteNotificationsEnabled = true

        // Request a push provider token.
        manager.requestPushToken()
    }

    public fun disableRemoteNotifications() {
        @OptIn(DelicateCoroutinesApi::class)
        GlobalScope.launch {
            try {
                // Keep track of the status in local storage.
                sharedPreferences.remoteNotificationsEnabled = false

                Notificare.deviceManager.registerTemporary()
                updateNotificationSettings(allowedUI = false)

                NotificareLogger.info("Unregistered from push provider.")
            } catch (e: Exception) {
                NotificareLogger.error("Failed to register a temporary device.", e)
            }
        }
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public suspend fun registerPushToken(
        transport: NotificareTransport,
        token: String
    ): Unit = withContext(Dispatchers.IO) {
        Notificare.deviceManager.registerPushToken(transport, token)

        try {
            updateNotificationSettings(
                allowedUI = NotificationManagerCompat.from(Notificare.requireContext()).areNotificationsEnabled()
            )
        } catch (e: Exception) {
            NotificareLogger.warning("Failed to update the device's notification settings.", e)
        }
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun handleRemoteMessage(message: NotificareRemoteMessage) {
        when (message) {
            is NotificareSystemRemoteMessage -> handleSystemNotification(message)
            is NotificareNotificationRemoteMessage -> handleNotification(message)
            is NotificareUnknownRemoteMessage -> {
                val notification = message.toNotification()

                Notificare.requireContext().sendBroadcast(
                    Intent(Notificare.requireContext(), intentReceiver)
                        .setAction(INTENT_ACTION_UNKNOWN_NOTIFICATION_RECEIVED)
                        .putExtra(Notificare.INTENT_EXTRA_NOTIFICATION, notification)
                )
            }
        }
    }

    public fun handleTrampolineIntent(intent: Intent): Boolean {
        if (intent.action != INTENT_ACTION_REMOTE_MESSAGE_OPENED) {
            NotificareLogger.debug("Received an un-processable intent. Ignoring...")
            return false
        }

        handleTrampolineMessage(
            message = requireNotNull(intent.getParcelableExtra(INTENT_EXTRA_REMOTE_MESSAGE)),
            notification = requireNotNull(intent.getParcelableExtra(Notificare.INTENT_EXTRA_NOTIFICATION)),
            action = intent.getParcelableExtra(Notificare.INTENT_EXTRA_ACTION)
        )

        return true
    }

    public fun handleTrampolineMessage(
        message: NotificareNotificationRemoteMessage,
        notification: NotificareNotification,
        action: NotificareNotification.Action?
    ) {
        // Log the notification open event.
        Notificare.eventsManager.logNotificationOpened(notification.id)

        @OptIn(DelicateCoroutinesApi::class)
        GlobalScope.launch(Dispatchers.IO) {
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
                        .setAction(INTENT_ACTION_NOTIFICATION_OPENED)
                        .putExtra(Notificare.INTENT_EXTRA_NOTIFICATION, notification)
                )
            } else {
                Notificare.requireContext().sendBroadcast(
                    Intent(Notificare.requireContext(), intentReceiver)
                        .setAction(INTENT_ACTION_ACTION_OPENED)
                        .putExtra(Notificare.INTENT_EXTRA_NOTIFICATION, notification)
                        .putExtra(Notificare.INTENT_EXTRA_ACTION, action)
                )
            }

            // Notify the consumer's custom activity about the notification open event.
            val notificationIntent = Intent()
                .setAction(
                    if (action == null) INTENT_ACTION_NOTIFICATION_OPENED
                    else INTENT_ACTION_ACTION_OPENED
                )
                .putExtra(Notificare.INTENT_EXTRA_NOTIFICATION, notification)
                .putExtra(Notificare.INTENT_EXTRA_ACTION, action)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .setPackage(Notificare.requireContext().packageName)

            if (notificationIntent.resolveActivity(Notificare.requireContext().packageManager) != null) {
                // Notification handled by custom activity in package
                Notificare.requireContext().startActivity(notificationIntent)
            } else {
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
                "re.notifica.notification.system.Wallet" -> {
                    // TODO: handle Wallet system notifications
                }
                "re.notifica.notification.system.Products" -> {
                    // TODO: handle Products system notifications
                }
                "re.notifica.notification.system.Inbox" -> InboxIntegration.reloadInbox()
                else -> NotificareLogger.warning("Unhandled system notification: ${message.type}")
            }
        } else {
            NotificareLogger.info("Processing custom system notification.")
            val notification = NotificareSystemNotification(
                id = message.id,
                type = message.type,
                extra = message.extra,
            )

            Notificare.requireContext().sendBroadcast(
                Intent(Notificare.requireContext(), intentReceiver)
                    .setAction(INTENT_ACTION_SYSTEM_NOTIFICATION_RECEIVED)
                    .putExtra(Notificare.INTENT_EXTRA_NOTIFICATION, notification)
            )
        }
    }

    private fun handleNotification(message: NotificareNotificationRemoteMessage) {
        Notificare.eventsManager.logNotificationReceived(message.id)

        @OptIn(DelicateCoroutinesApi::class)
        GlobalScope.launch {
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

            Notificare.requireContext().sendBroadcast(
                Intent(Notificare.requireContext(), intentReceiver)
                    .setAction(INTENT_ACTION_NOTIFICATION_RECEIVED)
                    .putExtra(Notificare.INTENT_EXTRA_NOTIFICATION, notification)
            )
        }
    }

    private fun generateNotification(
        message: NotificareNotificationRemoteMessage,
        notification: NotificareNotification
    ) {
        val extras = bundleOf(
            INTENT_EXTRA_REMOTE_MESSAGE to message,
            Notificare.INTENT_EXTRA_NOTIFICATION to notification,
        )

        val openIntent = PendingIntent.getActivity(
            Notificare.requireContext(),
            createUniqueNotificationId(),
            Intent().apply {
                action = INTENT_ACTION_REMOTE_MESSAGE_OPENED

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

        val attachmentImage = runBlocking {
            message.attachment?.uri?.let { NotificareUtils.loadBitmap(it) }
        }

        if (attachmentImage != null) {
            // Show the attachment image when the notification is collapse but make it null in the BigPictureStyle
            // to hide it when the notification gets expanded.
            builder.setLargeIcon(attachmentImage)

            builder.setStyle(
                NotificationCompat.BigPictureStyle()
                    .setSummaryText(message.alert)
                    .bigPicture(attachmentImage)
                    .bigLargeIcon(null)
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
                        setAction(NotificarePushSystemIntentReceiver.INTENT_ACTION_QUICK_RESPONSE)

                        putExtras(extras)
                        putExtra(Notificare.INTENT_EXTRA_ACTION, action)
                    }
                } else {
                    Intent().apply {
                        setAction(INTENT_ACTION_REMOTE_MESSAGE_OPENED)

                        putExtras(extras)
                        putExtra(Notificare.INTENT_EXTRA_ACTION, action)
                    }
                }

                builder.addAction(
                    NotificationCompat.Action.Builder(
                        0,
                        action.getLocalizedLabel(Notificare.requireContext()),
                        if (useQuickResponse) {
                            PendingIntent.getBroadcast(
                                Notificare.requireContext(),
                                createUniqueNotificationId(),
                                actionIntent,
                                // Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                                if (BuildCompat.isAtLeastS()) {
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
                                RemoteInput.Builder(INTENT_EXTRA_TEXT_RESPONSE)
                                    .setLabel(action.getLocalizedLabel(Notificare.requireContext()))
                                    .build()
                            )
                        }
                    }.build()
                )

                wearableExtender.addAction(
                    NotificationCompat.Action.Builder(
                        0,
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
                    builder.setSound(Uri.parse("android.resource://${Notificare.requireContext().packageName}/$identifier"))
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
                NotificareLogger.warning("The color '${lightsColor}' could not be parsed.")
            }
        }

        notificationManager.notify(message.notificationId, 0, builder.build())
    }

    private suspend fun updateNotificationSettings(allowedUI: Boolean): Unit = withContext(Dispatchers.IO) {
        val device = Notificare.deviceManager.currentDevice ?: run {
            NotificareLogger.warning("No device registered yet.")
            throw NotificareException.NotReady()
        }

        NotificareRequest.Builder()
            .put(
                url = "/device/${device.id}",
                body = DeviceUpdateNotificationSettingsPayload(
                    allowedUI = allowedUI,
                ),
            )
            .response()

        // Update current device properties.
        this@NotificarePush.allowedUI = allowedUI
    }
}
