package re.notifica.iam

import re.notifica.Notificare
import re.notifica.iam.ktx.inAppMessaging

public object NotificareInAppMessagingCompat {

    @JvmStatic
    public var hasMessagesSuppressed: Boolean
        get() = Notificare.inAppMessaging().hasMessagesSuppressed
        set(value) {
            Notificare.inAppMessaging().hasMessagesSuppressed = value
        }

    @JvmStatic
    public fun setMessagesSuppressed(suppressed: Boolean, evaluateContext: Boolean) {
        Notificare.inAppMessaging().setMessagesSuppressed(suppressed, evaluateContext)
    }

    @JvmStatic
    public fun addLifecycleListener(listener: NotificareInAppMessaging.MessageLifecycleListener) {
        Notificare.inAppMessaging().addLifecycleListener(listener)
    }

    @JvmStatic
    public fun removeLifecycleListener(
        listener: NotificareInAppMessaging.MessageLifecycleListener
    ) {
        Notificare.inAppMessaging().removeLifecycleListener(listener)
    }
}
