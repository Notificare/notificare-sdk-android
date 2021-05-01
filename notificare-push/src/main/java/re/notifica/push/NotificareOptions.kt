package re.notifica.push

import re.notifica.internal.NotificareOptions

val NotificareOptions.preferredMobileServices: String?
    get() {
        return info.metaData.getString(
            "re.notifica.push.preferred_mobile_services",
            null,
        )
    }

val NotificareOptions.defaultChannelId: String?
    get() {
        return info.metaData.getString(
            "re.notifica.push.default_channel_id",
            NotificarePush.DEFAULT_NOTIFICATION_CHANNEL_ID,
        )
    }

val NotificareOptions.automaticDefaultChannelEnabled: Boolean
    get() {
        return info.metaData.getBoolean(
            "re.notifica.push.automatic_default_channel_enabled",
            true,
        )
    }

val NotificareOptions.notificationAutoCancel: Boolean
    get() {
        return info.metaData.getBoolean(
            "re.notifica.push.notification_auto_cancel",
            true,
        )
    }

val NotificareOptions.notificationSmallIcon: Int?
    get() {
        val icon = info.metaData.getInt(
            "re.notifica.push.notification_small_icon",
            0,
        )

        return if (icon == 0) null else icon
    }

val NotificareOptions.notificationAccentColor: Int?
    get() {
        return if (info.metaData.containsKey("re.notifica.push.notification_accent_color"))
            info.metaData.getInt("re.notifica.push.notification_accent_color", 0)
        else
            null
    }

val NotificareOptions.notificationLightsColor: String?
    get() {
        return info.metaData.getString(
            "re.notifica.push.notification_lights_color",
            null,
        )
    }

val NotificareOptions.notificationLightsOn: Int
    get() {
        return info.metaData.getInt(
            "re.notifica.push.notification_lights_on",
            500,
        )
    }

val NotificareOptions.notificationLightsOff: Int
    get() {
        return info.metaData.getInt(
            "re.notifica.push.notification_lights_off",
            1500,
        )
    }
