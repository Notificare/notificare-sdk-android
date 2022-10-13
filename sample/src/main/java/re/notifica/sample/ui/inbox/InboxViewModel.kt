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

    fun onInboxItemClicked(activity: Activity, item: NotificareInboxItem) {
        viewModelScope.launch {
            try {
                val notification = Notificare.inbox().open(item)
                Notificare.pushUI().presentNotification(activity, notification)
            } catch (e: Exception) {
                Timber.e(e, "Failed to open an inbox item.")
                showSnackBar("Failed to open an inbox item: ${e.message}")

                return@launch
            }
        }
    }

    fun onMarkItemAsReadClicked(item: NotificareInboxItem) {
        viewModelScope.launch {
            try {
                Notificare.inbox().markAsRead(item)
            } catch (e: Exception) {
                Timber.e(e, "Failed to mark an item as read.")
                showSnackBar("Failed to mark an item as read: ${e.message}")

                return@launch
            }
        }
    }

    fun onRemoveItemClicked(item: NotificareInboxItem) {
        viewModelScope.launch {
            try {
                Notificare.inbox().remove(item)
            } catch (e: Exception) {
                Timber.e(e, "Failed to remove an item.")
                showSnackBar("Failed to remove an item: ${e.message}")

                return@launch
            }
        }
    }

    fun onReadAllClicked() {
        viewModelScope.launch {
            try {
                Notificare.inbox().markAllAsRead()
            } catch (e: Exception) {
                Timber.e(e, "Failed to mark all items as read.")
                showSnackBar("Failed to mark all items as read: ${e.message}")

                return@launch
            }
        }
    }

    fun onRemoveAllClicked() {
        viewModelScope.launch {
            try {
                Notificare.inbox().clear()
            } catch (e: Exception) {
                Timber.e(e, "Failed to remove all items.")
                showSnackBar("Failed to remove all items: ${e.message}")

                return@launch
            }
        }
    }

    fun onRefreshClicked() {
        viewModelScope.launch {
            try {
                Notificare.inbox().refresh()
            } catch (e: Exception) {
                Timber.e(e, "Failed to refresh inbox.")
                showSnackBar("Failed to refresh inbox: ${e.message}")

                return@launch
            }

            Timber.i("Refreshed inbox successfully.")
            showSnackBar("Refreshed inbox successfully.")
        }
    }
}
