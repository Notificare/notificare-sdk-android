package re.notifica.iam

import androidx.annotation.MainThread
import re.notifica.iam.models.NotificareInAppMessage
import re.notifica.internal.NotificareLogger

public interface NotificareInAppMessaging {

    public var hasMessagesSuppressed: Boolean

    public fun addLifecycleListener(listener: MessageLifecycleListener)

    public fun removeLifecycleListener(listener: MessageLifecycleListener)


    public interface MessageLifecycleListener {
        @MainThread
        public fun onMessagePresented(message: NotificareInAppMessage) {
            NotificareLogger.debug("Message presented, please override onMessagePresented if you want to receive these events.")
        }

        @MainThread
        public fun onMessageFinishedPresenting(message: NotificareInAppMessage) {
            NotificareLogger.debug("Message finished presenting, please override onMessageFinishedPresenting if you want to receive these events.")
        }

        @MainThread
        public fun onMessageFailedToPresent(message: NotificareInAppMessage) {
            NotificareLogger.debug("Message failed to present, please override onMessageFailedToPresent if you want to receive these events.")
        }

        @MainThread
        public fun onActionExecuted(message: NotificareInAppMessage, action: NotificareInAppMessage.Action) {
            NotificareLogger.debug("Action executed, please override onActionExecuted if you want to receive these events.")
        }

        @MainThread
        public fun onActionFailedToExecute(
            message: NotificareInAppMessage,
            action: NotificareInAppMessage.Action,
            error: Exception?,
        ) {
            NotificareLogger.debug("Action failed to execute, please override onActionFailedToExecute if you want to receive these events.")
        }
    }
}
