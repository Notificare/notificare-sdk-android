package re.notifica.sample.user.inbox.core

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import re.notifica.sample.user.inbox.ktx.Event

internal abstract class BaseViewModel : ViewModel() {
    internal companion object {
        internal lateinit var registerDeviceUrl: String
            private set

        internal lateinit var fetchInboxUrl: String
            private set

        internal var accessToken: String? = null
    }

    private val eventChannel = Channel<Event>(Channel.BUFFERED)
    internal val eventsFlow = eventChannel.receiveAsFlow()

    internal fun showSnackBar(message: String) {
        viewModelScope.launch {
            eventChannel.send(Event.ShowSnackBar(message))
        }
    }

    internal fun setUserInboxURLs(registerDevice: String, fetchInbox: String) {
        registerDeviceUrl = registerDevice
        fetchInboxUrl = fetchInbox
    }
}
