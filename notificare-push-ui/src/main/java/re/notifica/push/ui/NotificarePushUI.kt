package re.notifica.push.ui

import android.app.Activity
import android.net.Uri
import androidx.annotation.MainThread
import re.notifica.InternalNotificareApi
import re.notifica.internal.NotificareLogger
import re.notifica.models.NotificareNotification

public interface NotificarePushUI {

    public var notificationActivity: Class<out NotificationActivity>

    public fun addLifecycleListener(listener: NotificationLifecycleListener)

    public fun removeLifecycleListener(listener: NotificationLifecycleListener)

    public fun presentNotification(activity: Activity, notification: NotificareNotification)

    public fun presentAction(
        activity: Activity,
        notification: NotificareNotification,
        action: NotificareNotification.Action,
    )

    public interface NotificationLifecycleListener {
        @MainThread
        public fun onNotificationWillPresent(notification: NotificareNotification) {
            NotificareLogger.debug(
                "Notification will present, please override onNotificationWillPresent if you want to receive these events."
            )
        }

        @MainThread
        public fun onNotificationPresented(notification: NotificareNotification) {
            NotificareLogger.debug(
                "Notification presented, please override onNotificationPresented if you want to receive these events."
            )
        }

        @MainThread
        public fun onNotificationFinishedPresenting(notification: NotificareNotification) {
            NotificareLogger.debug(
                "Notification finished presenting, please override onNotificationFinishedPresenting if you want to receive these events."
            )
        }

        @MainThread
        public fun onNotificationFailedToPresent(notification: NotificareNotification) {
            NotificareLogger.debug(
                "Notification failed to present, please override onNotificationFailedToPresent if you want to receive these events."
            )
        }

        @MainThread
        public fun onNotificationUrlClicked(notification: NotificareNotification, uri: Uri) {
            NotificareLogger.debug(
                "Notification url clicked, please override onNotificationUrlClicked if you want to receive these events."
            )
        }

        @MainThread
        public fun onActionWillExecute(notification: NotificareNotification, action: NotificareNotification.Action) {
            NotificareLogger.debug(
                "Action will execute, please override onActionWillExecute if you want to receive these events."
            )
        }

        @MainThread
        public fun onActionExecuted(notification: NotificareNotification, action: NotificareNotification.Action) {
            NotificareLogger.debug(
                "Action executed, please override onActionExecuted if you want to receive these events."
            )
        }

//        fun onActionNotExecuted(notification: NotificareNotification, action: NotificareNotification.Action) {
//            NotificareLogger.debug("Action did not execute, please override onActionNotExecuted if you want to receive these events.")
//        }

        @MainThread
        public fun onActionFailedToExecute(
            notification: NotificareNotification,
            action: NotificareNotification.Action,
            error: Exception?,
        ) {
            NotificareLogger.debug(
                "Action failed to execute, please override onActionFailedToExecute if you want to receive these events.",
                error
            )
        }

        @MainThread
        public fun onCustomActionReceived(
            notification: NotificareNotification,
            action: NotificareNotification.Action,
            uri: Uri,
        ) {
            NotificareLogger.warning(
                "Action received, please override onCustomActionReceived if you want to receive these events."
            )
        }
    }
}

public interface NotificareInternalPushUI {
    @InternalNotificareApi
    public val lifecycleListeners: List<NotificarePushUI.NotificationLifecycleListener>
}
