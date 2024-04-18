package re.notifica.internal.storage.preferences

import android.annotation.SuppressLint
import android.content.Context
import androidx.core.content.edit
import re.notifica.Notificare
import re.notifica.internal.NotificareLogger
import re.notifica.internal.moshi
import re.notifica.models.NotificareApplication
import re.notifica.models.NotificareDevice
import re.notifica.models.NotificareEvent

internal class NotificareSharedPreferences(context: Context) {

    companion object {
        private const val PREFERENCES_FILE_NAME = "re.notifica.preferences"

        private const val PREFERENCE_MIGRATED = "re.notifica.preferences.migrated"
        private const val PREFERENCE_APPLICATION = "re.notifica.preferences.application"
        private const val PREFERENCE_DEVICE = "re.notifica.preferences.device"
        private const val PREFERENCE_PREFERRED_LANGUAGE = "re.notifica.preferences.preferred_language"
        private const val PREFERENCE_PREFERRED_REGION = "re.notifica.preferences.preferred_region"
        private const val PREFERENCE_CRASH_REPORT = "re.notifica.preferences.crash_report"
    }

    private val sharedPreferences = context.getSharedPreferences(
        PREFERENCES_FILE_NAME,
        Context.MODE_PRIVATE
    )

    var migrated: Boolean
        get() {
            return sharedPreferences.getBoolean(PREFERENCE_MIGRATED, false)
        }
        set(value) {
            sharedPreferences.edit {
                putBoolean(PREFERENCE_MIGRATED, value)
            }
        }

    var application: NotificareApplication?
        get() {
            return sharedPreferences.getString(PREFERENCE_APPLICATION, null)
                ?.let {
                    try {
                        Notificare.moshi.adapter(NotificareApplication::class.java).fromJson(it)
                    } catch (e: Exception) {
                        NotificareLogger.warning("Failed to decode the stored application.", e)

                        // Remove the corrupted device from local storage.
                        application = null

                        null
                    }
                }
        }
        set(value) {
            sharedPreferences.edit().also {
                if (value == null) it.remove(PREFERENCE_APPLICATION)
                else it.putString(
                    PREFERENCE_APPLICATION,
                    Notificare.moshi.adapter(NotificareApplication::class.java).toJson(value)
                )
            }.apply()
        }

    var device: NotificareDevice?
        get() {
            return sharedPreferences.getString(PREFERENCE_DEVICE, null)
                ?.let {
                    try {
                        Notificare.moshi.adapter(NotificareDevice::class.java).fromJson(it)
                    } catch (e: Exception) {
                        NotificareLogger.warning("Failed to decode the stored device.", e)

                        // Remove the corrupted device from local storage.
                        device = null

                        null
                    }
                }
        }
        set(value) {
            sharedPreferences.edit().also {
                if (value == null) it.remove(PREFERENCE_DEVICE)
                else it.putString(
                    PREFERENCE_DEVICE,
                    Notificare.moshi.adapter(NotificareDevice::class.java).toJson(value)
                )
            }.apply()
        }

    var preferredLanguage: String?
        get() {
            return sharedPreferences.getString(
                PREFERENCE_PREFERRED_LANGUAGE,
                null
            )
        }
        set(value) {
            sharedPreferences.edit()
                .apply {
                    if (value == null) {
                        remove(PREFERENCE_PREFERRED_LANGUAGE)
                    } else {
                        putString(PREFERENCE_PREFERRED_LANGUAGE, value)
                    }
                }
                .apply()
        }

    var preferredRegion: String?
        get() {
            return sharedPreferences.getString(
                PREFERENCE_PREFERRED_REGION,
                null
            )
        }
        set(value) {
            sharedPreferences.edit()
                .apply {
                    if (value == null) {
                        remove(PREFERENCE_PREFERRED_REGION)
                    } else {
                        putString(PREFERENCE_PREFERRED_REGION, value)
                    }
                }
                .apply()
        }

    var crashReport: NotificareEvent?
        get() {
            return sharedPreferences.getString(PREFERENCE_CRASH_REPORT, null)
                ?.let {
                    try {
                        Notificare.moshi.adapter(NotificareEvent::class.java).fromJson(it)
                    } catch (e: Exception) {
                        NotificareLogger.warning("Failed to decode the stored crash report.", e)

                        // Remove the corrupted crash report from local storage.
                        crashReport = null

                        null
                    }
                }
        }

        @SuppressLint("ApplySharedPref")
        set(value) {
            sharedPreferences.edit().also {
                if (value == null) it.remove(PREFERENCE_CRASH_REPORT)
                else it.putString(
                    PREFERENCE_CRASH_REPORT,
                    Notificare.moshi.adapter(NotificareEvent::class.java).toJson(value)
                )
            }.commit()
        }
}
