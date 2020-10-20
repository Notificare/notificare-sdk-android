package re.notifica.internal.storage.preferences

import android.content.Context
import re.notifica.Notificare
import re.notifica.NotificareDefinitions
import re.notifica.internal.NotificareUtils
import re.notifica.models.NotificareDevice

internal class NotificareSharedPreferences(context: Context) {

    private val moshi = NotificareUtils.createMoshi()
    private val sharedPreferences = context.getSharedPreferences(
        NotificareDefinitions.SHARED_PREFERENCES_NAME,
        Context.MODE_PRIVATE
    )

    var device: NotificareDevice?
        get() {
            return sharedPreferences.getString(NotificareDefinitions.Preferences.DEVICE, null)
                ?.let { moshi.adapter(NotificareDevice::class.java).fromJson(it) }
        }
        set(value) {
            sharedPreferences.edit().also {
                if (value == null) it.remove(NotificareDefinitions.Preferences.DEVICE)
                else it.putString(
                    NotificareDefinitions.Preferences.DEVICE,
                    moshi.adapter(NotificareDevice::class.java).toJson(value)
                )
            }.apply()
        }

    var preferredLanguage: String?
        get() {
            return sharedPreferences.getString(
                NotificareDefinitions.Preferences.PREFERRED_LANGUAGE,
                null
            )
        }
        set(value) {
            sharedPreferences.edit()
                .apply {
                    if (value == null) {
                        remove(NotificareDefinitions.Preferences.PREFERRED_LANGUAGE)
                    } else {
                        putString(NotificareDefinitions.Preferences.PREFERRED_LANGUAGE, value)
                    }
                }
                .apply()
        }

    var preferredRegion: String?
        get() {
            return sharedPreferences.getString(
                NotificareDefinitions.Preferences.PREFERRED_REGION,
                null
            )
        }
        set(value) {
            sharedPreferences.edit()
                .apply {
                    if (value == null) {
                        remove(NotificareDefinitions.Preferences.PREFERRED_REGION)
                    } else {
                        putString(NotificareDefinitions.Preferences.PREFERRED_REGION, value)
                    }
                }
                .apply()
        }
}
