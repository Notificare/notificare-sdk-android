package re.notifica.loyalty

import android.content.res.Resources
import androidx.core.content.ContextCompat
import re.notifica.Notificare
import re.notifica.internal.NotificareOptions

public val NotificareOptions.passNotificationChannel: String
    get() {
        return metadata.getString(
            "re.notifica.loyalty.pass_notification_channel",
            "notificare_channel_default"
        )
    }

public val NotificareOptions.passNotificationSmallIcon: Int
    get() {
        return metadata.getInt(
            "re.notifica.loyalty.pass_notification_small_icon",
            info.icon
        )
    }

public val NotificareOptions.passNotificationAccentColor: Int?
    get() {
        if (!metadata.containsKey("re.notifica.loyalty.pass_notification_accent_color")) return null

        val color = metadata.getInt("re.notifica.loyalty.pass_notification_accent_color", 0)

        try {
            // Check if a resource id has been passed.
            return ContextCompat.getColor(Notificare.requireContext(), color)
        } catch (e: Resources.NotFoundException) {
            // Check if a valid color value has been passed.
            if (color != 0) return color
        }

        return null
    }

public val NotificareOptions.passNotificationOngoing: Boolean
    get() {
        return metadata.getBoolean(
            "re.notifica.loyalty.pass_notification_ongoing",
            false
        )
    }

public val NotificareOptions.passRelevanceText: String
    get() {
        val context = Notificare.requireContext()

        val str = metadata.getString("re.notifica.loyalty.pass_relevance_text", null)
        if (str != null) return str

        val id = metadata.getInt("re.notifica.loyalty.pass_relevance_text", 0)
        return if (id == 0) context.getString(R.string.notificare_passbook_location_relevance_text)
        else context.getString(id)
    }

public val NotificareOptions.passRelevanceHours: Int
    get() {
        return metadata.getInt("re.notifica.loyalty.pass_relevance_hours", 2)
    }

public val NotificareOptions.passRelevanceLargeRadius: Double
    get() {
        return metadata.getDouble("re.notifica.loyalty.pass_relevance_large_radius", 2000.0)
    }

public val NotificareOptions.passRelevanceSmallRadius: Double
    get() {
        return metadata.getDouble("re.notifica.loyalty.pass_relevance_small_radius", 500.0)
    }
