package re.notifica.loyalty.internal.storage

import android.content.Context
import androidx.core.content.edit

private const val PREFERENCES_FILE_NAME = "re.notifica.loyalty.preferences"
private const val PREFERENCE_PASSES_LAST_UPDATE = "re.notifica.loyalty.preferences.passes_last_update"

internal class LocalStorage(context: Context) {

    private val sharedPreferences = context.getSharedPreferences(
        PREFERENCES_FILE_NAME,
        Context.MODE_PRIVATE
    )

    internal var passesLastUpdate: String?
        get() = sharedPreferences.getString(PREFERENCE_PASSES_LAST_UPDATE, null)
        set(value) = sharedPreferences.edit { putString(PREFERENCE_PASSES_LAST_UPDATE, value) }
}
