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
import androidx.core.content.ContextCompat
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import re.notifica.Notificare
import re.notifica.NotificareLogger
import re.notifica.internal.NotificareUtils
import re.notifica.models.NotificareApplication
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
    const val INTENT_ACTION_NOTIFICATION_OPENED = "re.notifica.intent.action.NotificationOpened"
    const val INTENT_EXTRA_NOTIFICATION = "re.notifica.intent.extra.Notification"

    private var serviceManager: NotificareServiceManager? = null
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
                        .setAction(NotificarePushIntentReceiver.Actions.UNKNOWN_NOTIFICATION_RECEIVED)
                        .putExtra(NotificarePushIntentReceiver.Extras.NOTIFICATION, notification)
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
            try {
                val notification = Notificare.fetchNotification(message.id)

                // TODO notify listeners

                Notificare.requireContext().sendBroadcast(
                    Intent(Notificare.requireContext(), intentReceiver)
                        .setAction(NotificarePushIntentReceiver.Actions.NOTIFICATION_RECEIVED)
                        .putExtra(NotificarePushIntentReceiver.Extras.NOTIFICATION, notification)
                )
            } catch (e: Exception) {
                NotificareLogger.error("Failed to fetch notification.", e)
            }
        }

        // TODO add to inbox

        if (message.notify) {
            generateNotification(
                type = NotificationIntentType.NOTIFICATION,
                message = message,
            )
        }
    }

    private fun generateNotification(type: NotificationIntentType, message: NotificareNotificationRemoteMessage) {
        val openIntent = PendingIntent.getBroadcast(
            Notificare.requireContext(),
            createUniqueNotificationId(),
            Intent(Notificare.requireContext(), NotificarePushSystemIntentReceiver::class.java).apply {
                action = when (type) {
                    NotificationIntentType.NOTIFICATION -> NotificarePushSystemIntentReceiver.Actions.REMOTE_MESSAGE_OPENED
//                    NotificationIntentType.RELEVANCE_NOTIFICATION -> NotificarePushSystemIntentReceiver.Actions.RELEVANCE_REMOTE_MESSAGE_OPENED
                }

                putExtra(NotificarePushSystemIntentReceiver.Extras.REMOTE_MESSAGE, message)
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
//                putExtra(NotificarePushSystemIntentReceiver.Extras.REMOTE_MESSAGE, message)
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

//        if (largeIcon != null) {
//            builder.setLargeIcon(largeIcon)
//        }

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

        /*
        // Extend for Android Auto
        NotificationCompat.CarExtender carExtender = new NotificationCompat.CarExtender();

    	RemoteInput remoteCarInput = new RemoteInput.Builder(Notificare.INTENT_EXTRA_VOICE_REPLY)
        .setLabel("reply")
        .setChoices(new CharSequence[]{"yes","no"})
        .setAllowFreeFormInput(true)
        .build();

        UnreadConversation.Builder unreadConversationBuilder =
        	    new UnreadConversation.Builder(Notificare.shared().getApplicationName())
        			.addMessage(notificationIntent.getStringExtra(Notificare.INTENT_EXTRA_ALERT))
        			.setLatestTimestamp(new Date().getTime())
        	        .setReadPendingIntent(broadcast)
        	        .setReplyAction(broadcast, remoteCarInput);

        carExtender.setUnreadConversation(unreadConversationBuilder.build());
        */

        // TODO handle action categories


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

    private enum class NotificationIntentType {
        NOTIFICATION,
//        RELEVANCE_NOTIFICATION
    }
}
