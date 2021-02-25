package re.notifica.push.ui

import android.app.Activity
import android.content.Intent
import re.notifica.Notificare
import re.notifica.models.NotificareNotification
import re.notifica.push.NotificarePush

object NotificarePushUI {

    fun presentNotification(activity: Activity, notification: NotificareNotification) {
        val intent = Intent(Notificare.requireContext(), NotificationActivity::class.java)
            .putExtra(NotificarePush.INTENT_EXTRA_NOTIFICATION, notification)
//            .putExtra(Notificare.INTENT_EXTRA_ACTION, action)
//            .setPackage(Notificare.requireContext().packageName)

        activity.startActivity(intent)
        activity.overridePendingTransition(0, 0)
    }
}
