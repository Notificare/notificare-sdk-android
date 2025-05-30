package re.notifica.push

import android.content.Intent
import androidx.lifecycle.LiveData
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.flow.StateFlow
import re.notifica.InternalNotificareApi
import re.notifica.NotificareCallback
import re.notifica.push.models.NotificareNotificationActionOpenedIntentResult
import re.notifica.push.models.NotificareNotificationOpenedIntentResult
import re.notifica.push.models.NotificarePushSubscription
import re.notifica.push.models.NotificareTransport
import re.notifica.push.models.NotificareRemoteMessage

public interface NotificarePush {

    /**
     * Specifies the intent receiver class for handling push intents.
     *
     * This property defines the class that will receive and process the intents related to push notifications.
     * The class must extend [NotificarePushIntentReceiver].
     */
    public var intentReceiver: Class<out NotificarePushIntentReceiver>

    /**
     * Indicates whether remote notifications are enabled.
     *
     * This property returns `true` if remote notifications are enabled for the application, and `false` otherwise.
     */
    public val hasRemoteNotificationsEnabled: Boolean

    /**
     * Provides the current push transport information.
     *
     * This property returns the [NotificareTransport] assigned to the device.
     */
    public val transport: NotificareTransport?

    /**
     * Provides the current push subscription token.
     *
     * This property returns the [NotificarePushSubscription] object containing the device's current push subscription
     * token, or `null` if no token is available.
     */
    public val subscription: NotificarePushSubscription?

    /**
     * Provides a [LiveData] object for observing push subscription changes.
     *
     * This property returns a [LiveData] object that can be observed to track changes to the device's push subscription
     * token. The initial value is `null`, and it emits the token once it becomes available.
     */
    public val observableSubscription: LiveData<NotificarePushSubscription?>

    /**
     * Provides a [StateFlow] for observing push subscription changes.
     *
     * This property returns a [StateFlow] that can be collected to track changes to the device's push subscription
     * token. The initial value is `null`, and it emits the token once it becomes available.
     */
    public val subscriptionStream: StateFlow<NotificarePushSubscription?>

    /**
     * Indicates whether the device is capable of receiving remote notifications.
     *
     * This property returns `true` if the user has granted permission to receive push notifications and the device
     * has successfully obtained a push token from the notification service. It reflects whether the app can present
     * notifications as allowed by the system and user settings.
     *
     * @return `true` if the device can receive remote notifications, `false` otherwise.
     */
    public val allowedUI: Boolean

    /**
     * Provides a [LiveData] object for observing changes to the `allowedUI` state.
     *
     * This property returns a [LiveData] representation of [allowedUI] and can be observed to track any changes
     * to whether the device can receive remote notifications.
     */
    public val observableAllowedUI: LiveData<Boolean>

    /**
     * Provides a [StateFlow] for observing changes to the `allowedUI` state.
     *
     * This property returns a [StateFlow] representation of [allowedUI] and can be collected to track any changes
     * to whether the device can receive remote notifications.
     */
    public val allowedUIStream: StateFlow<Boolean>

    /**
     * Enables remote notifications.
     *
     * This suspending function enables remote notifications for the application, allowing push notifications to be
     * received.
     *
     * **Note**: Starting with Android 13 (API level 33), this function requires the developer to explicitly request
     * the `POST_NOTIFICATIONS` permission from the user.
     */
    public suspend fun enableRemoteNotifications()

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
    public fun enableRemoteNotifications(callback: NotificareCallback<Unit>)

    /**
     * Disables remote notifications.
     *
     * This suspending function disables remote notifications for the application, preventing push notifications from
     * being received.
     */
    public suspend fun disableRemoteNotifications()

    /**
     * Disables remote notifications with a callback.
     *
     * This method disables remote notifications for the application, preventing push notifications from being received.
     * The provided [NotificareCallback] will be invoked upon success or failure.
     *
     * @param callback The [NotificareCallback] to be invoked when the operation completes.
     */
    public fun disableRemoteNotifications(callback: NotificareCallback<Unit>)

    /**
     * Determines whether a remote message is a Notificare notification.
     *
     * This method checks if the provided [RemoteMessage] is a valid Notificare notification.
     *
     * @param remoteMessage The [RemoteMessage] to check.
     * @return `true` if the message is a Notificare notification, `false` otherwise.
     */
    public fun isNotificareNotification(remoteMessage: RemoteMessage): Boolean

    /**
     * Handles a trampoline intent.
     *
     * This method processes an intent and determines if it is a Notificare push notification intent
     * that requires handling by a trampoline mechanism.
     *
     * @param intent The [Intent] to handle.
     * @return `true` if the intent was handled, `false` otherwise.
     */
    public fun handleTrampolineIntent(intent: Intent): Boolean

    /**
     * Parses an intent to retrieve information about an opened notification.
     *
     * This method extracts the details of a notification that was opened from the provided [Intent].
     *
     * @param intent The [Intent] representing the notification opened event.
     * @return A [NotificareNotificationOpenedIntentResult] containing the details, or `null` if parsing failed.
     */
    public fun parseNotificationOpenedIntent(intent: Intent): NotificareNotificationOpenedIntentResult?

    /**
     * Parses an intent to retrieve information about an opened notification action.
     *
     * This method extracts the details of a notification action that was opened from the provided [Intent].
     *
     * @param intent The [Intent] representing the notification action opened event.
     * @return A [NotificareNotificationActionOpenedIntentResult] containing the details, or `null` if parsing failed.
     */
    public fun parseNotificationActionOpenedIntent(intent: Intent): NotificareNotificationActionOpenedIntentResult?

    /**
     * Registers a live activity with optional topics.
     *
     * This suspending function registers a live activity identified by the provided `activityId`, optionally
     * categorizing it with a list of topics.
     * Live activities represent ongoing actions that can be updated in real-time.
     *
     * @param activityId The ID of the live activity to register.
     * @param topics A list of topics to subscribe to (optional).
     */
    public suspend fun registerLiveActivity(activityId: String, topics: List<String> = listOf())

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
    public fun registerLiveActivity(
        activityId: String,
        topics: List<String> = listOf(),
        callback: NotificareCallback<Unit>,
    )

    /**
     * Ends a live activity.
     *
     * This suspending function ends the live activity identified by the provided `activityId`.
     *
     * @param activityId The ID of the live activity to end.
     */
    public suspend fun endLiveActivity(activityId: String)

    /**
     * Ends a live activity with a callback.
     *
     * This method ends the live activity identified by the provided `activityId`. The provided [NotificareCallback]
     * will be invoked upon success or failure.
     *
     * @param activityId The ID of the live activity to end.
     * @param callback The [NotificareCallback] to be invoked when the operation completes.
     */
    public fun endLiveActivity(activityId: String, callback: NotificareCallback<Unit>)
}

internal interface NotificareInternalPush {

    @InternalNotificareApi
    fun handleNewToken(transport: NotificareTransport, token: String)

    @InternalNotificareApi
    fun handleRemoteMessage(message: NotificareRemoteMessage)
}
