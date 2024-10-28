package re.notifica.sample.user.inbox.core

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.auth0.android.Auth0
import com.auth0.android.authentication.AuthenticationAPIClient
import com.auth0.android.authentication.storage.CredentialsManager
import com.auth0.android.authentication.storage.SharedPreferencesStorage
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import re.notifica.sample.user.inbox.ktx.Event

internal abstract class BaseViewModel : ViewModel() {
    internal companion object {
        internal lateinit var baseUrl: String
            private set

        internal lateinit var registerDeviceUrl: String
            private set

        internal lateinit var fetchInboxUrl: String
            private set
    }

    private val eventChannel = Channel<Event>(Channel.BUFFERED)
    internal val eventsFlow = eventChannel.receiveAsFlow()

    internal fun showSnackBar(message: String) {
        viewModelScope.launch {
            eventChannel.send(Event.ShowSnackBar(message))
        }
    }

    internal fun setUserInboxURLs(
        base: String,
        registerDevice: String,
        fetchInbox: String
    ) {
        baseUrl = base
        registerDeviceUrl = registerDevice
        fetchInboxUrl = fetchInbox
    }

    internal fun getCredentialsManager(context: Context, account: Auth0): CredentialsManager {
        val authentication = AuthenticationAPIClient(account)
        val storage = SharedPreferencesStorage(context)

        return CredentialsManager(authentication, storage)
    }
}
