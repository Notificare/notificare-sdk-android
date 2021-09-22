package re.notifica.geo.internal.storage

import android.content.Context
import androidx.core.content.edit
import com.squareup.moshi.Types
import re.notifica.Notificare
import re.notifica.geo.models.NotificareLocation
import re.notifica.geo.models.NotificareRegion
import re.notifica.geo.models.NotificareRegionSession
import re.notifica.internal.NotificareLogger
import re.notifica.internal.moshi

private const val PREFERENCES_FILE_NAME = "re.notifica.geo.preferences"
private const val PREFERENCE_LOCATION_SERVICES_ENABLED = "re.notifica.geo.preferences.location_services_enabled"
private const val PREFERENCE_MONITORED_REGIONS = "re.notifica.geo.preferences.monitored_regions"
private const val PREFERENCE_ENTERED_REGIONS = "re.notifica.geo.preferences.entered_regions"
private const val PREFERENCE_REGION_SESSIONS = "re.notifica.geo.preferences.region_sessions"

internal class LocalStorage(context: Context) {
    private val sharedPreferences = context.getSharedPreferences(
        PREFERENCES_FILE_NAME,
        Context.MODE_PRIVATE
    )

    var locationServicesEnabled: Boolean
        get() = sharedPreferences.getBoolean(PREFERENCE_LOCATION_SERVICES_ENABLED, false)
        set(value) = sharedPreferences.edit { putBoolean(PREFERENCE_LOCATION_SERVICES_ENABLED, value) }

    var monitoredRegions: List<NotificareRegion>
        get() {
            val jsonStr = sharedPreferences.getString(PREFERENCE_MONITORED_REGIONS, null)
                ?: return emptyList()

            try {
                val type = Types.newParameterizedType(List::class.java, NotificareRegion::class.java)
                val adapter = Notificare.moshi.adapter<List<NotificareRegion>>(type)

                return adapter.fromJson(jsonStr) ?: emptyList()
            } catch (e: Exception) {
                NotificareLogger.warning("Failed to decode the monitored regions.", e)

                // remove the corrupted data.
                monitoredRegions = emptyList()
            }

            return emptyList()
        }
        set(value) {
            try {
                val type = Types.newParameterizedType(List::class.java, NotificareRegion::class.java)
                val adapter = Notificare.moshi.adapter<List<NotificareRegion>>(type)

                sharedPreferences.edit {
                    putString(PREFERENCE_MONITORED_REGIONS, adapter.toJson(value))
                }
            } catch (e: Exception) {
                NotificareLogger.warning("Failed to encode the monitored regions.", e)
            }
        }

    var enteredRegions: Set<String>
        get() {
            return sharedPreferences.getStringSet(PREFERENCE_ENTERED_REGIONS, null)
                ?: emptySet()
        }
        set(value) {
            sharedPreferences.edit {
                putStringSet(PREFERENCE_ENTERED_REGIONS, value)
            }
        }

    var regionSessions: Map<String, NotificareRegionSession>
        get() {
            val jsonStr = sharedPreferences.getString(PREFERENCE_REGION_SESSIONS, null)
                ?: return emptyMap()

            try {
                val type = Types.newParameterizedType(List::class.java, NotificareRegionSession::class.java)
                val adapter = Notificare.moshi.adapter<List<NotificareRegionSession>>(type)

                val sessions = adapter.fromJson(jsonStr) ?: emptyList()

                return sessions.map { it.regionId to it }.toMap()
            } catch (e: Exception) {
                NotificareLogger.warning("Failed to decode the region sessions.", e)

                // remove the corrupted data.
                regionSessions = emptyMap()
            }

            return emptyMap()
        }
        private set(value) {
            try {
                val sessions = value.values.toList()

                val type = Types.newParameterizedType(List::class.java, NotificareRegionSession::class.java)
                val adapter = Notificare.moshi.adapter<List<NotificareRegionSession>>(type)

                sharedPreferences.edit {
                    putString(PREFERENCE_REGION_SESSIONS, adapter.toJson(sessions))
                }
            } catch (e: Exception) {
                NotificareLogger.warning("Failed to encode the region sessions.", e)
            }
        }


    fun addRegionSession(session: NotificareRegionSession) {
        regionSessions = regionSessions.toMutableMap().apply {
            put(session.regionId, session)
        }
    }

    fun updateRegionSessions(location: NotificareLocation) {
        regionSessions = regionSessions.toMutableMap().onEach { entry ->
            entry.value.locations.add(location)
        }
    }

    fun removeRegionSession(session: NotificareRegionSession) {
        regionSessions = regionSessions.toMutableMap().apply {
            remove(session.regionId)
        }
    }
}
