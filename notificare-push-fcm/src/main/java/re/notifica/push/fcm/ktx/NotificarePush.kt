package re.notifica.push.fcm.ktx

import com.google.firebase.messaging.RemoteMessage
import re.notifica.push.NotificarePush

@Suppress("unused")
public fun NotificarePush.isNotificareNotification(remoteMessage: RemoteMessage): Boolean {
    return remoteMessage.data["x-sender"] == "notificare"
}
