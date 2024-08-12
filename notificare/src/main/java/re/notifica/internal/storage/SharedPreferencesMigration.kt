package re.notifica.internal.storage

import android.content.Context
import androidx.core.content.edit
import org.json.JSONObject
import re.notifica.internal.NotificareLogger
import re.notifica.internal.NotificareModule
import re.notifica.internal.NotificareUtils
import re.notifica.internal.storage.preferences.NotificareSharedPreferences
import re.notifica.internal.storage.preferences.entities.StoredDevice

internal class SharedPreferencesMigration(
    private val context: Context,
) {

    companion object {
        private const val V2_SAVED_STATE_FILENAME = "re.notifica.preferences.SavedState"
        private const val V2_SETTINGS_FILENAME = "re.notifica.preferences.Settings"
    }

    val hasLegacyData: Boolean
        get() {
            val savedState = context.getSharedPreferences(V2_SAVED_STATE_FILENAME, Context.MODE_PRIVATE)
                ?: return false

            return savedState.contains("registeredDevice")
        }

    fun migrate() {
        val preferences = NotificareSharedPreferences(context)

        val v2SavedState = context.getSharedPreferences(V2_SAVED_STATE_FILENAME, Context.MODE_PRIVATE)
        val v2Settings = context.getSharedPreferences(V2_SETTINGS_FILENAME, Context.MODE_PRIVATE)

        if (v2SavedState.contains("registeredDevice")) {
            NotificareLogger.debug("Found v2 device stored.")

            val jsonStr = v2SavedState.getString("registeredDevice", null)
            if (jsonStr != null) {
                try {
                    val json = JSONObject(jsonStr)

                    val device = StoredDevice(
                        id = json.getString("deviceID"),
                        userId = if (!json.isNull("userID")) json.getString("userID") else null,
                        userName = if (!json.isNull("userName")) json.getString("userName") else null,
                        timeZoneOffset = if (!json.isNull("timeZoneOffset")) {
                            json.getDouble("timeZoneOffset")
                        } else {
                            0.toDouble()
                        },
                        osVersion = json.getString("osVersion"),
                        sdkVersion = json.getString("sdkVersion"),
                        appVersion = json.getString("appVersion"),
                        deviceString = json.getString("deviceString"),
                        language = if (!json.isNull("language")) {
                            json.getString("language")
                        } else {
                            NotificareUtils.deviceLanguage
                        },
                        region = if (!json.isNull("region")) {
                            json.getString("region")
                        } else {
                            NotificareUtils.deviceRegion
                        },
                        dnd = null,
                        userData = mapOf(),
                        transport = json.optString("transport", "Notificare"),
                    )

                    preferences.device = device
                } catch (e: Exception) {
                    NotificareLogger.error("Failed to migrate v2 device.", e)
                }
            }
        }

        if (v2Settings.contains("overrideLanguage")) {
            NotificareLogger.debug("Found v2 language override stored.")

            val language = v2Settings.getString("overrideLanguage", null)
            preferences.preferredLanguage = language
        }

        if (v2Settings.contains("overrideRegion")) {
            NotificareLogger.debug("Found v2 region override stored.")

            val region = v2Settings.getString("overrideRegion", null)
            preferences.preferredRegion = region
        }

        // Signal each available module to migrate whatever data it needs.
        NotificareModule.Module.entries.forEach {
            it.instance?.migrate(
                savedState = v2SavedState,
                settings = v2Settings,
            )
        }

        // Remove all data from the legacy files
        v2SavedState.edit { clear() }
        v2Settings.edit { clear() }
    }
}
