package re.notifica.sample.ui.inbox

import android.app.Activity
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import re.notifica.Notificare
import re.notifica.inbox.ktx.inbox
import re.notifica.inbox.models.NotificareInboxItem
import re.notifica.push.ui.ktx.pushUI
import re.notifica.sample.models.BaseViewModel
import timber.log.Timber

class InboxViewModel : BaseViewModel() {
    private val _items = MutableLiveData<List<NotificareInboxItem>>()
    val items: LiveData<List<NotificareInboxItem>> = _items

    init {
        viewModelScope.launch {
            Notificare.inbox().observableItems
                .asFlow()
                .collect { result ->
                    _items.postValue(result.toList())
                }
        }
    }

    fun open(activity: Activity, item: NotificareInboxItem) {
        viewModelScope.launch {
            try {
                val notification = Notificare.inbox().open(item)
                Notificare.pushUI().presentNotification(activity, notification)

                Timber.i("Opened inbox item successfully.")
                showSnackBar("Opened inbox item successfully.")
            } catch (e: Exception) {
                Timber.e(e, "Failed to open inbox item.")
                showSnackBar("Failed to open inbox item.")
            }
        }
    }

    fun markAsRead(item: NotificareInboxItem) {
        viewModelScope.launch {
            try {
                Notificare.inbox().markAsRead(item)

                Timber.i("Mark inbox item as read successfully.")
                showSnackBar("Mark inbox item as read successfully.")
            } catch (e: Exception) {
                Timber.e(e, "Failed to mark inbox item as read.")
                showSnackBar("Failed to mark inbox item as read.")
            }
        }
    }

    fun remove(item: NotificareInboxItem) {
        viewModelScope.launch {
            try {
                Notificare.inbox().remove(item)

                Timber.i("Removed inbox item successfully.")
                showSnackBar("Removed inbox item successfully.")
            } catch (e: Exception) {
                Timber.e(e, "Failed to remove inbox item.")
                showSnackBar("Failed to remove inbox item.")
            }
        }
    }

    fun markAllAsRead() {
        viewModelScope.launch {
            try {
                Notificare.inbox().markAllAsRead()

                Timber.i("Marked all items as read successfully.")
                showSnackBar("Marked all items as read successfully.")
            } catch (e: Exception) {
                Timber.e(e, "Failed to mark all items as read.")
                showSnackBar("Failed to mark all items as read.")
            }
        }
    }

    fun clear() {
        viewModelScope.launch {
            try {
                Notificare.inbox().clear()

                Timber.i("Inbox cleared successfully.")
                showSnackBar("Inbox cleared successfully.")
            } catch (e: Exception) {
                Timber.e(e, "Failed to clear inbox.")
                showSnackBar("Failed to remove clear inbox.")
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            try {
                Notificare.inbox().refresh()

                Timber.i("Refreshed inbox successfully.")
                showSnackBar("Refreshed inbox successfully.")
            } catch (e: Exception) {
                Timber.e(e, "Failed to refresh inbox.")
                showSnackBar("Failed to refresh inbox.")
            }
        }
    }
}
