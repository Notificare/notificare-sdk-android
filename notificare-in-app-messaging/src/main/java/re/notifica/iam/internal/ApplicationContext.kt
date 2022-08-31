package re.notifica.iam.internal

import re.notifica.iam.models.NotificareInAppMessage

internal enum class ApplicationContext {
    LAUNCH,
    FOREGROUND;

    val rawValue: String
        get() = when (this) {
            LAUNCH -> NotificareInAppMessage.CONTEXT_LAUNCH
            FOREGROUND -> NotificareInAppMessage.CONTEXT_FOREGROUND
        }

    override fun toString(): String {
        return rawValue
    }
}
