package re.notifica.push.ui

import android.content.res.Resources.NotFoundException
import re.notifica.Notificare
import re.notifica.NotificareLogger
import re.notifica.internal.NotificareOptions

val NotificareOptions.closeWindowQueryParameter: String
    get() {
        return info.metaData?.getString(
            "re.notifica.push.ui.close_window_query_parameter",
            null,
        ) ?: "notificareCloseWindow"
    }

val NotificareOptions.openActionsQueryParameter: String
    get() {
        return info.metaData.getString(
            "re.notifica.push.ui.open_actions_query_parameter",
            null,
        ) ?: "notificareOpenActions"
    }

val NotificareOptions.openActionQueryParameter: String
    get() {
        return info.metaData.getString(
            "re.notifica.push.ui.open_action_query_parameter",
            null,
        ) ?: "notificareOpenAction"
    }

val NotificareOptions.urlSchemes: List<String>
    get() {
        val resource = info.metaData?.getInt("re.notifica.metadata.UrlSchemes")

        if (resource != null && resource != 0) {
            try {
                return Notificare.requireContext().resources.getStringArray(resource).asList()
            } catch (e: NotFoundException) {
                NotificareLogger.warning("Could not load the URL schemes.", e)
            }
        }

        return emptyList()
    }

val NotificareOptions.showNotificationProgress: Boolean
    get() {
        return info.metaData?.getBoolean(
            "re.notifica.push.ui.show_notification_progress",
            true,
        ) ?: true
    }

val NotificareOptions.showNotificationToasts: Boolean
    get() {
        return info.metaData?.getBoolean(
            "re.notifica.push.ui.show_notification_toasts",
            false,
        ) ?: false
    }
