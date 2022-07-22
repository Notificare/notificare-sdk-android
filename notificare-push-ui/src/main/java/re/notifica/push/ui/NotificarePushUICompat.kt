package re.notifica.push.ui

import android.app.Activity
import re.notifica.Notificare
import re.notifica.models.NotificareNotification
import re.notifica.push.ui.ktx.pushUI

public object NotificarePushUICompat {

    @JvmStatic
    public var notificationActivity: Class<out NotificationActivity>
        get() = Notificare.pushUI().notificationActivity
        set(value) {
            Notificare.pushUI().notificationActivity = value
        }

    @JvmStatic
    public fun addLifecycleListener(listener: NotificarePushUI.NotificationLifecycleListener) {
        Notificare.pushUI().addLifecycleListener(listener)
    }

    @JvmStatic
    public fun removeLifecycleListener(listener: NotificarePushUI.NotificationLifecycleListener) {
        Notificare.pushUI().removeLifecycleListener(listener)
    }

    @JvmStatic
    public fun presentNotification(activity: Activity, notification: NotificareNotification) {
        Notificare.pushUI().presentNotification(activity, notification)
    }

    @JvmStatic
    public fun presentAction(
        activity: Activity,
        notification: NotificareNotification,
        action: NotificareNotification.Action,
    ) {
        Notificare.pushUI().presentAction(activity, notification, action)
    }
}
