package re.notifica.iam

import re.notifica.Notificare
import re.notifica.iam.ktx.inAppMessaging

public object NotificareInAppMessagingCompat {

    /**
     * Indicates whether in-app messages are currently suppressed.
     *
     * If `true`, message dispatching and the presentation of in-app messages are temporarily suppressed.
     * When `false`, in-app messages are allowed to be presented.
     */
    @JvmStatic
    public var hasMessagesSuppressed: Boolean
        get() = Notificare.inAppMessaging().hasMessagesSuppressed
        set(value) {
            Notificare.inAppMessaging().hasMessagesSuppressed = value
        }

    /**
     * Sets the message suppression state.
     *
     * When messages are suppressed, in-app messages will not be presented to the user.
     * By default, stopping the in-app message suppression does not re-evaluate the foreground context.
     *
     * To trigger a new context evaluation after stopping in-app message suppression, set the `evaluateContext`
     * parameter to `true`.
     *
     * @param suppressed Set to `true` to suppress in-app messages, or `false` to stop suppressing them.
     * @param evaluateContext Set to `true` to re-evaluate the foreground context when stopping in-app message
     * suppression,
     */
    @JvmStatic
    public fun setMessagesSuppressed(suppressed: Boolean, evaluateContext: Boolean) {
        Notificare.inAppMessaging().setMessagesSuppressed(suppressed, evaluateContext)
    }

    /**
     * Adds a [NotificareInAppMessaging.MessageLifecycleListener] to monitor the lifecycle of in-app messages.
     *
     * @param listener The [NotificareInAppMessaging.MessageLifecycleListener] to be added for lifecycle event
     * notifications.
     *
     * @see [NotificareInAppMessaging.MessageLifecycleListener]
     */
    @JvmStatic
    public fun addLifecycleListener(listener: NotificareInAppMessaging.MessageLifecycleListener) {
        Notificare.inAppMessaging().addLifecycleListener(listener)
    }

    /**
     * Removes a previously added [NotificareInAppMessaging.MessageLifecycleListener].
     *
     * @param listener The [NotificareInAppMessaging.MessageLifecycleListener] to be removed from the lifecycle
     * notifications.
     */
    @JvmStatic
    public fun removeLifecycleListener(listener: NotificareInAppMessaging.MessageLifecycleListener) {
        Notificare.inAppMessaging().removeLifecycleListener(listener)
    }
}
