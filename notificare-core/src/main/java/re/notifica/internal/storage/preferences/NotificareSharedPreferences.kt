package re.notifica.internal.storage.preferences

import android.annotation.SuppressLint
import android.content.Context
import re.notifica.Notificare
import re.notifica.NotificareDefinitions
import re.notifica.models.NotificareDevice
import re.notifica.models.NotificareEvent

internal class NotificareSharedPreferences(context: Context) {

    companion object {
        private const val PREFERENCE_DEVICE = "re.notifica.preferences.device"
        private const val PREFERENCE_PREFERRED_LANGUAGE = "re.notifica.preferences.preferred_language"
        private const val PREFERENCE_PREFERRED_REGION = "re.notifica.preferences.preferred_region"
        private const val PREFERENCE_CRASH_REPORT = "re.notifica.preferences.crash_report"
    }

    private val sharedPreferences = context.getSharedPreferences(
        NotificareDefinitions.SHARED_PREFERENCES_NAME,
        Context.MODE_PRIVATE
    )

    var device: NotificareDevice?
        get() {
            return sharedPreferences.getString(PREFERENCE_DEVICE, null)
                ?.let { Notificare.moshi.adapter(NotificareDevice::class.java).fromJson(it) }
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
                ?.let { Notificare.moshi.adapter(NotificareEvent::class.java).fromJson(it) }
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
