package re.notifica.iam

import androidx.annotation.MainThread
import re.notifica.iam.internal.logger
import re.notifica.iam.models.NotificareInAppMessage

public interface NotificareInAppMessaging {

    /**
     * Indicates whether in-app messages are currently suppressed.
     *
     * If `true`, message dispatching and the presentation of in-app messages are temporarily suppressed.
     * When `false`, in-app messages are allowed to be presented.
     */
    public var hasMessagesSuppressed: Boolean

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
     * suppression.
     */
    public fun setMessagesSuppressed(suppressed: Boolean, evaluateContext: Boolean)

    /**
     * Adds a [MessageLifecycleListener] to monitor the lifecycle of in-app messages.
     *
     * @param listener The [MessageLifecycleListener] to be added for lifecycle event notifications.
     *
     * @see [MessageLifecycleListener]
     */
    public fun addLifecycleListener(listener: MessageLifecycleListener)

    /**
     * Removes a previously added [MessageLifecycleListener].
     *
     * @param listener The [MessageLifecycleListener] to be removed from the lifecycle notifications.
     */
    public fun removeLifecycleListener(listener: MessageLifecycleListener)

    /**
     * Interface for listening to the lifecycle events of in-app messages.
     *
     * Implement this interface to receive callbacks for various stages of an in-app message's lifecycle,
     * including presentation, completion, failures, and actions performed on the message.
     */
    public interface MessageLifecycleListener {

        /**
         * Called when an in-app message is successfully presented to the user.
         *
         * @param message The [NotificareInAppMessage] that was presented.
         */
        @MainThread
        public fun onMessagePresented(message: NotificareInAppMessage) {
            logger.debug(
                "Message presented, please override onMessagePresented if you want to receive these events."
            )
        }

        /**
         * Called when the presentation of an in-app message has finished.
         *
         * This method is invoked after the message is no longer visible to the user.
         *
         * @param message The [NotificareInAppMessage] that finished presenting.
         */
        @MainThread
        public fun onMessageFinishedPresenting(message: NotificareInAppMessage) {
            logger.debug(
                "Message finished presenting, please override onMessageFinishedPresenting if you want to receive these events."
            )
        }

        /**
         * Called when an in-app message failed to present.
         *
         * @param message The [NotificareInAppMessage] that failed to be presented.
         */
        @MainThread
        public fun onMessageFailedToPresent(message: NotificareInAppMessage) {
            logger.debug(
                "Message failed to present, please override onMessageFailedToPresent if you want to receive these events."
            )
        }

        /**
         * Called when an action is successfully executed for an in-app message.
         *
         * @param message The [NotificareInAppMessage] for which the action was executed.
         * @param action The [NotificareInAppMessage.Action] that was executed.
         */
        @MainThread
        public fun onActionExecuted(message: NotificareInAppMessage, action: NotificareInAppMessage.Action) {
            logger.debug(
                "Action executed, please override onActionExecuted if you want to receive these events."
            )
        }

        /**
         * Called when an action execution failed for an in-app message.
         *
         * This method is triggered when an error occurs while attempting to execute an action.
         *
         * @param message The [NotificareInAppMessage] for which the action was attempted.
         * @param action The [NotificareInAppMessage.Action] that failed to execute.
         * @param error An optional [Exception] describing the error, or `null` if no specific error was provided.
         */
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
