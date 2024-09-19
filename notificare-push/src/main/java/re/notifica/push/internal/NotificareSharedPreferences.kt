package re.notifica.push.internal

import android.content.Context
import androidx.core.content.edit
import re.notifica.Notificare
import re.notifica.utilities.NotificareLogger
import re.notifica.internal.moshi
import re.notifica.push.models.NotificarePushSubscription
import re.notifica.push.models.NotificareTransport

internal class NotificareSharedPreferences(context: Context) {

    private val logger = NotificareLogger(
        Notificare.options?.debugLoggingEnabled ?: false,
        "NotificareSharedPreferences"
    )

    companion object {
        private const val PREFERENCES_FILE_NAME = "re.notifica.push.preferences"

        private const val PREFERENCE_REMOTE_NOTIFICATIONS_ENABLED =
            "re.notifica.push.preferences.remote_notifications_enabled"
        private const val PREFERENCE_TRANSPORT = "re.notifica.push.preferences.transport"
        private const val PREFERENCE_SUBSCRIPTION = "re.notifica.push.preferences.subscription"
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

    var transport: NotificareTransport?
        get() {
            return sharedPreferences.getString(PREFERENCE_TRANSPORT, null)?.let {
                try {
                    Notificare.moshi.adapter(NotificareTransport::class.java).fromJson(it)
                } catch (e: Exception) {
                    logger.warning("Failed to decode the stored transport.", e)

                    // Remove the corrupted value from local storage.
                    sharedPreferences.edit { remove(PREFERENCE_TRANSPORT) }

                    null
                }
            }
        }
        set(value) {
            sharedPreferences.edit {
                if (value == null) remove(PREFERENCE_TRANSPORT)
                else putString(
                    PREFERENCE_TRANSPORT,
                    Notificare.moshi.adapter(NotificareTransport::class.java).toJson(value)
                )
            }
        }

    var subscription: NotificarePushSubscription?
        get() {
            return sharedPreferences.getString(PREFERENCE_SUBSCRIPTION, null)?.let {
                try {
                    Notificare.moshi.adapter(NotificarePushSubscription::class.java).fromJson(it)
                } catch (e: Exception) {
                    logger.warning("Failed to decode the stored subscription.", e)

                    // Remove the corrupted value from local storage.
                    sharedPreferences.edit { remove(PREFERENCE_SUBSCRIPTION) }

                    null
                }
            }
        }
        set(value) {
            sharedPreferences.edit {
                if (value == null) remove(PREFERENCE_SUBSCRIPTION)
                else putString(
                    PREFERENCE_SUBSCRIPTION,
                    Notificare.moshi.adapter(NotificarePushSubscription::class.java).toJson(value)
                )
            }
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
