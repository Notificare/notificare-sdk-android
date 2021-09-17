package re.notifica.geo.internal.storage

import android.content.Context
import androidx.core.content.edit
import com.squareup.moshi.Types
import re.notifica.Notificare
import re.notifica.geo.models.NotificareRegion
import re.notifica.internal.NotificareLogger
import re.notifica.internal.moshi

private const val PREFERENCES_FILE_NAME = "re.notifica.geo.preferences"
private const val PREFERENCE_MONITORED_REGIONS = "re.notifica.geo.preferences.monitored_regions"
private const val PREFERENCE_ENTERED_REGIONS = "re.notifica.geo.preferences.entered_regions"

internal class LocalStorage(context: Context) {
    private val sharedPreferences = context.getSharedPreferences(
        PREFERENCES_FILE_NAME,
        Context.MODE_PRIVATE
    )

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
}
