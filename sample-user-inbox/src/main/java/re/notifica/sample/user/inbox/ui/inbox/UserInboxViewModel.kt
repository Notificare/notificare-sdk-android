package re.notifica.sample.user.inbox.ui.inbox

import android.app.Activity
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.json.JSONObject
import re.notifica.Notificare
import re.notifica.inbox.user.ktx.userInbox
import re.notifica.inbox.user.models.NotificareUserInboxItem
import re.notifica.push.ui.ktx.pushUI
import re.notifica.sample.user.inbox.core.BaseViewModel
import re.notifica.sample.user.inbox.core.NotificationEvent
import re.notifica.sample.user.inbox.network.SampleRequest
import timber.log.Timber

internal class UserInboxViewModel : BaseViewModel() {
    private val _items = MutableLiveData<List<NotificareUserInboxItem>>()
    val items: LiveData<List<NotificareUserInboxItem>> = _items

    init {
        viewModelScope.launch {
            NotificationEvent.inboxShouldUpdateFlow.collect {
                refresh()
            }
        }
    }

    internal fun open(activity: Activity, item: NotificareUserInboxItem) {
        viewModelScope.launch {
            try {
                val notification = Notificare.userInbox().open(item)
                Notificare.pushUI().presentNotification(activity, notification)
                NotificationEvent.triggerInboxShouldUpdateEvent()

                Timber.i("Opened inbox item successfully.")
                showSnackBar("Opened inbox item successfully.")
            } catch (e: Exception) {
                Timber.e(e, "Failed to open inbox item.")
                showSnackBar("Failed to open inbox item.")
            }
        }
    }

    internal fun markAsRead(item: NotificareUserInboxItem) {
        viewModelScope.launch {
            try {
                Notificare.userInbox().markAsRead(item)
                NotificationEvent.triggerInboxShouldUpdateEvent()

                Timber.i("Mark inbox item as read successfully.")
                showSnackBar("Mark inbox item as read successfully.")
            } catch (e: Exception) {
                Timber.e(e, "Failed to mark inbox item as read.")
                showSnackBar("Failed to mark inbox item as read.")
            }
        }
    }

    internal fun remove(item: NotificareUserInboxItem) {
        viewModelScope.launch {
            try {
                Notificare.userInbox().remove(item)
                NotificationEvent.triggerInboxShouldUpdateEvent()

                Timber.i("Removed inbox item successfully.")
                showSnackBar("Removed inbox item successfully.")
            } catch (e: Exception) {
                Timber.e(e, "Failed to remove inbox item.")
                showSnackBar("Failed to remove inbox item.")
            }
        }
    }

    internal fun refresh() {
        if (accessToken == null) {
            Timber.e("Failed refreshing inbox, access token is missing.")
            showSnackBar("Failed refreshing inbox, access token is missing.")

            return
        }

        viewModelScope.launch {
            try {
                val responseStr = SampleRequest.Builder()
                    .authentication("Bearer $accessToken")
                    .get(fetchInboxUrl)
                    .responseString()

                val json = JSONObject(responseStr)
                val userInboxResponse = Notificare.userInbox().parseResponse(json)

                _items.postValue(userInboxResponse.items)

                Timber.i("Refresh inbox success.")
                showSnackBar("Refresh inbox success.")
            } catch (e: Exception) {
                Timber.e(e, "Failed to refresh inbox.")
                showSnackBar("Failed to refresh inbox.")
            }
        }
    }
}
