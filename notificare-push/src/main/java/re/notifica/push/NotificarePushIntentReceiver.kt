package re.notifica.push

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import re.notifica.Notificare
import re.notifica.utilities.parcel.parcelable
import re.notifica.models.NotificareNotification
import re.notifica.push.internal.logger
import re.notifica.push.ktx.INTENT_ACTION_ACTION_OPENED
import re.notifica.push.ktx.INTENT_ACTION_LIVE_ACTIVITY_UPDATE
import re.notifica.push.ktx.INTENT_ACTION_NOTIFICATION_OPENED
import re.notifica.push.ktx.INTENT_ACTION_NOTIFICATION_RECEIVED
import re.notifica.push.ktx.INTENT_ACTION_SUBSCRIPTION_CHANGED
import re.notifica.push.ktx.INTENT_ACTION_SYSTEM_NOTIFICATION_RECEIVED
import re.notifica.push.ktx.INTENT_ACTION_TOKEN_CHANGED
import re.notifica.push.ktx.INTENT_ACTION_UNKNOWN_NOTIFICATION_RECEIVED
import re.notifica.push.ktx.INTENT_EXTRA_DELIVERY_MECHANISM
import re.notifica.push.ktx.INTENT_EXTRA_LIVE_ACTIVITY_UPDATE
import re.notifica.push.ktx.INTENT_EXTRA_SUBSCRIPTION
import re.notifica.push.ktx.INTENT_EXTRA_TOKEN
import re.notifica.push.models.NotificareLiveActivityUpdate
import re.notifica.push.models.NotificareNotificationDeliveryMechanism
import re.notifica.push.models.NotificarePushSubscription
import re.notifica.push.models.NotificareSystemNotification
import re.notifica.push.models.NotificareUnknownNotification

/**
 * A broadcast receiver for handling push-related events from the Notificare SDK.
 *
 * Extend this class to handle push notifications, subscription updates, live activity updates, and other
 * push-related events. Override specific methods to handle each event as needed.
 */
public open class NotificarePushIntentReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Notificare.INTENT_ACTION_SUBSCRIPTION_CHANGED -> {
                val subscription: NotificarePushSubscription? = intent.parcelable(Notificare.INTENT_EXTRA_SUBSCRIPTION)
                onSubscriptionChanged(context, subscription)
            }

            Notificare.INTENT_ACTION_TOKEN_CHANGED -> {
                val token: String = requireNotNull(
                    intent.getStringExtra(Notificare.INTENT_EXTRA_TOKEN)
                )

                @Suppress("DEPRECATION")
                onTokenChanged(context, token)
            }

            Notificare.INTENT_ACTION_NOTIFICATION_RECEIVED -> {
                val notification: NotificareNotification = requireNotNull(
                    intent.parcelable(Notificare.INTENT_EXTRA_NOTIFICATION)
                )

                val deliveryMechanism: NotificareNotificationDeliveryMechanism = requireNotNull(
                    intent.parcelable(Notificare.INTENT_EXTRA_DELIVERY_MECHANISM)
                )

                onNotificationReceived(context, notification, deliveryMechanism)
            }

            Notificare.INTENT_ACTION_SYSTEM_NOTIFICATION_RECEIVED -> {
                val notification: NotificareSystemNotification = requireNotNull(
                    intent.parcelable(Notificare.INTENT_EXTRA_NOTIFICATION)
                )

                onSystemNotificationReceived(context, notification)
            }

            Notificare.INTENT_ACTION_UNKNOWN_NOTIFICATION_RECEIVED -> {
                val notification: NotificareUnknownNotification = requireNotNull(
                    intent.parcelable(Notificare.INTENT_EXTRA_NOTIFICATION)
                )

                onUnknownNotificationReceived(context, notification)
            }

            Notificare.INTENT_ACTION_NOTIFICATION_OPENED -> {
                val notification: NotificareNotification = requireNotNull(
                    intent.parcelable(Notificare.INTENT_EXTRA_NOTIFICATION)
                )

                onNotificationOpened(context, notification)
            }

            Notificare.INTENT_ACTION_ACTION_OPENED -> {
                val notification: NotificareNotification = requireNotNull(
                    intent.parcelable(Notificare.INTENT_EXTRA_NOTIFICATION)
                )

                val action: NotificareNotification.Action = requireNotNull(
                    intent.parcelable(Notificare.INTENT_EXTRA_ACTION)
                )

                onActionOpened(context, notification, action)
            }

            Notificare.INTENT_ACTION_LIVE_ACTIVITY_UPDATE -> {
                val update: NotificareLiveActivityUpdate = requireNotNull(
                    intent.parcelable(Notificare.INTENT_EXTRA_LIVE_ACTIVITY_UPDATE)
                )

                onLiveActivityUpdate(context, update)
            }
        }
    }

    /**
     * Called when the device's push subscription changes.
     *
     * Override to handle changes in the push subscription status, such as updates to push preferences
     * or the receipt of a new push token.
     *
     * @param context The context in which the receiver is running.
     * @param subscription The updated [NotificarePushSubscription], or `null` if the subscription token is unavailable.
     */
    protected open fun onSubscriptionChanged(context: Context, subscription: NotificarePushSubscription?) {
        logger.debug(
            "The subscription changed, please override onSubscriptionChanged if you want to receive these intents."
        )
    }

    /**
     * Called when the device's push token changes.
     *
     * This method is deprecated in favor of [onSubscriptionChanged]. Override to handle token changes for
     * compatibility with older implementations.
     *
     * @param context The context in which the receiver is running.
     * @param token The updated push token.
     */
    @Deprecated(
        message = "Use onSubscriptionChanged() instead.",
        replaceWith = ReplaceWith("onSubscriptionChanged(context, subscription)")
    )
    protected open fun onTokenChanged(context: Context, token: String) {
        logger.debug(
            "The push token changed, please override onTokenChanged if you want to receive these intents."
        )
    }

    /**
     * Called when a push notification is received.
     *
     * Override to execute additional actions when a [NotificareNotification] is received as indicated by the specified
     * [NotificareNotificationDeliveryMechanism].
     *
     * @param context The context in which the receiver is running.
     * @param notification The received [NotificareNotification] object.
     * @param deliveryMechanism The mechanism used to deliver the notification.
     */
    protected open fun onNotificationReceived(
        context: Context,
        notification: NotificareNotification,
        deliveryMechanism: NotificareNotificationDeliveryMechanism,
    ) {
        logger.info(
            "Received a notification, please override onNotificationReceived if you want to receive these intents."
        )
    }

    /**
     * Called when a system notification is received.
     *
     * Override to handle incoming [NotificareSystemNotification], typically used for internal or system-related
     * notifications managed by the SDK.
     *
     * @param context The context in which the receiver is running.
     * @param notification The received [NotificareSystemNotification].
     */
    protected open fun onSystemNotificationReceived(context: Context, notification: NotificareSystemNotification) {
        logger.info(
            "Received a system notification, please override onSystemNotificationReceived if you want to receive these intents."
        )
    }

    /**
     * Called when an unknown type of notification is received.
     *
     * Override to handle notifications that do not match predefined types. This can be useful for processing
     * custom or unsupported notification formats.
     *
     * @param context The context in which the receiver is running.
     * @param notification The received [NotificareUnknownNotification].
     */
    protected open fun onUnknownNotificationReceived(context: Context, notification: NotificareUnknownNotification) {
        logger.info(
            "Received an unknown notification, please override onUnknownNotificationReceived if you want to receive these intents."
        )
    }

    /**
     * Called when a push notification is opened by the user.
     *
     * Override to handle the event when a [NotificareNotification] is opened, enabling custom behavior
     * or redirection upon notification interaction.
     *
     * @param context The context in which the receiver is running.
     * @param notification The [NotificareNotification] that was opened.
     */
    protected open fun onNotificationOpened(context: Context, notification: NotificareNotification) {
        logger.debug(
            "Opened a notification, please override onNotificationOpened if you want to receive these intents."
        )
    }

    /**
     * Called when a push notification action is opened by the user.
     *
     * Override to handle specific actions associated with a [NotificareNotification], such as custom actions
     * or URL redirections within the app.
     *
     * @param context The context in which the receiver is running.
     * @param notification The [NotificareNotification] containing the action.
     * @param action The specific action opened by the user.
     */
    protected open fun onActionOpened(
        context: Context,
        notification: NotificareNotification,
        action: NotificareNotification.Action
    ) {
        logger.debug(
            "Opened a notification action, please override onActionOpened if you want to receive these intents."
        )
    }

    /**
     * Called when a live activity update is received.
     *
     * Override to handle updates to live activities, represented by the [NotificareLiveActivityUpdate].
     * This can include changes in ongoing activities that are dynamically updated.
     *
     * @param context The context in which the receiver is running.
     * @param update The received [NotificareLiveActivityUpdate].
     */
    protected open fun onLiveActivityUpdate(context: Context, update: NotificareLiveActivityUpdate) {
        logger.debug(
            "Received a live activity update, please override onLiveActivityUpdate if you want to receive these intents."
        )
    }
}
