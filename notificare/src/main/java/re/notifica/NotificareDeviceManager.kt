package re.notifica

import android.content.Intent
import androidx.annotation.RestrictTo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import re.notifica.*
import re.notifica.internal.NotificareUtils
import re.notifica.internal.common.filterNotNull
import re.notifica.internal.common.toByteArray
import re.notifica.internal.common.toHex
import re.notifica.internal.network.push.*
import re.notifica.internal.network.request.NotificareRequest
import re.notifica.models.NotificareDevice
import re.notifica.models.NotificareDoNotDisturb
import re.notifica.models.NotificareTransport
import re.notifica.models.NotificareUserData
import java.util.*

class NotificareDeviceManager {

    var currentDevice: NotificareDevice?
        get() = Notificare.sharedPreferences.device
        private set(value) {
            Notificare.sharedPreferences.device = value
        }

    val preferredLanguage: String?
        get() {
            val preferredLanguage = Notificare.sharedPreferences.preferredLanguage ?: return null
            val preferredRegion = Notificare.sharedPreferences.preferredRegion ?: return null

            return "$preferredLanguage-$preferredRegion"
        }

    fun configure() {}

    suspend fun launch() {
        val device = currentDevice

        if (device != null) {
            if (device.appVersion != NotificareUtils.applicationVersion) {
                // It's not the same version, let's log it as an upgrade.
                NotificareLogger.debug("New version detected")
                Notificare.eventsManager.logApplicationUpgrade()
            }

            register(
                transport = device.transport,
                token = device.id,
                userId = device.userId,
                userName = device.userName,
            )
        } else {
            NotificareLogger.debug("New install detected")

            // Let's logout the user in case there's an account in the keychain
            // TODO: [[NotificareAuth shared] logoutAccount]

            try {
                registerTemporary()

                // We will log the Install & Registration events here since this will execute only one time at the start.
                Notificare.eventsManager.logApplicationInstall()
                Notificare.eventsManager.logApplicationRegistration()
            } catch (e: Exception) {
                NotificareLogger.warning("Failed to register temporary device.", e)
                throw e
            }
        }
    }

    suspend fun register(userId: String?, userName: String?): Unit = withContext(Dispatchers.IO) {
        val currentDevice = checkNotificareReady()
        register(currentDevice.transport, currentDevice.id, userId, userName)
    }

    fun register(userId: String?, userName: String?, callback: NotificareCallback<Unit>) {
        GlobalScope.launch {
            try {
                register(userId, userName)
                callback.onSuccess(Unit)
            } catch (e: Exception) {
                callback.onFailure(e)
            }
        }
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    suspend fun registerTemporary(): Unit = withContext(Dispatchers.IO) {
        val device = currentDevice

        // NOTE: keep the same token if available and only when not changing transport providers.
        val token = when {
            device != null && device.transport == NotificareTransport.NOTIFICARE -> device.id
            else -> UUID.randomUUID().toByteArray().toHex()
        }

        register(
            transport = NotificareTransport.NOTIFICARE,
            token = token,
            userId = currentDevice?.userId,
            userName = currentDevice?.userName,
        )
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    suspend fun registerPushToken(transport: NotificareTransport, token: String): Unit = withContext(Dispatchers.IO) {
        if (transport == NotificareTransport.NOTIFICARE) {
            throw IllegalArgumentException("Invalid transport '$transport'.")
        }

        register(
            transport = transport,
            token = token,
            userId = currentDevice?.userId,
            userName = currentDevice?.userName,
        )
    }

    suspend fun updatePreferredLanguage(preferredLanguage: String?): Unit = withContext(Dispatchers.IO) {
        checkNotificareReady()

        if (preferredLanguage != null) {
            val parts = preferredLanguage.split("-")
            if (
                parts.size != 2 ||
                !Locale.getISOLanguages().contains(parts[0]) ||
                !Locale.getISOCountries().contains(parts[1])
            ) {
                throw IllegalArgumentException("Invalid preferred language value: $preferredLanguage")
            }

            Notificare.sharedPreferences.preferredLanguage = parts[0]
            Notificare.sharedPreferences.preferredRegion = parts[1]

            updateLanguage()
        } else {
            Notificare.sharedPreferences.preferredLanguage = null
            Notificare.sharedPreferences.preferredRegion = null

            updateLanguage()
        }
    }

    fun updatePreferredLanguage(preferredLanguage: String?, callback: NotificareCallback<Unit>) {
        GlobalScope.launch {
            try {
                updatePreferredLanguage(preferredLanguage)
                callback.onSuccess(Unit)
            } catch (e: Exception) {
                callback.onFailure(e)
            }
        }
    }

    suspend fun fetchTags(): List<String> = withContext(Dispatchers.IO) {
        val device = checkNotificareReady()

        NotificareRequest.Builder()
            .get("/device/${device.id}/tags")
            .responseDecodable(DeviceTagsResponse::class)
            .tags
    }

    fun fetchTags(callback: NotificareCallback<List<String>>) {
        GlobalScope.launch {
            try {
                val tags = fetchTags()
                callback.onSuccess(tags)
            } catch (e: Exception) {
                callback.onFailure(e)
            }
        }
    }

    suspend fun addTag(tag: String): Unit = withContext(Dispatchers.IO) {
        addTags(listOf(tag))
    }

    fun addTag(tag: String, callback: NotificareCallback<Unit>) {
        GlobalScope.launch {
            try {
                addTag(tag)
                callback.onSuccess(Unit)
            } catch (e: Exception) {
                callback.onFailure(e)
            }
        }
    }

    suspend fun addTags(tags: List<String>): Unit = withContext(Dispatchers.IO) {
        val device = checkNotificareReady()
        NotificareRequest.Builder()
            .put("/device/${device.id}/addtags", DeviceTagsPayload(tags))
            .response()
    }

    fun addTags(tags: List<String>, callback: NotificareCallback<Unit>) {
        GlobalScope.launch {
            try {
                addTags(tags)
                callback.onSuccess(Unit)
            } catch (e: Exception) {
                callback.onFailure(e)
            }
        }
    }

    suspend fun removeTag(tag: String): Unit = withContext(Dispatchers.IO) {
        removeTags(listOf(tag))
    }

    fun removeTag(tag: String, callback: NotificareCallback<Unit>) {
        GlobalScope.launch {
            try {
                removeTag(tag)
                callback.onSuccess(Unit)
            } catch (e: Exception) {
                callback.onFailure(e)
            }
        }
    }

    suspend fun removeTags(tags: List<String>): Unit = withContext(Dispatchers.IO) {
        val device = checkNotificareReady()
        NotificareRequest.Builder()
            .put("/device/${device.id}/removetags", DeviceTagsPayload(tags))
            .response()
    }

    fun removeTags(tags: List<String>, callback: NotificareCallback<Unit>) {
        GlobalScope.launch {
            try {
                removeTags(tags)
                callback.onSuccess(Unit)
            } catch (e: Exception) {
                callback.onFailure(e)
            }
        }
    }

    suspend fun clearTags(): Unit = withContext(Dispatchers.IO) {
        val device = checkNotificareReady()
        NotificareRequest.Builder()
            .put("/device/${device.id}/cleartags", null)
            .response()
    }

    fun clearTags(callback: NotificareCallback<Unit>) {
        GlobalScope.launch {
            try {
                clearTags()
                callback.onSuccess(Unit)
            } catch (e: Exception) {
                callback.onFailure(e)
            }
        }
    }

    suspend fun fetchDoNotDisturb(): NotificareDoNotDisturb? = withContext(Dispatchers.IO) {
        val device = checkNotificareReady()
        val dnd = NotificareRequest.Builder()
            .get("/device/${device.id}/dnd")
            .responseDecodable(DeviceDoNotDisturbResponse::class)
            .dnd

        // Update current device properties.
        currentDevice?.dnd = dnd

        return@withContext dnd
    }

    fun fetchDoNotDisturb(callback: NotificareCallback<NotificareDoNotDisturb?>) {
        GlobalScope.launch {
            try {
                val dnd = fetchDoNotDisturb()
                callback.onSuccess(dnd)
            } catch (e: Exception) {
                callback.onFailure(e)
            }
        }
    }

    suspend fun updateDoNotDisturb(dnd: NotificareDoNotDisturb): Unit = withContext(Dispatchers.IO) {
        val device = checkNotificareReady()
        NotificareRequest.Builder()
            .put("/device/${device.id}/dnd", dnd)
            .response()

        // Update current device properties.
        currentDevice?.dnd = dnd
    }

    fun updateDoNotDisturb(dnd: NotificareDoNotDisturb, callback: NotificareCallback<Unit>) {
        GlobalScope.launch {
            try {
                updateDoNotDisturb(dnd)
                callback.onSuccess(Unit)
            } catch (e: Exception) {
                callback.onFailure(e)
            }
        }
    }

    suspend fun clearDoNotDisturb(): Unit = withContext(Dispatchers.IO) {
        val device = checkNotificareReady()
        NotificareRequest.Builder()
            .put("/device/${device.id}/cleardnd", null)
            .response()

        // Update current device properties.
        currentDevice?.dnd = null
    }

    fun clearDoNotDisturb(callback: NotificareCallback<Unit>) {
        GlobalScope.launch {
            try {
                clearDoNotDisturb()
                callback.onSuccess(Unit)
            } catch (e: Exception) {
                callback.onFailure(e)
            }
        }
    }

    suspend fun fetchUserData(): NotificareUserData = withContext(Dispatchers.IO) {
        val device = checkNotificareReady()
        val userData = NotificareRequest.Builder()
            .get("/device/${device.id}/userdata")
            .responseDecodable(DeviceUserDataResponse::class)
            .userData?.filterNotNull { it.value }
            ?: mapOf()

        // Update current device properties.
        currentDevice?.userData = userData

        return@withContext userData
    }

    fun fetchUserData(callback: NotificareCallback<NotificareUserData>) {
        GlobalScope.launch {
            try {
                val userData = fetchUserData()
                callback.onSuccess(userData)
            } catch (e: Exception) {
                callback.onFailure(e)
            }
        }
    }

    suspend fun updateUserData(userData: NotificareUserData): Unit = withContext(Dispatchers.IO) {
        val device = checkNotificareReady()
        NotificareRequest.Builder()
            .put("/device/${device.id}/userdata", userData)
            .response()

        // Update current device properties.
        currentDevice?.userData = userData
    }

    fun updateUserData(userData: NotificareUserData, callback: NotificareCallback<Unit>) {
        GlobalScope.launch {
            try {
                updateUserData(userData)
                callback.onSuccess(Unit)
            } catch (e: Exception) {
                callback.onFailure(e)
            }
        }
    }

    suspend fun updateNotificationSettings(allowedUI: Boolean): Unit = withContext(Dispatchers.IO) {
        val device = checkNotificareReady()

        NotificareRequest.Builder()
            .put(
                url = "/device/${device.id}",
                body = DeviceUpdateNotificationSettingsPayload(
                    language = getLanguage(),
                    region = getRegion(),
                    allowedUI = allowedUI,
                ),
            )
            .response()

        // Update current device properties.
        currentDevice?.allowedUI = allowedUI
    }

    fun updateNotificationSettings(allowedUI: Boolean, callback: NotificareCallback<Unit>) {
        GlobalScope.launch {
            try {
                updateNotificationSettings(allowedUI)
                callback.onSuccess(Unit)
            } catch (e: Exception) {
                callback.onFailure(e)
            }
        }
    }

    internal suspend fun delete(): Unit = withContext(Dispatchers.IO) {
        val device = checkNotificareReady()

        NotificareRequest.Builder()
            .delete(
                url = "/device/${device.id}",
                body = null,
            )
            .response()

        // Remove current device.
        currentDevice = null
    }

    // region Private API

    private fun checkNotificareReady(): NotificareDevice {
        val device = currentDevice

        if (!Notificare.isReady || device == null) {
            throw NotificareException.NotReady()
        }

        return device
    }

    private suspend fun register(transport: NotificareTransport, token: String, userId: String?, userName: String?) {
        if (registrationChanged(token, userId, userName)) {
            val currentDevice = currentDevice

            val oldDeviceId =
                if (currentDevice?.id != null && currentDevice.id != token) currentDevice.id
                else null

            val deviceRegistration = DeviceRegistrationPayload(
                deviceId = token,
                oldDeviceId = oldDeviceId,
                userId = userId,
                userName = userName,
                language = getLanguage(),
                region = getRegion(),
                platform = "Android",
                transport = transport,
                osVersion = NotificareUtils.osVersion,
                sdkVersion = Notificare.SDK_VERSION,
                appVersion = NotificareUtils.applicationVersion,
                deviceString = NotificareUtils.deviceString,
                timeZoneOffset = NotificareUtils.timeZoneOffset,
                allowedUI = if (transport == NotificareTransport.NOTIFICARE) false else currentDevice?.allowedUI
                    ?: false,
                backgroundAppRefresh = true,
                locationServicesAuthStatus = "none", // TODO me
                bluetoothEnabled = false, // TODO me
            )

            NotificareRequest.Builder()
                .post("/device", deviceRegistration)
                .response()

            deviceRegistration.toStoredDevice(currentDevice).also {
                this.currentDevice = it
            }
        } else {
            NotificareLogger.info("Skipping device registration, nothing changed.")
        }

        // Send a device registered broadcast.
        Notificare.requireContext().sendBroadcast(
            Intent(Notificare.requireContext(), Notificare.intentReceiver)
                .setAction(NotificareIntentReceiver.INTENT_ACTION_DEVICE_REGISTERED)
                .putExtra(NotificareIntentReceiver.INTENT_EXTRA_DEVICE, checkNotNull(currentDevice))
        )
    }

    private fun registrationChanged(token: String?, userId: String?, userName: String?): Boolean {
        val device = currentDevice ?: run {
            NotificareLogger.debug("Registration check: fresh installation")
            return true
        }

        var changed = false

        if (device.userId != userId) {
            NotificareLogger.debug("Registration check: user id changed")
            changed = true
        }

        if (device.userName != userName) {
            NotificareLogger.debug("Registration check: user name changed")
            changed = true
        }

        if (device.id != token) {
            NotificareLogger.debug("Registration check: device token changed")
            changed = true
        }

        if (device.deviceString != NotificareUtils.deviceString) {
            NotificareLogger.debug("Registration check: device string changed")
            changed = true
        }

        if (device.appVersion != NotificareUtils.applicationVersion) {
            NotificareLogger.debug("Registration check: application version changed")
            changed = true
        }

        if (device.osVersion != NotificareUtils.osVersion) {
            NotificareLogger.debug("Registration check: os version changed")
            changed = true
        }

        if (device.sdkVersion != Notificare.SDK_VERSION) {
            NotificareLogger.debug("Registration check: sdk version changed")
            changed = true
        }

        if (device.timeZoneOffset != NotificareUtils.timeZoneOffset) {
            NotificareLogger.debug("Registration check: timezone offset changed")
            changed = true
        }

        if (device.language != getLanguage()) {
            NotificareLogger.debug("Registration check: language changed")
            changed = true
        }

        if (device.region != getRegion()) {
            NotificareLogger.debug("Registration check: region changed")
            changed = true
        }

        val oneDayAgo = GregorianCalendar().apply { add(Calendar.DAY_OF_YEAR, -1) }.time
        if (device.lastRegistered.before(oneDayAgo)) {
            NotificareLogger.debug("Registration check: region changed")
            changed = true
        }

        // TODO check the properties below
        // v2 Android checks for:
        // transport, allowedUI, locationServicesAuthStatus, bluetoothEnabled

        return changed
    }

    private fun getLanguage(): String {
        return Notificare.sharedPreferences.preferredLanguage ?: NotificareUtils.deviceLanguage
    }

    private fun getRegion(): String {
        return Notificare.sharedPreferences.preferredRegion ?: NotificareUtils.deviceRegion
    }

    internal fun registerTestDevice(nonce: String, callback: NotificareCallback<Unit>) {
        GlobalScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    NotificareRequest.Builder()
                        .put(
                            url = "/support/testdevice/$nonce",
                            body = TestDeviceRegistrationPayload(
                                deviceId = checkNotNull(currentDevice).id,
                            ),
                        )
                        .response()

                    callback.onSuccess(Unit)
                } catch (e: Exception) {
                    callback.onFailure(e)
                }
            }
        }
    }

    internal suspend fun updateLanguage() {
        val device = checkNotificareReady()

        NotificareRequest.Builder()
            .put(
                url = "/device/${device.id}",
                body = DeviceUpdateLanguagePayload(
                    language = getLanguage(),
                    region = getRegion(),
                ),
            )
            .response()
    }

    internal suspend fun updateTimeZone() {
        val device = checkNotificareReady()

        NotificareRequest.Builder()
            .put(
                url = "/device/${device.id}",
                body = DeviceUpdateTimeZonePayload(
                    language = getLanguage(),
                    region = getRegion(),
                    timeZoneOffset = NotificareUtils.timeZoneOffset,
                ),
            )
            .response()
    }

    // endregion
}

private fun DeviceRegistrationPayload.toStoredDevice(previous: NotificareDevice?): NotificareDevice {
    return NotificareDevice(
        id = this.deviceId,
        userId = this.userId,
        userName = this.userName,
        timeZoneOffset = this.timeZoneOffset,
        osVersion = this.osVersion,
        sdkVersion = this.sdkVersion,
        appVersion = this.appVersion,
        deviceString = this.deviceString,
        language = this.language,
        region = this.region,
        transport = this.transport,
        dnd = previous?.dnd,
        userData = previous?.userData ?: mapOf(),
        lastRegistered = Date(),
        allowedUI = this.allowedUI,
    )
}
