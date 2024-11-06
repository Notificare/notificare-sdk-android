package re.notifica.push

import android.content.Intent
import androidx.lifecycle.LiveData
import com.google.firebase.messaging.RemoteMessage
import re.notifica.Notificare
import re.notifica.NotificareCallback
import re.notifica.push.ktx.INTENT_ACTION_ACTION_OPENED
import re.notifica.push.ktx.INTENT_ACTION_NOTIFICATION_OPENED
import re.notifica.push.ktx.INTENT_ACTION_NOTIFICATION_RECEIVED
import re.notifica.push.ktx.INTENT_ACTION_QUICK_RESPONSE
import re.notifica.push.ktx.INTENT_ACTION_REMOTE_MESSAGE_OPENED
import re.notifica.push.ktx.INTENT_ACTION_SYSTEM_NOTIFICATION_RECEIVED
import re.notifica.push.ktx.INTENT_ACTION_UNKNOWN_NOTIFICATION_RECEIVED
import re.notifica.push.ktx.INTENT_EXTRA_DELIVERY_MECHANISM
import re.notifica.push.ktx.INTENT_EXTRA_LIVE_ACTIVITY_UPDATE
import re.notifica.push.ktx.INTENT_EXTRA_REMOTE_MESSAGE
import re.notifica.push.ktx.INTENT_EXTRA_TEXT_RESPONSE
import re.notifica.push.ktx.INTENT_EXTRA_TOKEN
import re.notifica.push.ktx.push
import re.notifica.push.models.NotificareNotificationActionOpenedIntentResult
import re.notifica.push.models.NotificareNotificationOpenedIntentResult
import re.notifica.push.models.NotificarePushSubscription
import re.notifica.push.models.NotificareTransport

public object NotificarePushCompat {

    // region Intent actions

    @JvmField
    public val INTENT_ACTION_REMOTE_MESSAGE_OPENED: String =
        Notificare.INTENT_ACTION_REMOTE_MESSAGE_OPENED

    @JvmField
    public val INTENT_ACTION_NOTIFICATION_RECEIVED: String =
        Notificare.INTENT_ACTION_NOTIFICATION_RECEIVED

    @JvmField
    public val INTENT_ACTION_SYSTEM_NOTIFICATION_RECEIVED: String =
        Notificare.INTENT_ACTION_SYSTEM_NOTIFICATION_RECEIVED

    @JvmField
    public val INTENT_ACTION_UNKNOWN_NOTIFICATION_RECEIVED: String =
        Notificare.INTENT_ACTION_UNKNOWN_NOTIFICATION_RECEIVED

    @JvmField
    public val INTENT_ACTION_NOTIFICATION_OPENED: String =
        Notificare.INTENT_ACTION_NOTIFICATION_OPENED

    @JvmField
    public val INTENT_ACTION_ACTION_OPENED: String = Notificare.INTENT_ACTION_ACTION_OPENED

    @JvmField
    public val INTENT_ACTION_QUICK_RESPONSE: String = Notificare.INTENT_ACTION_QUICK_RESPONSE

    // endregion

    // region Intent extras

    @JvmField
    public val INTENT_EXTRA_TOKEN: String = Notificare.INTENT_EXTRA_TOKEN

    @JvmField
    public val INTENT_EXTRA_REMOTE_MESSAGE: String = Notificare.INTENT_EXTRA_REMOTE_MESSAGE

    @JvmField
    public val INTENT_EXTRA_TEXT_RESPONSE: String = Notificare.INTENT_EXTRA_TEXT_RESPONSE

    @JvmField
    public val INTENT_EXTRA_LIVE_ACTIVITY_UPDATE: String = Notificare.INTENT_EXTRA_LIVE_ACTIVITY_UPDATE

    @JvmField
    public val INTENT_EXTRA_DELIVERY_MECHANISM: String = Notificare.INTENT_EXTRA_DELIVERY_MECHANISM

    // endregion

    /**
     * Specifies the intent receiver class for handling push intents.
     *
     * This property defines the class that will receive and process the intents related to push notifications.
     * The class must extend [NotificarePushIntentReceiver].
     */
    @JvmStatic
    public var intentReceiver: Class<out NotificarePushIntentReceiver>
        get() = Notificare.push().intentReceiver
        set(value) {
            Notificare.push().intentReceiver = value
        }

    /**
     * Indicates whether remote notifications are enabled.
     *
     * This property returns `true` if remote notifications are enabled for the application, and `false` otherwise.
     */
    @JvmStatic
    public val hasRemoteNotificationsEnabled: Boolean
        get() = Notificare.push().hasRemoteNotificationsEnabled

    /**
     * Provides the current push transport information.
     *
     * This property returns the [NotificareTransport] assigned to the device.
     */
    @JvmStatic
    public val transport: NotificareTransport?
        get() = Notificare.push().transport

    /**
     * Provides the current push subscription token.
     *
     * This property returns the [NotificarePushSubscription] object containing the device's current push subscription
     * token, or `null` if no token is available.
     */
    @JvmStatic
    public val subscription: NotificarePushSubscription?
        get() = Notificare.push().subscription

    /**
     * Provides a live data object for observing push subscription changes.
     *
     * This property returns a [LiveData] object that can be observed to track changes to the device's push subscription
     * token. It emits `null` when no token is available.
     */
    @JvmStatic
    public val observableSubscription: LiveData<NotificarePushSubscription?>
        get() = Notificare.push().observableSubscription

    /**
     * Indicates whether the device is capable of receiving remote notifications.
     *
     * This property returns `true` if the user has granted permission to receive push notifications and the device
     * has successfully obtained a push token from the notification service. It reflects whether the UI can present
     * notifications as allowed by the system and user settings.
     *
     * @return `true` if the device can receive remote notifications, `false` otherwise.
     */
    @JvmStatic
    public val allowedUI: Boolean
        get() = Notificare.push().allowedUI

    /**
     * Provides a live data object for observing changes to the allowed UI state.
     *
     * This property returns a [LiveData] object that can be observed to track changes in whether push-related UI is
     * allowed.
     */
    @JvmStatic
    public val observableAllowedUI: LiveData<Boolean> = Notificare.push().observableAllowedUI

    /**
     * Enables remote notifications with a callback.
     *
     * This method enables remote notifications for the application, allowing push notifications to be received.
     * The provided [NotificareCallback] will be invoked upon success or failure.
     *
     * **Note**: Starting with Android 13 (API level 33), this function requires the developer to explicitly request
     * the `POST_NOTIFICATIONS` permission from the user.
     *
     * @param callback The [NotificareCallback] to be invoked when the operation completes.
     */
    @JvmStatic
    public fun enableRemoteNotifications(callback: NotificareCallback<Unit>) {
        Notificare.push().enableRemoteNotifications(callback)
    }

    /**
     * Disables remote notifications with a callback.
     *
     * This method disables remote notifications for the application, preventing push notifications from being received.
     * The provided [NotificareCallback] will be invoked upon success or failure.
     *
     * @param callback The [NotificareCallback] to be invoked when the operation completes.
     */
    @JvmStatic
    public fun disableRemoteNotifications(callback: NotificareCallback<Unit>) {
        Notificare.push().disableRemoteNotifications(callback)
    }

    /**
     * Determines whether a remote message is a Notificare notification.
     *
     * This method checks if the provided [RemoteMessage] is a valid Notificare notification.
     *
     * @param remoteMessage The [RemoteMessage] to check.
     * @return `true` if the message is a Notificare notification, `false` otherwise.
     */
    @JvmStatic
    public fun isNotificareNotification(remoteMessage: RemoteMessage): Boolean {
        return Notificare.push().isNotificareNotification(remoteMessage)
    }

    /**
     * Handles a trampoline intent.
     *
     * This method processes an intent and determines if it is a Notificare push notification intent
     * that requires handling by a trampoline mechanism.
     *
     * @param intent The [Intent] to handle.
     * @return `true` if the intent was handled, `false` otherwise.
     */
    @JvmStatic
    public fun handleTrampolineIntent(intent: Intent): Boolean {
        return Notificare.push().handleTrampolineIntent(intent)
    }

    /**
     * Parses an intent to retrieve information about an opened notification.
     *
     * This method extracts the details of a notification that was opened from the provided [Intent].
     *
     * @param intent The [Intent] representing the notification opened event.
     * @return A [NotificareNotificationOpenedIntentResult] containing the details, or `null` if parsing failed.
     */
    @JvmStatic
    public fun parseNotificationOpenedIntent(intent: Intent): NotificareNotificationOpenedIntentResult? {
        return Notificare.push().parseNotificationOpenedIntent(intent)
    }

    /**
     * Parses an intent to retrieve information about an opened notification action.
     *
     * This method extracts the details of a notification action that was opened from the provided [Intent].
     *
     * @param intent The [Intent] representing the notification action opened event.
     * @return A [NotificareNotificationActionOpenedIntentResult] containing the details, or `null` if parsing failed.
     */
    @JvmStatic
    public fun parseNotificationActionOpenedIntent(intent: Intent): NotificareNotificationActionOpenedIntentResult? {
        return Notificare.push().parseNotificationActionOpenedIntent(intent)
    }

    /**
     * Registers a live activity with optional topics and a callback.
     *
     * This method registers a live activity identified by the provided `activityId`, optionally categorizing it with a
     * list of topics.
     * The provided [NotificareCallback] will be invoked upon success or failure.
     *
     * @param activityId The ID of the live activity to register.
     * @param topics A list of topics to subscribe to (optional).
     * @param callback The [NotificareCallback] to be invoked when the operation completes.
     */
    @JvmStatic
    public fun registerLiveActivity(
        activityId: String,
        topics: List<String> = listOf(),
        callback: NotificareCallback<Unit>,
    ) {
        Notificare.push().registerLiveActivity(activityId, topics, callback)
    }

    /**
     * Ends a live activity with a callback.
     *
     * This method ends the live activity identified by the provided `activityId`. The provided [NotificareCallback]
     * will be invoked upon success or failure.
     *
     * @param activityId The ID of the live activity to end.
     * @param callback The [NotificareCallback] to be invoked when the operation completes.
     */
    @JvmStatic
    public fun endLiveActivity(activityId: String, callback: NotificareCallback<Unit>) {
        Notificare.push().endLiveActivity(activityId, callback)
    }
}
