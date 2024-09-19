package re.notifica.iam

import androidx.annotation.MainThread
import re.notifica.Notificare
import re.notifica.iam.models.NotificareInAppMessage
import re.notifica.utilities.NotificareLogger

private val logger = NotificareLogger(
    Notificare.options?.debugLoggingEnabled ?: false,
    "NotificareInAppMessaging"
)

public interface NotificareInAppMessaging {

    public var hasMessagesSuppressed: Boolean

    public fun setMessagesSuppressed(suppressed: Boolean, evaluateContext: Boolean)

    public fun addLifecycleListener(listener: MessageLifecycleListener)

    public fun removeLifecycleListener(listener: MessageLifecycleListener)

    public interface MessageLifecycleListener {
        @MainThread
        public fun onMessagePresented(message: NotificareInAppMessage) {
            logger.debug(
                "Message presented, please override onMessagePresented if you want to receive these events."
            )
        }

        @MainThread
        public fun onMessageFinishedPresenting(message: NotificareInAppMessage) {
            logger.debug(
                "Message finished presenting, please override onMessageFinishedPresenting if you want to receive these events."
            )
        }

        @MainThread
        public fun onMessageFailedToPresent(message: NotificareInAppMessage) {
            logger.debug(
                "Message failed to present, please override onMessageFailedToPresent if you want to receive these events."
            )
        }

        @MainThread
        public fun onActionExecuted(message: NotificareInAppMessage, action: NotificareInAppMessage.Action) {
            logger.debug(
                "Action executed, please override onActionExecuted if you want to receive these events."
            )
        }

        @MainThread
        public fun onActionFailedToExecute(
            message: NotificareInAppMessage,
            action: NotificareInAppMessage.Action,
            error: Exception?,
        ) {
            logger.debug(
                "Action failed to execute, please override onActionFailedToExecute if you want to receive these events."
            )
        }
    }
}
