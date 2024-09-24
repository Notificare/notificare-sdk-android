package re.notifica.push.ui

import android.content.res.Resources.NotFoundException
import androidx.annotation.ColorInt
import re.notifica.Notificare
import re.notifica.internal.NotificareOptions
import re.notifica.push.ui.internal.logger

public val NotificareOptions.closeWindowQueryParameter: String
    get() {
        return metadata.getString("re.notifica.push.ui.close_window_query_parameter", "notificareCloseWindow")
    }

public val NotificareOptions.openActionsQueryParameter: String
    get() {
        return metadata.getString("re.notifica.push.ui.open_actions_query_parameter", "notificareOpenActions")
    }

public val NotificareOptions.openActionQueryParameter: String
    get() {
        return metadata.getString("re.notifica.push.ui.open_action_query_parameter", "notificareOpenAction")
    }

public val NotificareOptions.urlSchemes: List<String>
    get() {
        val resource = metadata.getInt("re.notifica.push.ui.notification_url_schemes")

        if (resource != 0) {
            try {
                return Notificare.requireContext().resources.getStringArray(resource).asList()
            } catch (e: NotFoundException) {
                logger.warning("Could not load the URL schemes.", e)
            }
        }

        return emptyList()
    }

public val NotificareOptions.showNotificationProgress: Boolean
    get() {
        return metadata.getBoolean("re.notifica.push.ui.show_notification_progress", true)
    }

public val NotificareOptions.showNotificationToasts: Boolean
    get() {
        return metadata.getBoolean("re.notifica.push.ui.show_notification_toasts", false)
    }

public val NotificareOptions.customTabsShowTitle: Boolean
    get() {
        return metadata.getBoolean("re.notifica.push.ui.custom_tabs_show_title", true)
    }

public val NotificareOptions.customTabsColorScheme: String?
    get() {
        return metadata.getString("re.notifica.push.ui.custom_tabs_color_scheme", null)
    }

@get:ColorInt
public val NotificareOptions.customTabsToolbarColor: Int?
    get() {
        val resource = metadata.getInt("re.notifica.push.ui.custom_tabs_toolbar_color")

        if (resource != 0) {
            try {
                return Notificare.requireContext().getColor(resource)
            } catch (e: NotFoundException) {
                logger.warning(
                    "Invalid color resource provided for 're.notifica.push.ui.custom_tabs_toolbar_color'.",
                    e
                )
            }
        }

        return null
    }

@get:ColorInt
public val NotificareOptions.customTabsNavigationBarColor: Int?
    get() {
        val resource = metadata.getInt("re.notifica.push.ui.custom_tabs_navigation_bar_color")

        if (resource != 0) {
            try {
                return Notificare.requireContext().getColor(resource)
            } catch (e: NotFoundException) {
                logger.warning(
                    "Invalid color resource provided for 're.notifica.push.ui.custom_tabs_navigation_bar_color'.",
                    e
                )
            }
        }

        return null
    }

@get:ColorInt
public val NotificareOptions.customTabsNavigationBarDividerColor: Int?
    get() {
        val resource = metadata.getInt("re.notifica.push.ui.custom_tabs_navigation_bar_divider_color")

        if (resource != 0) {
            try {
                return Notificare.requireContext().getColor(resource)
            } catch (e: NotFoundException) {
                logger.warning(
                    "Invalid color resource provided for 're.notifica.push.ui.custom_tabs_navigation_bar_divider_color'.",
                    e
                )
            }
        }

        return null
    }
