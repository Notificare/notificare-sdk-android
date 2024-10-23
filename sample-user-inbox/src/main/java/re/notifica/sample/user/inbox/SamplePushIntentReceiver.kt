package re.notifica.sample.user.inbox

import android.content.Context
import re.notifica.models.NotificareNotification
import re.notifica.push.NotificarePushIntentReceiver
import re.notifica.push.models.NotificareNotificationDeliveryMechanism
import re.notifica.sample.user.inbox.core.NotificationEvent

class SamplePushIntentReceiver : NotificarePushIntentReceiver() {
    override fun onNotificationReceived(
        context: Context,
        notification: NotificareNotification,
        deliveryMechanism: NotificareNotificationDeliveryMechanism
    ) {
        super.onNotificationReceived(context, notification, deliveryMechanism)

        NotificationEvent.triggerInboxShouldUpdateEvent()
    }

    override fun onNotificationOpened(context: Context, notification: NotificareNotification) {
        super.onNotificationOpened(context, notification)

        NotificationEvent.triggerInboxShouldUpdateEvent()
    }
}
