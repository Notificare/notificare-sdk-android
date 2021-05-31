package re.notifica.internal

import android.content.Context
import androidx.core.content.edit
import org.json.JSONObject
import re.notifica.Notificare
import re.notifica.NotificareLogger
import re.notifica.internal.storage.preferences.NotificareSharedPreferences
import re.notifica.models.NotificareDevice
import re.notifica.models.NotificareTransport
import java.util.*

internal object MigrationUtils {

    private const val V2_SAVED_STATE_FILENAME = "re.notifica.preferences.SavedState"
    private const val V2_DEVICE = "registeredDevice"
    private const val V2_LANGUAGE = "overrideLanguage"
    private const val V2_REGION = "overrideRegion"

    fun migrate(context: Context) {
        val preferences = NotificareSharedPreferences(context)

        val v2Preferences = context.getSharedPreferences(V2_SAVED_STATE_FILENAME, Context.MODE_PRIVATE)

        if (v2Preferences.contains(V2_DEVICE)) {
            NotificareLogger.debug("Found v2 device stored.")

            val jsonStr = v2Preferences.getString(V2_DEVICE, null)
            if (jsonStr != null) {
                try {
                    val json = JSONObject(jsonStr)

                    val lastRegistered: Date = try {
                        val date = if (!json.isNull("lastActive")) {
                            val adapter = Notificare.moshi.adapter(Date::class.java)
                            adapter.fromJsonValue(json.getString("lastActive"))
                        } else {
                            null
                        }

                        date ?: Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -7) }.time
                    } catch (e: Exception) {
                        NotificareLogger.warning("Failed to parse legacy last registered date.")
                        Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -7) }.time
                    }

                    val device = NotificareDevice(
                        id = json.getString("deviceID"),
                        userId = if (!json.isNull("userID")) json.getString("userID") else null,
                        userName = if (!json.isNull("userName")) json.getString("userName") else null,
                        timeZoneOffset = if (!json.isNull("timeZoneOffset")) json.getDouble("timeZoneOffset") else 0.toDouble(),
                        osVersion = json.getString("osVersion"),
                        sdkVersion = json.getString("sdkVersion"),
                        appVersion = json.getString("appVersion"),
                        deviceString = json.getString("deviceString"),
                        language = if (!json.isNull("language")) json.getString("language") else NotificareUtils.deviceLanguage,
                        region = if (!json.isNull("region")) json.getString("region") else NotificareUtils.deviceRegion,
                        transport = when (json.optString("transport", "Notificare")) {
                            "Notificare" -> NotificareTransport.NOTIFICARE
                            "GCM" -> NotificareTransport.GCM
                            "HMS" -> NotificareTransport.HMS
                            else -> NotificareTransport.NOTIFICARE
                        },
                        dnd = null,
                        userData = mapOf(),
                        lastRegistered = lastRegistered,
//                        allowedUI = if (!json.isNull("allowedUI")) json.getBoolean("allowedUI") else false,
//                        bluetoothEnabled = if (!json.isNull("bluetoothEnabled")) json.getBoolean("bluetoothEnabled") else false,
                    )

                    preferences.device = device
                } catch (e: Exception) {
                    NotificareLogger.error("Failed to migrate v2 device.", e)
                }
            }
        }

        if (v2Preferences.contains(V2_LANGUAGE)) {
            NotificareLogger.debug("Found v2 language override stored.")

            val language = v2Preferences.getString(V2_LANGUAGE, null)
            preferences.preferredLanguage = language
        }

        if (v2Preferences.contains(V2_REGION)) {
            NotificareLogger.debug("Found v2 region override stored.")

            val region = v2Preferences.getString(V2_REGION, null)
            preferences.preferredRegion = region
        }
    }

    fun cleanUp(context: Context) {
        val v2Preferences = context.getSharedPreferences(V2_SAVED_STATE_FILENAME, Context.MODE_PRIVATE)

        v2Preferences.edit {
            remove(V2_DEVICE)
            remove(V2_LANGUAGE)
            remove(V2_REGION)
        }
    }
}
