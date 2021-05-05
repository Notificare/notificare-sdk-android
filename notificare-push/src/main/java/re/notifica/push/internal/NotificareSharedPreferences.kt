package re.notifica.push.internal

import android.content.Context

internal class NotificareSharedPreferences(context: Context) {

    companion object {
        private const val PREFERENCES_FILE_NAME = "re.notifica.push.preferences"

        private const val PREFERENCE_REMOTE_NOTIFICATIONS_ENABLED =
            "re.notifica.push.preferences.remote_notifications_enabled"
    }

    private val sharedPreferences = context.getSharedPreferences(
        PREFERENCES_FILE_NAME,
        Context.MODE_PRIVATE
    )

    var remoteNotificationsEnabled: Boolean
        get() {
            return sharedPreferences.getBoolean(
                PREFERENCE_REMOTE_NOTIFICATIONS_ENABLED,
                false
            )
        }
        set(value) {
            sharedPreferences.edit()
                .putBoolean(PREFERENCE_REMOTE_NOTIFICATIONS_ENABLED, value)
                .apply()
        }
}
