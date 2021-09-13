package re.notifica.push.hms.ktx

import com.huawei.hms.push.RemoteMessage
import re.notifica.push.NotificarePush

@Suppress("unused")
public fun NotificarePush.isNotificareNotification(remoteMessage: RemoteMessage): Boolean {
    return remoteMessage.dataOfMap?.get("x-sender") == "notificare"
}
