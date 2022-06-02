package re.notifica.push.hms.ktx

import com.huawei.hms.push.RemoteMessage
import re.notifica.Notificare
import re.notifica.push.NotificareInternalPush
import re.notifica.push.NotificarePush
import re.notifica.push.ktx.push

@Suppress("unused")
public fun NotificarePush.isNotificareNotification(remoteMessage: RemoteMessage): Boolean {
    return remoteMessage.dataOfMap?.get("x-sender") == "notificare"
}

internal fun Notificare.pushInternal(): NotificareInternalPush {
    return push() as NotificareInternalPush
}
