package re.notifica.push.internal

import android.content.Context
import androidx.core.content.edit

internal class NotificareSharedPreferences(context: Context) {

    companion object {
        private const val PREFERENCES_FILE_NAME = "re.notifica.push.preferences"

        private const val PREFERENCE_REMOTE_NOTIFICATIONS_ENABLED =
            "re.notifica.push.preferences.remote_notifications_enabled"
        private const val PREFERENCE_ALLOWED_UI = "re.notifica.push.preferences.allowed_ui"
        private const val PREFERENCE_FIRST_REGISTRATION = "re.notifica.push.preferences.first_registration"
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

    var allowedUI: Boolean
        get() {
            return sharedPreferences.getBoolean(
                PREFERENCE_ALLOWED_UI,
                false
            )
        }
        set(value) {
            sharedPreferences.edit()
                .putBoolean(PREFERENCE_ALLOWED_UI, value)
                .apply()
        }

    var firstRegistration: Boolean
        get() = sharedPreferences.getBoolean(PREFERENCE_FIRST_REGISTRATION, true)
        set(value) = sharedPreferences.edit { putBoolean(PREFERENCE_FIRST_REGISTRATION, value) }
}
