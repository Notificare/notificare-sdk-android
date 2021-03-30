package re.notifica.push

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import androidx.annotation.RestrictTo
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.RemoteInput
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import re.notifica.Notificare
import re.notifica.NotificareLogger
import re.notifica.internal.NotificareUtils
import re.notifica.models.NotificareApplication
import re.notifica.models.NotificareNotification
import re.notifica.models.NotificareTransport
import re.notifica.modules.NotificareModule
import re.notifica.push.app.NotificarePushIntentReceiver
import re.notifica.push.internal.NotificarePushSystemIntentReceiver
import re.notifica.push.models.NotificareNotificationRemoteMessage
import re.notifica.push.models.NotificareRemoteMessage
import re.notifica.push.models.NotificareSystemRemoteMessage
import re.notifica.push.models.NotificareUnknownRemoteMessage
import java.util.concurrent.atomic.AtomicInteger

object NotificarePush : NotificareModule() {

    const val DEFAULT_NOTIFICATION_CHANNEL_ID = "notificare_channel_default"
    internal const val INBOX_RECEIVER_CLASS_NAME = "re.notifica.inbox.internal.NotificareInboxSystemReceiver"

    // Intent actions
    const val INTENT_ACTION_REMOTE_MESSAGE_OPENED = "re.notifica.intent.action.RemoteMessageOpened"
    const val INTENT_ACTION_NOTIFICATION_RECEIVED = "re.notifica.intent.action.NotificationReceived"
    const val INTENT_ACTION_SYSTEM_NOTIFICATION_RECEIVED = "re.notifica.intent.action.SystemNotificationReceived"
    const val INTENT_ACTION_UNKNOWN_NOTIFICATION_RECEIVED = "re.notifica.intent.action.UnknownNotificationReceived"
    const val INTENT_ACTION_NOTIFICATION_OPENED = "re.notifica.intent.action.NotificationOpened"
    const val INTENT_ACTION_ACTION_OPENED = "re.notifica.intent.action.ActionOpened"
    private const val INTENT_ACTION_INBOX_NOTIFICATION_RECEIVED = "re.notifica.inbox.intent.action.NotificationReceived"
    internal const val INTENT_ACTION_INBOX_MARK_ITEM_AS_READ = "re.notifica.inbox.intent.action.MarkItemAsRead"

    // Intent extras
    const val INTENT_EXTRA_REMOTE_MESSAGE = "re.notifica.intent.extra.RemoteMessage"
    const val INTENT_EXTRA_TEXT_RESPONSE = "re.notifica.intent.extra.TextResponse"
    private const val INTENT_EXTRA_INBOX_NOTIFICATION_RECEIVED_BUNDLE = "re.notifica.inbox.intent.extra.InboxBundle"
    internal const val INTENT_EXTRA_INBOX_ITEM_ID = "re.notifica.inbox.intent.extra.InboxItemId"

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    var serviceManager: NotificareServiceManager? = null
        private set
    private val notificationSequence = AtomicInteger()

    var intentReceiver: Class<out NotificarePushIntentReceiver> = NotificarePushIntentReceiver::class.java

    override fun configure() {
        checkPushPermissions()

        if (checkNotNull(Notificare.options).automaticDefaultChannelEnabled) {
            NotificareLogger.debug("Creating the default notifications channel.")
            createDefaultChannel()
        }

        serviceManager = NotificareServiceManager.Factory.create(
            Notificare.requireContext()
        )
    }

    override suspend fun launch() {}


    fun enableRemoteNotifications() {
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

        manager.registerDeviceToken()
    }

    fun disableRemoteNotifications() {
        GlobalScope.launch {
            try {
                Notificare.deviceManager.registerTemporary()
                NotificareLogger.info("Unregistered from push provider.")
            } catch (e: Exception) {
                NotificareLogger.error("Failed to register a temporary device.", e)
            }
        }
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    suspend fun registerPushToken(transport: NotificareTransport, token: String) {
        Notificare.deviceManager.registerPushToken(transport, token)

        try {
            Notificare.deviceManager.updateNotificationSettings(allowedUI = getAllowedUI())
        } catch (e: Exception) {
            NotificareLogger.warning("Failed to update the device's notification settings.", e)
        }
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    fun handleRemoteMessage(message: NotificareRemoteMessage) {
        when (message) {
            is NotificareSystemRemoteMessage -> handleSystemNotification(message)
            is NotificareNotificationRemoteMessage -> handleNotification(message)
            is NotificareUnknownRemoteMessage -> {
                val notification = message.toNotification()

                // TODO notify listeners

                Notificare.requireContext().sendBroadcast(
                    Intent(Notificare.requireContext(), intentReceiver)
                        .setAction(INTENT_ACTION_UNKNOWN_NOTIFICATION_RECEIVED)
                        .putExtra(Notificare.INTENT_EXTRA_NOTIFICATION, notification)
                )
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
        if (message.systemType.startsWith("re.notifica.")) {
            NotificareLogger.info("Processing system notification: ${message.systemType}")
            when (message.systemType) {
                "re.notifica.notification.system.Application" -> {
                    // TODO: handle Application system notifications
//        Notificare.shared.fetchApplication { result in
//            switch result {
//            case .success:
//                self.reloadActionCategories()
//                completion(.success(()))
//
//            case .failure:
//                completion(.success(()))
//            }
//        }
                }
                "re.notifica.notification.system.Wallet" -> {
                    // TODO: handle Wallet system notifications
                }
                "re.notifica.notification.system.Products" -> {
                    // TODO: handle Products system notifications
                }
                "re.notifica.notification.system.Inbox" -> {
                    // TODO: handle Inbox system notifications
                    // Notify the inbox to reload itself.
                    // NotificationCenter.default.post(name: NotificareDefinitions.InternalNotification.reloadInbox, object: nil, userInfo: nil)
                }
                else -> NotificareLogger.warning("Unhandled system notification: ${message.systemType}")
            }
        } else {
            NotificareLogger.info("Processing custom system notification.")

//                val ignoreKeys = listOf(
//                    "system",
//                    "systemType",
//                    "id",
//                    "notification_id",
//                    "notification_type",
//                    "attachment",
//                    "x-sender"
//                )
//
//                val notification = NotificareSystemNotification(
//                    id = remoteMessage.data["id"],
//                    type = remoteMessage.systemType,
//                    extra = remoteMessage.data.filterKeys { !ignoreKeys.contains(it) }
//                )
//
//                Notificare.requireContext().sendBroadcast(
//                    Intent(Notificare.requireContext(), intentReceiver)
//                        .setAction(NotificarePushIntentReceiver.Actions.SYSTEM_NOTIFICATION_RECEIVED)
//                        .putExtra(NotificarePushIntentReceiver.Extras.NOTIFICATION, notification)
//                )
        }
    }

    private fun handleNotification(message: NotificareNotificationRemoteMessage) {
        Notificare.eventsManager.logNotificationReceived(message.id)

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

            if (message.inboxItemId != null) {
                try {
                    val klass = Class.forName("re.notifica.inbox.internal.NotificareInboxSystemReceiver")
                    val intent = Intent(Notificare.requireContext(), klass).apply {
                        action = INTENT_ACTION_INBOX_NOTIFICATION_RECEIVED

                        putExtra(Notificare.INTENT_EXTRA_NOTIFICATION, notification)
                        putExtra(
                            INTENT_EXTRA_INBOX_NOTIFICATION_RECEIVED_BUNDLE,
                            bundleOf(
                                "inboxItemId" to message.inboxItemId,
                                "inboxItemVisible" to message.inboxItemVisible,
                                "inboxItemExpires" to message.inboxItemExpires
                            )
                        )
                    }

                    Notificare.requireContext().sendBroadcast(intent)
                } catch (e: Exception) {
                    NotificareLogger.debug("Failed to send an inbox broadcast.", e)
                }
            }

            // TODO notify listeners

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

        val openIntent = PendingIntent.getBroadcast(
            Notificare.requireContext(),
            createUniqueNotificationId(),
            Intent(Notificare.requireContext(), NotificarePushSystemIntentReceiver::class.java).apply {
                action = INTENT_ACTION_REMOTE_MESSAGE_OPENED

                putExtras(extras)
            },
            PendingIntent.FLAG_CANCEL_CURRENT
        )

//        val deleteIntent = PendingIntent.getBroadcast(
//            Notificare.requireContext(),
//            createUniqueNotificationId(),
//            Intent(Notificare.requireContext(), NotificarePushSystemIntentReceiver::class.java).apply {
//                action = when (type) {
//                    NotificationIntentType.NOTIFICATION -> NotificarePushSystemIntentReceiver.Actions.REMOTE_MESSAGE_DELETED
//                    NotificationIntentType.RELEVANCE_NOTIFICATION -> NotificarePushSystemIntentReceiver.Actions.RELEVANCE_REMOTE_MESSAGE_DELETED
//                }
//
//                putExtras(extras)
//            },
//            PendingIntent.FLAG_CANCEL_CURRENT
//        )

        val notificationManager = NotificationManagerCompat.from(Notificare.requireContext())

        val channel = message.notificationChannel
            ?: Notificare.options?.defaultChannelId
            ?: DEFAULT_NOTIFICATION_CHANNEL_ID

        NotificareLogger.debug("Sending notification to channel '$channel'.")

        val builder = NotificationCompat.Builder(Notificare.requireContext(), channel)
            .setAutoCancel(checkNotNull(Notificare.options).notificationAutoCancel)
            .setSmallIcon(checkNotNull(Notificare.options).notificationSmallIcon)
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
                        .setSmallIcon(checkNotNull(Notificare.options).notificationSmallIcon)
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
            builder.setStyle(
                NotificationCompat.BigPictureStyle()
                    .setSummaryText(message.alert)
                    .bigPicture(attachmentImage)
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
                val actionIntent =
                    Intent(Notificare.requireContext(), NotificarePushSystemIntentReceiver::class.java).apply {
                        setAction(INTENT_ACTION_REMOTE_MESSAGE_OPENED)

                        putExtras(extras)
                        putExtra(Notificare.INTENT_EXTRA_ACTION, action)
                    }

                builder.addAction(
                    NotificationCompat.Action.Builder(
                        0,
                        action.getLocalizedLabel(Notificare.requireContext()),
                        PendingIntent.getBroadcast(
                            Notificare.requireContext(),
                            createUniqueNotificationId(),
                            actionIntent,
                            PendingIntent.FLAG_CANCEL_CURRENT
                        )
                    ).apply {
                        if (action.keyboard && !action.camera) {
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
                            PendingIntent.FLAG_CANCEL_CURRENT
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
                val color = Color.parseColor(message.lightsColor)
                val onMs = message.lightsOn ?: checkNotNull(Notificare.options).notificationLightsOn
                val offMs = message.lightsOff ?: checkNotNull(Notificare.options).notificationLightsOff

                builder.setLights(color, onMs, offMs)
            } catch (e: IllegalArgumentException) {
                NotificareLogger.warning("The color '${lightsColor}' could not be parsed.")
            }
        }

        notificationManager.notify(message.notificationId, 0, builder.build())
    }

    private fun getAllowedUI(): Boolean {
        return NotificationManagerCompat.from(Notificare.requireContext()).areNotificationsEnabled()
    }
}
