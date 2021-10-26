package re.notifica.push

import re.notifica.internal.NotificareOptions
import re.notifica.push.internal.NotificarePushImpl

public val NotificareOptions.defaultChannelId: String
    get() {
        return info.metaData?.getString(
            "re.notifica.push.default_channel_id",
            NotificarePushImpl.DEFAULT_NOTIFICATION_CHANNEL_ID,
        ) ?: NotificarePushImpl.DEFAULT_NOTIFICATION_CHANNEL_ID
    }

public val NotificareOptions.automaticDefaultChannelEnabled: Boolean
    get() {
        return info.metaData?.getBoolean(
            "re.notifica.push.automatic_default_channel_enabled",
            true,
        ) ?: true
    }

public val NotificareOptions.notificationAutoCancel: Boolean
    get() {
        return info.metaData?.getBoolean(
            "re.notifica.push.notification_auto_cancel",
            true,
        ) ?: true
    }

public val NotificareOptions.notificationSmallIcon: Int?
    get() {
        val icon = info.metaData?.getInt(
            "re.notifica.push.notification_small_icon",
            0,
        )

        return if (icon == 0) null else icon
    }

public val NotificareOptions.notificationAccentColor: Int?
    get() {
        return if (info.metaData != null && info.metaData.containsKey("re.notifica.push.notification_accent_color"))
            info.metaData.getInt("re.notifica.push.notification_accent_color", 0)
        else
            null
    }

public val NotificareOptions.notificationLightsColor: String?
    get() {
        return info.metaData?.getString(
            "re.notifica.push.notification_lights_color",
            null,
        )
    }

public val NotificareOptions.notificationLightsOn: Int
    get() {
        return info.metaData?.getInt(
            "re.notifica.push.notification_lights_on",
            500,
        ) ?: 500
    }

public val NotificareOptions.notificationLightsOff: Int
    get() {
        return info.metaData?.getInt(
            "re.notifica.push.notification_lights_off",
            1500,
        ) ?: 1500
    }
