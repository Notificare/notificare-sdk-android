package re.notifica.sample.ui.events

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import re.notifica.Notificare
import re.notifica.ktx.events
import re.notifica.sample.models.BaseViewModel
import timber.log.Timber

class EventsViewModel : BaseViewModel() {
    val eventDataFields = mutableListOf<EventField>()

    fun registerEvent(event: String) {
        val data = eventDataFields
            .filter { it.key.isNotBlank() && it.value.isNotBlank() }
            .associate { it.key to it.value }

        eventDataFields.clear()

        viewModelScope.launch {
            try {
                Notificare.events().logCustom(event, data.ifEmpty { null })

                Timber.i("Logged custom event successfully.")
                showSnackBar("Logged custom event successfully.")
            } catch (e: Exception) {
                Timber.e(e, "Failed to log custom event.")
                showSnackBar("Failed to log custom event: ${e.message}")
            }
        }
    }
}

data class EventField(
    var key: String = "",
    var value: String = ""
)
