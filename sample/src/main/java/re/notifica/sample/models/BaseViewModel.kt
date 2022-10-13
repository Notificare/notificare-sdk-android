package re.notifica.sample.models

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import re.notifica.sample.ktx.Event

abstract class BaseViewModel : ViewModel() {
    private val eventChannel = Channel<Event>(Channel.BUFFERED)
    val eventsFlow = eventChannel.receiveAsFlow()

    fun showSnackBar(message: String) {
        viewModelScope.launch {
            eventChannel.send(Event.ShowSnackBar(message))
        }
    }
}
