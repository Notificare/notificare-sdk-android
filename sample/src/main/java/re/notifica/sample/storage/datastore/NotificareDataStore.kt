package re.notifica.sample.storage.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.squareup.moshi.Moshi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import re.notifica.sample.live_activities.models.CoffeeBrewerContentState

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "re.notifica.sample.datastore")
private val KEY_COFFEE_BREWER_CONTENT_STATE = stringPreferencesKey("coffee_brewer_content_state")

class NotificareDataStore(context: Context) {
    private val moshi: Moshi = Moshi.Builder().build()
    private val dataStore = context.dataStore

    val coffeeBrewerContentStateStream: Flow<CoffeeBrewerContentState?> =
        dataStore.data.map { preferences ->
            val str = preferences[KEY_COFFEE_BREWER_CONTENT_STATE] ?: return@map null

            val adapter = moshi.adapter(CoffeeBrewerContentState::class.java)
            return@map adapter.fromJson(str)
        }

    suspend fun updateCoffeeBrewerContentState(
        contentState: CoffeeBrewerContentState?
    ): Unit = withContext(Dispatchers.IO) {
        dataStore.edit { preferences ->
            val str = contentState?.let {
                val adapter = moshi.adapter(CoffeeBrewerContentState::class.java)
                adapter.toJson(it)
            }

            if (str != null) {
                preferences[KEY_COFFEE_BREWER_CONTENT_STATE] = str
            } else {
                preferences.remove(KEY_COFFEE_BREWER_CONTENT_STATE)
            }
        }
    }
}
