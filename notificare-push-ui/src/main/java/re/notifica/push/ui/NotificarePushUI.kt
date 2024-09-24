package re.notifica.push.ui

import android.app.Activity
import android.net.Uri
import androidx.annotation.MainThread
import re.notifica.InternalNotificareApi
import re.notifica.models.NotificareNotification
import re.notifica.push.ui.internal.logger
import java.lang.ref.WeakReference

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
            logger.debug(
                "Notification will present, please override onNotificationWillPresent if you want to receive these events."
            )
        }

        @MainThread
        public fun onNotificationPresented(notification: NotificareNotification) {
            logger.debug(
                "Notification presented, please override onNotificationPresented if you want to receive these events."
            )
        }

        @MainThread
        public fun onNotificationFinishedPresenting(notification: NotificareNotification) {
            logger.debug(
                "Notification finished presenting, please override onNotificationFinishedPresenting if you want to receive these events."
            )
        }

        @MainThread
        public fun onNotificationFailedToPresent(notification: NotificareNotification) {
            logger.debug(
                "Notification failed to present, please override onNotificationFailedToPresent if you want to receive these events."
            )
        }

        @MainThread
        public fun onNotificationUrlClicked(notification: NotificareNotification, uri: Uri) {
            logger.debug(
                "Notification url clicked, please override onNotificationUrlClicked if you want to receive these events."
            )
        }

        @MainThread
        public fun onActionWillExecute(notification: NotificareNotification, action: NotificareNotification.Action) {
            logger.debug(
                "Action will execute, please override onActionWillExecute if you want to receive these events."
            )
        }

        @MainThread
        public fun onActionExecuted(notification: NotificareNotification, action: NotificareNotification.Action) {
            logger.debug(
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
            logger.debug(
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
            logger.warning(
                "Action received, please override onCustomActionReceived if you want to receive these events."
            )
        }
    }
}

internal interface NotificareInternalPushUI {
    @InternalNotificareApi
    val lifecycleListeners: List<WeakReference<NotificarePushUI.NotificationLifecycleListener>>
}
