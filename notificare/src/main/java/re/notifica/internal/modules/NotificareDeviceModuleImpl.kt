package re.notifica.internal.modules

import android.content.Intent
import kotlinx.coroutines.*
import re.notifica.*
import re.notifica.internal.NotificareLogger
import re.notifica.internal.NotificareModule
import re.notifica.internal.NotificareUtils
import re.notifica.internal.common.filterNotNull
import re.notifica.internal.common.toByteArray
import re.notifica.internal.common.toHex
import re.notifica.internal.ktx.toCallbackFunction
import re.notifica.internal.network.push.*
import re.notifica.internal.network.request.NotificareRequest
import re.notifica.ktx.events
import re.notifica.models.NotificareDevice
import re.notifica.models.NotificareDoNotDisturb
import re.notifica.models.NotificareTransport
import re.notifica.models.NotificareUserData
import java.util.*

internal object NotificareDeviceModuleImpl : NotificareModule(), NotificareDeviceModule,
    NotificareInternalDeviceModule {

    // region Notificare Module

    override suspend fun launch() {
        val device = currentDevice

        if (device != null) {
            if (device.appVersion != NotificareUtils.applicationVersion) {
                // It's not the same version, let's log it as an upgrade.
                NotificareLogger.debug("New version detected")
                Notificare.events().logApplicationUpgrade()
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
                Notificare.events().logApplicationInstall()
                Notificare.events().logApplicationRegistration()
            } catch (e: Exception) {
                NotificareLogger.warning("Failed to register temporary device.", e)
                throw e
            }
        }
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

    override suspend fun register(userId: String?, userName: String?): Unit = withContext(Dispatchers.IO) {
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

            Notificare.sharedPreferences.preferredLanguage = parts[0]
            Notificare.sharedPreferences.preferredRegion = parts[1]

            updateLanguage()
        } else {
            Notificare.sharedPreferences.preferredLanguage = null
            Notificare.sharedPreferences.preferredRegion = null

            updateLanguage()
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

    override fun fetchTags(callback: NotificareCallback<List<String>>): Unit =
        toCallbackFunction(::fetchTags)(callback)

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

    override fun clearTags(callback: NotificareCallback<Unit>): Unit =
        toCallbackFunction(::clearTags)(callback)

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

    override suspend fun registerPushToken(
        transport: NotificareTransport,
        token: String
    ): Unit = withContext(Dispatchers.IO) {
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
                backgroundAppRefresh = true,
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
        @OptIn(DelicateCoroutinesApi::class)
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
        checkPrerequisites()

        val device = checkNotNull(currentDevice)

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
        checkPrerequisites()

        val device = checkNotNull(currentDevice)

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
