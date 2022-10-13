package re.notifica.sample.ui.main

import androidx.lifecycle.*
import kotlinx.coroutines.launch
import re.notifica.Notificare
import re.notifica.geo.ktx.geo
import re.notifica.iam.ktx.inAppMessaging
import re.notifica.ktx.device
import re.notifica.models.NotificareDevice
import re.notifica.models.NotificareDoNotDisturb
import re.notifica.models.NotificareTime
import re.notifica.push.ktx.push
import re.notifica.sample.ktx.*
import re.notifica.sample.models.BaseViewModel
import timber.log.Timber

class MainViewModel : BaseViewModel(), DefaultLifecycleObserver {
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

    private val _hasLocationUpdatesEnabled = MutableLiveData(isLocationUpdatesEnabled)
    val hasLocationUpdatesEnabled: LiveData<Boolean> = _hasLocationUpdatesEnabled

    private val _dndEnabled = MutableLiveData(hasDndEnabled)
    val dndEnabled: LiveData<Boolean> = _dndEnabled

    private val _dnd = MutableLiveData(Notificare.device().currentDevice?.dnd ?: NotificareDoNotDisturb.default)
    val dnd: LiveData<NotificareDoNotDisturb> = _dnd

    private val _locationPermission = MutableLiveData(checkLocationPermission)
    val locationPermission: LiveData<LocationPermission> = _locationPermission

    private val _hasBluetoothEnabled = MutableLiveData(checkBluetoothEnabled)
    val hasBluetoothEnabled: LiveData<Boolean> = _hasBluetoothEnabled

    private val _hasBluetoothPermission = MutableLiveData(checkBluetoothPermission)
    val hasBluetoothPermission: LiveData<Boolean> = _hasBluetoothPermission

    private val _iamSuppressed = MutableLiveData(isIamSuppressed)
    val iamSuppressed: LiveData<Boolean> = _iamSuppressed

    private val _deviceRegistrationData = MutableLiveData(deviceData)
    val deviceRegistrationData: LiveData<NotificareDevice?> = _deviceRegistrationData

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

    private val isLocationUpdatesEnabled: Boolean
        get() = Notificare.geo().hasLocationServicesEnabled && Notificare.geo().hasForegroundTrackingCapabilities

    private val checkLocationPermission: LocationPermission
        get() {
            if (Notificare.geo().hasForegroundTrackingCapabilities && Notificare.geo().hasBackgroundTrackingCapabilities) {
                return LocationPermission.BACKGROUND
            }

            if (Notificare.geo().hasForegroundTrackingCapabilities) {
                return LocationPermission.FOREGROUND
            }

            return LocationPermission.NONE
        }

    private val checkBluetoothEnabled: Boolean
        get() = Notificare.geo().hasBluetoothEnabled

    private val checkBluetoothPermission: Boolean
        get() = Notificare.geo().hasBluetoothCapabilities

    private val isIamSuppressed: Boolean
        get() = Notificare.inAppMessaging().hasMessagesSuppressed

    private val deviceData: NotificareDevice?
        get() = Notificare.device().currentDevice

    init {
        viewModelScope.launch {
            Notificare.push().observableAllowedUI
                .asFlow()
                .collect { enabled ->
                    _notificationsEnabled.postValue(enabled)
                    _notificationsAllowedUI.postValue(hasNotificationsAllowedUI)
                    _remoteNotificationsEnabled.postValue(hasRemoteNotificationsEnabled)
                }
        }
    }

    fun updateNotificareReady() {
        _notificareReady.value = Notificare.isReady
    }

    fun changeRemoteNotifications(enabled: Boolean) {
        if (enabled) {
            Notificare.push().enableRemoteNotifications()
            if (_hasNotificationsPermissions.value != true) {
                _hasNotificationsPermissions.postValue(hasNotificationsPermission)
            }
        } else {
            Notificare.push().disableRemoteNotifications()
        }
    }

    fun changeDoNotDisturbEnabled(enabled: Boolean) {
        viewModelScope.launch {
            try {
                if (enabled) {
                    Notificare.device().updateDoNotDisturb(NotificareDoNotDisturb.default)
                } else {
                    Notificare.device().clearDoNotDisturb()
                }

                _dndEnabled.postValue(enabled)
                _dnd.postValue(NotificareDoNotDisturb.default)
            } catch (e: Exception) {
                Timber.e(e, "Failed to update the do not disturb settings.")
                showSnackBar("Failed to update DnD: ${e.message}")
            }
        }
    }

    fun changeDoNotDisturb(dnd: NotificareDoNotDisturb) {
        viewModelScope.launch {
            try {
                Notificare.device().updateDoNotDisturb(dnd)
                _dnd.postValue(dnd)
            } catch (e: Exception) {
                Timber.e(e, "Failed to update the do not disturb settings.")
                showSnackBar("Failed to update DnD: ${e.message}")
            }
        }
    }

    fun changeLocationUpdates(enabled: Boolean) {
        if (enabled) {
            Notificare.geo().enableLocationUpdates()
        } else {
            Notificare.geo().disableLocationUpdates()
        }

        _hasLocationUpdatesEnabled.postValue(isLocationUpdatesEnabled)
        _locationPermission.postValue(checkLocationPermission)
        _hasBluetoothEnabled.postValue(checkBluetoothEnabled)
        _hasBluetoothPermission.postValue(checkBluetoothPermission)
    }

    fun changeIamSuppressed(suppressed: Boolean) {
        Notificare.inAppMessaging().hasMessagesSuppressed = suppressed
        _iamSuppressed.postValue(isIamSuppressed)
    }

    fun registerDevice(userID: String?, userName: String?) {
        viewModelScope.launch {
            try {
                Notificare.device().register(userID, userName)
            } catch (e: Exception) {
                Timber.e(e, "Failed to register device.")
                showSnackBar("Failed to register device: ${e.message}")
                return@launch
            }

            _deviceRegistrationData.postValue(deviceData)
        }
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
}
