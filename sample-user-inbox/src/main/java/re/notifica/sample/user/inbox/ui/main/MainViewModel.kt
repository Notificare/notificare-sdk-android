package re.notifica.sample.user.inbox.ui.main

import android.content.Context
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import com.auth0.android.Auth0
import com.auth0.android.provider.WebAuthProvider
import kotlinx.coroutines.launch
import org.json.JSONObject
import re.notifica.Notificare
import re.notifica.inbox.user.ktx.userInbox
import re.notifica.ktx.device
import re.notifica.models.NotificareApplication
import re.notifica.models.NotificareDoNotDisturb
import re.notifica.models.NotificareTime
import re.notifica.push.ktx.push
import re.notifica.sample.user.inbox.core.BaseViewModel
import re.notifica.sample.user.inbox.core.NotificationEvent
import re.notifica.sample.user.inbox.ktx.hasNotificationsPermission
import re.notifica.sample.user.inbox.models.ApplicationInfo
import re.notifica.sample.user.inbox.network.SampleRequest
import timber.log.Timber

internal class MainViewModel :
    BaseViewModel(),
    DefaultLifecycleObserver,
    Notificare.Listener {
    private val _isLoggedIn: MutableLiveData<Boolean> = MutableLiveData(false)
    val isLoggedIn: LiveData<Boolean> = _isLoggedIn

    private val _deviceRegistered = MutableLiveData(false)
    val deviceRegistered: LiveData<Boolean> = _deviceRegistered

    private val _notificareConfigured = MutableLiveData(isNotificareConfigured)
    val notificareConfigured: LiveData<Boolean> = _notificareConfigured

    private val _notificareReady = MutableLiveData(isNotificareReady)
    val notificareReady: LiveData<Boolean> = _notificareReady

    private val _notificationsEnabled = MutableLiveData(hasNotificationsEnabled)
    val notificationsEnabled: LiveData<Boolean> = _notificationsEnabled

    private val _remoteNotificationsEnabled = MutableLiveData(hasRemoteNotificationsEnabled)
    val remoteNotificationsEnabled: LiveData<Boolean> = _remoteNotificationsEnabled

    private val _notificationsAllowedUI = MutableLiveData(hasNotificationsAllowedUI)
    val notificationsAllowedUI: LiveData<Boolean> = _notificationsAllowedUI

    private val _hasNotificationsPermissions = MutableLiveData(hasNotificationsPermission)
    val hasNotificationsPermissions: LiveData<Boolean> = _hasNotificationsPermissions

    private val _badge: MutableLiveData<Int> = MutableLiveData(0)
    val badge: LiveData<Int> = _badge

    private val _dndEnabled = MutableLiveData(hasDndEnabled)
    val dndEnabled: LiveData<Boolean> = _dndEnabled

    private val _dnd = MutableLiveData(Notificare.device().currentDevice?.dnd ?: NotificareDoNotDisturb.default)
    val dnd: LiveData<NotificareDoNotDisturb> = _dnd

    private val _applicationInfo = MutableLiveData(appInfo)
    val applicationInfo: MutableLiveData<ApplicationInfo?> = _applicationInfo

    private val isNotificareConfigured: Boolean
        get() = Notificare.isConfigured

    private val isNotificareReady: Boolean
        get() = Notificare.isReady

    private val hasRemoteNotificationsEnabled: Boolean
        get() = Notificare.push().hasRemoteNotificationsEnabled

    private val hasNotificationsAllowedUI: Boolean
        get() = Notificare.push().allowedUI

    private val hasNotificationsEnabled: Boolean
        get() = Notificare.push().hasRemoteNotificationsEnabled && Notificare.push().allowedUI

    private val hasNotificationsPermission: Boolean
        get() = Notificare.push().hasNotificationsPermission

    private val hasDndEnabled: Boolean
        get() = Notificare.device().currentDevice?.dnd != null

    private val appInfo: ApplicationInfo?
        get() {
            val application = Notificare.application
            if (application != null) {
                return ApplicationInfo(application.name, application.id)
            }

            return null
        }

    init {
        Notificare.addListener(this)

        viewModelScope.launch {
            Notificare.push().observableAllowedUI
                .asFlow()
                .collect { enabled ->
                    _notificationsEnabled.postValue(enabled)
                    _notificationsAllowedUI.postValue(hasNotificationsAllowedUI)
                    _remoteNotificationsEnabled.postValue(hasRemoteNotificationsEnabled)
                }
        }

        viewModelScope.launch {
            Notificare.push().observableSubscription
                .asFlow()
                .collect { subscription ->
                    Timber.d("subscription changed = $subscription")
                }
        }

        viewModelScope.launch {
            NotificationEvent.inboxShouldUpdateFlow.collect {
                refreshBadge()
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        Notificare.removeListener(this)
    }

    override fun onReady(application: NotificareApplication) {
        updateNotificareReadyStatus()
        updateApplicationInfo()
    }

    override fun onUnlaunched() {
        updateNotificareReadyStatus()
    }

    internal fun updateRemoteNotificationsStatus(enabled: Boolean) {
        viewModelScope.launch {
            try {
                if (enabled) {
                    Notificare.push().enableRemoteNotifications()
                    if (_hasNotificationsPermissions.value != true) {
                        _hasNotificationsPermissions.postValue(hasNotificationsPermission)
                    }
                } else {
                    Notificare.push().disableRemoteNotifications()
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to update remote notifications registration.")
            }
        }
    }

    internal fun updateDndStatus(enabled: Boolean) {
        viewModelScope.launch {
            try {
                if (enabled) {
                    Notificare.device().updateDoNotDisturb(NotificareDoNotDisturb.default)
                } else {
                    Notificare.device().clearDoNotDisturb()
                }

                _dndEnabled.postValue(enabled)
                _dnd.postValue(NotificareDoNotDisturb.default)

                Timber.i("DnD updates successfully.")
                showSnackBar("DnD updates successfully.")
            } catch (e: Exception) {
                Timber.e(e, "Failed to update the do not disturb settings.")
                showSnackBar("Failed to update DnD: ${e.message}")
            }
        }
    }

    internal fun updateDndTime(dnd: NotificareDoNotDisturb) {
        viewModelScope.launch {
            try {
                Notificare.device().updateDoNotDisturb(dnd)
                _dnd.postValue(dnd)

                Timber.i("DnD time updated successfully.")
                showSnackBar("DnD time updated successfully.")
            } catch (e: Exception) {
                Timber.e(e, "Failed to update the do not disturb settings.")
                showSnackBar("Failed to update DnD: ${e.message}")
            }
        }
    }

    internal fun refreshBadge() {
        if (accessToken == null) {
            Timber.e("Failed refreshing badge, access token is missing.")
            showSnackBar("Failed refreshing badge, access token is missing.")

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

                _badge.postValue(userInboxResponse.unread)

                Timber.i("Refresh badge success.")
                showSnackBar("Refresh badge success.")
            } catch (e: Exception) {
                Timber.e(e, "Failed to refresh badge.")
                showSnackBar("Failed to refresh badge.")
            }
        }
    }

    internal suspend fun loginWithBrowser(context: Context, account: Auth0): String {
        val credentials = WebAuthProvider.login(account)
            .withScheme(AUTH_SCHEME)
            .await(context)

        accessToken = credentials.accessToken
        _isLoggedIn.postValue(true)

        return credentials.accessToken
    }

    internal suspend fun logoutWithBrowser(context: Context, account: Auth0) {
        WebAuthProvider.logout(account)
            .withScheme(AUTH_SCHEME)
            .await(context)

        accessToken = null
        _isLoggedIn.postValue(false)
    }

    internal suspend fun registerDevice(token: String) {
        if (_deviceRegistered.value == true) {
            Timber.i("Device already registered, skipping.")
            return
        }

        val device = Notificare.device().currentDevice
            ?: throw Exception("Notificare current device is null, can not assign device to user without device ID.")

        SampleRequest.Builder()
            .authentication("Bearer $token")
            .put("$registerDeviceUrl/${device.id}", null)
            .response()

        _deviceRegistered.postValue(true)
    }

    private fun updateNotificareReadyStatus() {
        _notificareReady.postValue(Notificare.isReady)
    }

    private fun updateApplicationInfo() {
        applicationInfo.postValue(appInfo)
    }

    private val NotificareDoNotDisturb.Companion.default: NotificareDoNotDisturb
        get() = NotificareDoNotDisturb(
            NotificareDoNotDisturb.defaultStart,
            NotificareDoNotDisturb.defaultEnd,
        )

    private val NotificareDoNotDisturb.Companion.defaultStart: NotificareTime
        get() = NotificareTime(hours = 23, minutes = 0)

    private val NotificareDoNotDisturb.Companion.defaultEnd: NotificareTime
        get() = NotificareTime(hours = 8, minutes = 0)

    companion object {
        private const val AUTH_SCHEME = "auth.re.notifica.sample.user.inbox.app.dev"
    }
}
