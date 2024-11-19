package re.notifica.push.ui

import android.app.Activity
import re.notifica.Notificare
import re.notifica.models.NotificareNotification
import re.notifica.push.ui.ktx.pushUI

public object NotificarePushUICompat {

    /**
     * Specifies the class used to present notifications.
     *
     * This property defines the activity class that will be used to present notifications to the user.
     * The class must extend [NotificationActivity].
     */
    @JvmStatic
    public var notificationActivity: Class<out NotificationActivity>
        get() = Notificare.pushUI().notificationActivity
        set(value) {
            Notificare.pushUI().notificationActivity = value
        }

    /**
     * Adds a lifecycle listener for notifications.
     *
     * The listener will receive various callbacks related to the presentation and handling of notifications,
     * such as when a notification is about to be presented, presented, or when an action is executed.
     * The listener must implement [NotificarePushUI.NotificationLifecycleListener].
     *
     * @param listener The [NotificarePushUI.NotificationLifecycleListener] to add for receiving notification lifecycle
     * events.
     */
    @JvmStatic
    public fun addLifecycleListener(listener: NotificarePushUI.NotificationLifecycleListener) {
        Notificare.pushUI().addLifecycleListener(listener)
    }

    /**
     * Removes a previously added notification lifecycle listener.
     *
     * Use this method to stop receiving notification lifecycle events for the specified listener.
     *
     * @param listener The [NotificarePushUI.NotificationLifecycleListener] to remove.
     */
    @JvmStatic
    public fun removeLifecycleListener(listener: NotificarePushUI.NotificationLifecycleListener) {
        Notificare.pushUI().removeLifecycleListener(listener)
    }

    /**
     * Presents a notification to the user.
     *
     * This method launches the UI for displaying the provided [NotificareNotification] in the specified activity.
     * It triggers lifecycle events like `onNotificationWillPresent` and `onNotificationPresented` in the
     * [NotificarePushUI.NotificationLifecycleListener].
     *
     * @param activity The [Activity] from which the notification will be presented.
     * @param notification The [NotificareNotification] to present.
     */
    @JvmStatic
    public fun presentNotification(activity: Activity, notification: NotificareNotification) {
        Notificare.pushUI().presentNotification(activity, notification)
    }

    /**
     * Presents an action associated with a notification.
     *
     * This method presents the UI for executing a specific [NotificareNotification.Action] associated with the
     * provided [NotificareNotification]. It triggers lifecycle events such as `onActionWillExecute` and
     * `onActionExecuted` in the [NotificarePushUI.NotificationLifecycleListener].
     *
     * @param activity The [Activity] from which the action will be executed.
     * @param notification The [NotificareNotification] to present.
     * @param action The [NotificareNotification.Action] to execute.
     */
    @JvmStatic
    public fun presentAction(
        activity: Activity,
        notification: NotificareNotification,
        action: NotificareNotification.Action,
    ) {
        Notificare.pushUI().presentAction(activity, notification, action)
    }
}
