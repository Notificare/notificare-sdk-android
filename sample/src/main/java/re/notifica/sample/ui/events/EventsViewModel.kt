package re.notifica.sample.ui.events

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import re.notifica.Notificare
import re.notifica.ktx.events
import re.notifica.sample.models.BaseViewModel
import timber.log.Timber

class EventsViewModel : BaseViewModel() {
    var userFieldsCount = 0

    fun registerEvent(event: String, data: Map<String, String>?) {
        viewModelScope.launch {
            try {
                Notificare.events().logCustom(event, data)
            } catch (e: Exception) {
                Timber.e(e, "Failed to log custom event.")
                showSnackBar("Failed to log custom event: ${e.message}")

                return@launch
            }

            Timber.i("Logged custom event successfully.")
            showSnackBar("Logged custom event successfully.")
        }
    }
}
