package re.notifica.internal.modules

import android.content.Intent
import androidx.annotation.Keep
import java.util.Calendar
import java.util.Date
import java.util.GregorianCalendar
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import re.notifica.Notificare
import re.notifica.NotificareCallback
import re.notifica.NotificareDeviceModule
import re.notifica.NotificareInternalDeviceModule
import re.notifica.NotificareNotReadyException
import re.notifica.internal.NotificareLogger
import re.notifica.internal.NotificareModule
import re.notifica.internal.NotificareUtils
import re.notifica.internal.common.filterNotNull
import re.notifica.internal.common.toByteArray
import re.notifica.internal.common.toHex
import re.notifica.internal.ktx.coroutineScope
import re.notifica.internal.ktx.toCallbackFunction
import re.notifica.internal.network.push.DeviceDoNotDisturbResponse
import re.notifica.internal.network.push.DeviceRegistrationPayload
import re.notifica.internal.network.push.DeviceTagsPayload
import re.notifica.internal.network.push.DeviceTagsResponse
import re.notifica.internal.network.push.DeviceUpdateLanguagePayload
import re.notifica.internal.network.push.DeviceUpdateTimeZonePayload
import re.notifica.internal.network.push.DeviceUserDataResponse
import re.notifica.internal.network.push.TestDeviceRegistrationPayload
import re.notifica.internal.network.request.NotificareRequest
import re.notifica.ktx.eventsImplementation
import re.notifica.ktx.session
import re.notifica.models.NotificareDevice
import re.notifica.models.NotificareDoNotDisturb
import re.notifica.models.NotificareTransport
import re.notifica.models.NotificareUserData

@Keep
internal object NotificareDeviceModuleImpl :
    NotificareModule(),
    NotificareDeviceModule,
    NotificareInternalDeviceModule {

    // region Notificare Module

    override suspend fun launch() {
        val device = currentDevice

        if (device != null) {
            register(
                transport = device.transport,
                token = device.id,
                userId = device.userId,
                userName = device.userName,
            )

            // Ensure a session exists for the current device.
            Notificare.session().launch()

            if (device.appVersion != NotificareUtils.applicationVersion) {
                // It's not the same version, let's log it as an upgrade.
                NotificareLogger.debug("New version detected")
                Notificare.eventsImplementation().logApplicationUpgrade()
            }
        } else {
            NotificareLogger.debug("New install detected")

            try {
                registerTemporary()

                // Ensure a session exists for the current device.
                Notificare.session().launch()

                // We will log the Install & Registration events here since this will execute only one time at the start.
                Notificare.eventsImplementation().logApplicationInstall()
                Notificare.eventsImplementation().logApplicationRegistration()
            } catch (e: Exception) {
                NotificareLogger.warning("Failed to register temporary device.", e)
                throw e
            }
        }
    }

    override suspend fun postLaunch() {
        val device = currentDevice
        if (device != null) notifyDeviceRegistered(device)
    }

    // endregion

    // region Notificare Device Module

    override var currentDevice: NotificareDevice?
        get() = Notificare.sharedPreferences.device
        private set(value) {
            Notificare.sharedPreferences.device = value
        }

    override val preferredLanguage: String?
        get() {
            val preferredLanguage = Notificare.sharedPreferences.preferredLanguage ?: return null
            val preferredRegion = Notificare.sharedPreferences.preferredRegion ?: return null

            return "$preferredLanguage-$preferredRegion"
        }

    override suspend fun register(userId: String?, userName: String?): Unit = withContext(
        Dispatchers.IO
    ) {
        checkPrerequisites()

        val device = checkNotNull(currentDevice)
        register(device.transport, device.id, userId, userName)
    }

    override fun register(userId: String?, userName: String?, callback: NotificareCallback<Unit>): Unit =
        toCallbackFunction(::register)(userId, userName, callback)

    override suspend fun updatePreferredLanguage(preferredLanguage: String?): Unit = withContext(Dispatchers.IO) {
        checkPrerequisites()

        if (preferredLanguage != null) {
            val parts = preferredLanguage.split("-")
            if (
                parts.size != 2 ||
                !Locale.getISOLanguages().contains(parts[0]) ||
                !Locale.getISOCountries().contains(parts[1])
            ) {
                throw IllegalArgumentException("Invalid preferred language value: $preferredLanguage")
            }

            val language = parts[0]
            val region = parts[1]
            updateLanguage(language, region)

            Notificare.sharedPreferences.preferredLanguage = language
            Notificare.sharedPreferences.preferredRegion = region
        } else {
            val language = NotificareUtils.deviceLanguage
            val region = NotificareUtils.deviceRegion
            updateLanguage(language, region)

            Notificare.sharedPreferences.preferredLanguage = null
            Notificare.sharedPreferences.preferredRegion = null
        }
    }

    override fun updatePreferredLanguage(preferredLanguage: String?, callback: NotificareCallback<Unit>): Unit =
        toCallbackFunction(::updatePreferredLanguage)(preferredLanguage, callback)

    override suspend fun fetchTags(): List<String> = withContext(Dispatchers.IO) {
        checkPrerequisites()

        val device = checkNotNull(currentDevice)

        NotificareRequest.Builder()
            .get("/device/${device.id}/tags")
            .responseDecodable(DeviceTagsResponse::class)
            .tags
    }

    override fun fetchTags(callback: NotificareCallback<List<String>>): Unit = toCallbackFunction(::fetchTags)(callback)

    override suspend fun addTag(tag: String): Unit = withContext(Dispatchers.IO) {
        addTags(listOf(tag))
    }

    override fun addTag(tag: String, callback: NotificareCallback<Unit>): Unit =
        toCallbackFunction(::addTag)(tag, callback)

    override suspend fun addTags(tags: List<String>): Unit = withContext(Dispatchers.IO) {
        checkPrerequisites()

        val device = checkNotNull(currentDevice)
        NotificareRequest.Builder()
            .put("/device/${device.id}/addtags", DeviceTagsPayload(tags))
            .response()
    }

    override fun addTags(tags: List<String>, callback: NotificareCallback<Unit>): Unit =
        toCallbackFunction(::addTags)(tags, callback)

    override suspend fun removeTag(tag: String): Unit = withContext(Dispatchers.IO) {
        removeTags(listOf(tag))
    }

    override fun removeTag(tag: String, callback: NotificareCallback<Unit>): Unit =
        toCallbackFunction(::removeTag)(tag, callback)

    override suspend fun removeTags(tags: List<String>): Unit = withContext(Dispatchers.IO) {
        checkPrerequisites()

        val device = checkNotNull(currentDevice)
        NotificareRequest.Builder()
            .put("/device/${device.id}/removetags", DeviceTagsPayload(tags))
            .response()
    }

    override fun removeTags(tags: List<String>, callback: NotificareCallback<Unit>): Unit =
        toCallbackFunction(::removeTags)(tags, callback)

    override suspend fun clearTags(): Unit = withContext(Dispatchers.IO) {
        checkPrerequisites()

        val device = checkNotNull(currentDevice)
        NotificareRequest.Builder()
            .put("/device/${device.id}/cleartags", null)
            .response()
    }

    override fun clearTags(callback: NotificareCallback<Unit>): Unit = toCallbackFunction(::clearTags)(callback)

    override suspend fun fetchDoNotDisturb(): NotificareDoNotDisturb? = withContext(Dispatchers.IO) {
        checkPrerequisites()

        val device = checkNotNull(currentDevice)
        val dnd = NotificareRequest.Builder()
            .get("/device/${device.id}/dnd")
            .responseDecodable(DeviceDoNotDisturbResponse::class)
            .dnd

        // Update current device properties.
        currentDevice = device.copy(dnd = dnd)

        return@withContext dnd
    }

    override fun fetchDoNotDisturb(callback: NotificareCallback<NotificareDoNotDisturb?>): Unit =
        toCallbackFunction(::fetchDoNotDisturb)(callback)

    override suspend fun updateDoNotDisturb(dnd: NotificareDoNotDisturb): Unit = withContext(Dispatchers.IO) {
        checkPrerequisites()

        val device = checkNotNull(currentDevice)
        NotificareRequest.Builder()
            .put("/device/${device.id}/dnd", dnd)
            .response()

        // Update current device properties.
        currentDevice = device.copy(dnd = dnd)
    }

    override fun updateDoNotDisturb(dnd: NotificareDoNotDisturb, callback: NotificareCallback<Unit>): Unit =
        toCallbackFunction(::updateDoNotDisturb)(dnd, callback)

    override suspend fun clearDoNotDisturb(): Unit = withContext(Dispatchers.IO) {
        checkPrerequisites()

        val device = checkNotNull(currentDevice)
        NotificareRequest.Builder()
            .put("/device/${device.id}/cleardnd", null)
            .response()

        // Update current device properties.
        currentDevice = device.copy(dnd = null)
    }

    override fun clearDoNotDisturb(callback: NotificareCallback<Unit>): Unit =
        toCallbackFunction(::clearDoNotDisturb)(callback)

    override suspend fun fetchUserData(): NotificareUserData = withContext(Dispatchers.IO) {
        checkPrerequisites()

        val device = checkNotNull(currentDevice)
        val userData = NotificareRequest.Builder()
            .get("/device/${device.id}/userdata")
            .responseDecodable(DeviceUserDataResponse::class)
            .userData?.filterNotNull { it.value }
            ?: mapOf()

        // Update current device properties.
        currentDevice = device.copy(userData = userData)

        return@withContext userData
    }

    override fun fetchUserData(callback: NotificareCallback<NotificareUserData>): Unit =
        toCallbackFunction(::fetchUserData)(callback)

    override suspend fun updateUserData(userData: NotificareUserData): Unit = withContext(Dispatchers.IO) {
        checkPrerequisites()

        val device = checkNotNull(currentDevice)
        NotificareRequest.Builder()
            .put("/device/${device.id}/userdata", userData)
            .response()

        // Update current device properties.
        currentDevice = device.copy(userData = userData)
    }

    override fun updateUserData(userData: NotificareUserData, callback: NotificareCallback<Unit>): Unit =
        toCallbackFunction(::updateUserData)(userData, callback)

    // endregion

    // region Notificare Internal Device Module

    override suspend fun registerTemporary(): Unit = withContext(Dispatchers.IO) {
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

    override suspend fun registerPushToken(transport: NotificareTransport, token: String): Unit =
        withContext(Dispatchers.IO) {
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

    // endregion

    internal suspend fun delete(): Unit = withContext(Dispatchers.IO) {
        checkPrerequisites()

        val device = checkNotNull(currentDevice)

        NotificareRequest.Builder()
            .delete(
                url = "/device/${device.id}",
                body = null,
            )
            .response()

        // Remove current device.
        currentDevice = null
    }

    @Throws
    private fun checkPrerequisites() {
        if (!Notificare.isReady) {
            NotificareLogger.warning("Notificare is not ready yet.")
            throw NotificareNotReadyException()
        }
    }

    private suspend fun register(transport: NotificareTransport, token: String, userId: String?, userName: String?) {
        if (registrationChanged(token, userId, userName)) {
            val currentDevice = currentDevice

            val oldDeviceId =
                if (currentDevice?.id != null && currentDevice.id != token) {
                    currentDevice.id
                } else {
                    null
                }

            val deviceRegistration = DeviceRegistrationPayload(
                deviceId = token,
                oldDeviceId = oldDeviceId,
                userId = userId,
                userName = userName,
                language = getDeviceLanguage(),
                region = getDeviceRegion(),
                platform = "Android",
                transport = transport,
                osVersion = NotificareUtils.osVersion,
                sdkVersion = Notificare.SDK_VERSION,
                appVersion = NotificareUtils.applicationVersion,
                deviceString = NotificareUtils.deviceString,
                timeZoneOffset = NotificareUtils.timeZoneOffset,
                backgroundAppRefresh = true,

                // Submit a value when registering a temporary to prevent
                // otherwise let the push module take over and update the setting accordingly.
                allowedUI = if (transport == NotificareTransport.NOTIFICARE) false else null
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

        if (Notificare.isReady) {
            val device = checkNotNull(currentDevice)
            notifyDeviceRegistered(device)
        }
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

        if (device.language != getDeviceLanguage()) {
            NotificareLogger.debug("Registration check: language changed")
            changed = true
        }

        if (device.region != getDeviceRegion()) {
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

    internal fun getDeviceLanguage(): String {
        return Notificare.sharedPreferences.preferredLanguage ?: NotificareUtils.deviceLanguage
    }

    internal fun getDeviceRegion(): String {
        return Notificare.sharedPreferences.preferredRegion ?: NotificareUtils.deviceRegion
    }

    internal fun registerTestDevice(nonce: String, callback: NotificareCallback<Unit>) {
        Notificare.coroutineScope.launch {
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

    internal suspend fun updateLanguage(language: String, region: String) {
        checkPrerequisites()

        val device = checkNotNull(currentDevice)

        NotificareRequest.Builder()
            .put(
                url = "/device/${device.id}",
                body = DeviceUpdateLanguagePayload(
                    language = language,
                    region = region,
                ),
            )
            .response()

        // Update current device properties.
        currentDevice = device.copy(
            language = language,
            region = region,
        )
    }

    internal suspend fun updateTimeZone() {
        checkPrerequisites()

        val device = checkNotNull(currentDevice)

        NotificareRequest.Builder()
            .put(
                url = "/device/${device.id}",
                body = DeviceUpdateTimeZonePayload(
                    language = getDeviceLanguage(),
                    region = getDeviceRegion(),
                    timeZoneOffset = NotificareUtils.timeZoneOffset,
                ),
            )
            .response()
    }

    private fun notifyDeviceRegistered(device: NotificareDevice) {
        Notificare.requireContext().sendBroadcast(
            Intent(Notificare.requireContext(), Notificare.intentReceiver)
                .setAction(Notificare.INTENT_ACTION_DEVICE_REGISTERED)
                .putExtra(Notificare.INTENT_EXTRA_DEVICE, device)
        )
    }
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
    )
}
