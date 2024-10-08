package re.notifica.internal.storage.preferences

import android.content.Context
import androidx.core.content.edit
import re.notifica.Notificare
import re.notifica.internal.logger
import re.notifica.internal.moshi
import re.notifica.internal.storage.preferences.entities.StoredDevice
import re.notifica.models.NotificareApplication
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
        private const val PREFERENCE_DEFERRED_LINK_CHECKED = "re.notifica.preferences.deferred_link_checked"
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
                        logger.warning("Failed to decode the stored application.", e)

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

    var device: StoredDevice?
        get() {
            return sharedPreferences.getString(PREFERENCE_DEVICE, null)
                ?.let {
                    try {
                        Notificare.moshi.adapter(StoredDevice::class.java).fromJson(it)
                    } catch (e: Exception) {
                        logger.warning("Failed to decode the stored device.", e)

                        // Remove the corrupted device from local storage.
                        device = null

                        null
                    }
                }
        }
        set(value) {
            sharedPreferences.edit {
                if (value == null) remove(PREFERENCE_DEVICE)
                else putString(
                    PREFERENCE_DEVICE,
                    Notificare.moshi.adapter(StoredDevice::class.java).toJson(value)
                )
            }
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
                        logger.warning("Failed to decode the stored crash report.", e)

                        // Remove the corrupted crash report from local storage.
                        crashReport = null

                        null
                    }
                }
        }
        set(value) {
            sharedPreferences.edit(commit = true) {
                if (value == null) remove(PREFERENCE_CRASH_REPORT)
                else putString(
                    PREFERENCE_CRASH_REPORT,
                    Notificare.moshi.adapter(NotificareEvent::class.java).toJson(value)
                )
            }
        }

    var deferredLinkChecked: Boolean?
        get() {
            if (!sharedPreferences.contains(PREFERENCE_DEFERRED_LINK_CHECKED)) {
                return null
            }

            return sharedPreferences.getBoolean(PREFERENCE_DEFERRED_LINK_CHECKED, false)
        }
        set(value) = sharedPreferences.edit {
            if (value == null) {
                remove(PREFERENCE_DEFERRED_LINK_CHECKED)
            } else {
                putBoolean(PREFERENCE_DEFERRED_LINK_CHECKED, value)
            }
        }

    fun clear() {
        sharedPreferences.edit {
            clear()
        }
    }
}
