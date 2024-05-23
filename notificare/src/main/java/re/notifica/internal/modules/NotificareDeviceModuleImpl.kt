package re.notifica.internal.modules

import android.content.Intent
import androidx.annotation.Keep
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import re.notifica.Notificare
import re.notifica.NotificareCallback
import re.notifica.NotificareDeviceModule
import re.notifica.NotificareDeviceUnavailableException
import re.notifica.NotificareNotReadyException
import re.notifica.internal.NOTIFICARE_VERSION
import re.notifica.internal.NotificareLogger
import re.notifica.internal.NotificareModule
import re.notifica.internal.NotificareUtils
import re.notifica.internal.common.filterNotNull
import re.notifica.internal.ktx.coroutineScope
import re.notifica.internal.ktx.toCallbackFunction
import re.notifica.internal.network.push.CreateDevicePayload
import re.notifica.internal.network.push.CreateDeviceResponse
import re.notifica.internal.network.push.DeviceDoNotDisturbResponse
import re.notifica.internal.network.push.DeviceTagsPayload
import re.notifica.internal.network.push.DeviceTagsResponse
import re.notifica.internal.network.push.DeviceUpdateLanguagePayload
import re.notifica.internal.network.push.DeviceUpdateTimeZonePayload
import re.notifica.internal.network.push.DeviceUserDataResponse
import re.notifica.internal.network.push.TestDeviceRegistrationPayload
import re.notifica.internal.network.push.UpdateDevicePayload
import re.notifica.internal.network.push.UpgradeToLongLivedDevicePayload
import re.notifica.internal.network.request.NotificareRequest
import re.notifica.internal.storage.preferences.entities.StoredDevice
import re.notifica.internal.storage.preferences.ktx.asPublic
import re.notifica.ktx.eventsImplementation
import re.notifica.ktx.session
import re.notifica.models.NotificareDevice
import re.notifica.models.NotificareDoNotDisturb
import re.notifica.models.NotificareUserData
import java.util.Locale

@Keep
internal object NotificareDeviceModuleImpl : NotificareModule(), NotificareDeviceModule {

    private var storedDevice: StoredDevice?
        get() = Notificare.sharedPreferences.device
        set(value) {
            Notificare.sharedPreferences.device = value
        }

    private var hasPendingDeviceRegistrationEvent: Boolean? = null

    // region Notificare Module

    override suspend fun launch() {
        upgradeToLongLivedDeviceWhenNeeded()

        val storedDevice = storedDevice

        if (storedDevice == null) {
            NotificareLogger.debug("New install detected")

            createDevice()
            hasPendingDeviceRegistrationEvent = true

            // Ensure a session exists for the current device.
            Notificare.session().launch()

            // We will log the Install & Registration events here since this will execute only one time at the start.
            Notificare.eventsImplementation().logApplicationInstall()
            Notificare.eventsImplementation().logApplicationRegistration()
        } else {
            val isApplicationUpgrade = storedDevice.appVersion != NotificareUtils.applicationVersion

            updateDevice(
                userId = storedDevice.userId,
                userName = storedDevice.userName,
            )

            // Ensure a session exists for the current device.
            Notificare.session().launch()

            if (isApplicationUpgrade) {
                // It's not the same version, let's log it as an upgrade.
                NotificareLogger.debug("New version detected")
                Notificare.eventsImplementation().logApplicationUpgrade()
            }
        }
    }

    override suspend fun postLaunch() {
        val device = storedDevice
        if (device != null && hasPendingDeviceRegistrationEvent == true) {
            notifyDeviceRegistered(device.asPublic())
        }
    }

    // endregion

    // region Notificare Device Module

    override val currentDevice: NotificareDevice?
        get() = Notificare.sharedPreferences.device?.asPublic()

    override val preferredLanguage: String?
        get() {
            val preferredLanguage = Notificare.sharedPreferences.preferredLanguage ?: return null
            val preferredRegion = Notificare.sharedPreferences.preferredRegion ?: return null

            return "$preferredLanguage-$preferredRegion"
        }

    @Deprecated(
        message = "Use updateUser() instead.",
        replaceWith = ReplaceWith("updateUser(userId, userName)"),
    )
    override suspend fun register(userId: String?, userName: String?): Unit = withContext(Dispatchers.IO) {
        updateUser(
            userId = userId,
            userName = userName,
        )
    }

    @Deprecated(
        message = "Use updateUser() instead.",
        replaceWith = ReplaceWith("updateUser(userId, userName, callback)"),
    )
    @Suppress("DEPRECATION")
    override fun register(userId: String?, userName: String?, callback: NotificareCallback<Unit>): Unit =
        toCallbackFunction(::register)(userId, userName, callback)

    override suspend fun updateUser(userId: String?, userName: String?): Unit = withContext(Dispatchers.IO) {
        checkPrerequisites()
        if (storedDevice == null) throw NotificareDeviceUnavailableException()

        updateDevice(userId, userName)
    }

    override fun updateUser(userId: String?, userName: String?, callback: NotificareCallback<Unit>): Unit =
        toCallbackFunction(::updateUser)(userId, userName, callback)

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

        val device = checkNotNull(storedDevice)

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

        val device = checkNotNull(storedDevice)
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

        val device = checkNotNull(storedDevice)
        NotificareRequest.Builder()
            .put("/device/${device.id}/removetags", DeviceTagsPayload(tags))
            .response()
    }

    override fun removeTags(tags: List<String>, callback: NotificareCallback<Unit>): Unit =
        toCallbackFunction(::removeTags)(tags, callback)

    override suspend fun clearTags(): Unit = withContext(Dispatchers.IO) {
        checkPrerequisites()

        val device = checkNotNull(storedDevice)
        NotificareRequest.Builder()
            .put("/device/${device.id}/cleartags", null)
            .response()
    }

    override fun clearTags(callback: NotificareCallback<Unit>): Unit =
        toCallbackFunction(::clearTags)(callback)

    override suspend fun fetchDoNotDisturb(): NotificareDoNotDisturb? = withContext(Dispatchers.IO) {
        checkPrerequisites()

        val device = checkNotNull(storedDevice)
        val dnd = NotificareRequest.Builder()
            .get("/device/${device.id}/dnd")
            .responseDecodable(DeviceDoNotDisturbResponse::class)
            .dnd

        // Update current device properties.
        storedDevice = device.copy(dnd = dnd)

        return@withContext dnd
    }

    override fun fetchDoNotDisturb(callback: NotificareCallback<NotificareDoNotDisturb?>): Unit =
        toCallbackFunction(::fetchDoNotDisturb)(callback)

    override suspend fun updateDoNotDisturb(dnd: NotificareDoNotDisturb): Unit = withContext(Dispatchers.IO) {
        checkPrerequisites()

        val device = checkNotNull(storedDevice)
        NotificareRequest.Builder()
            .put("/device/${device.id}/dnd", dnd)
            .response()

        // Update current device properties.
        storedDevice = device.copy(dnd = dnd)
    }

    override fun updateDoNotDisturb(dnd: NotificareDoNotDisturb, callback: NotificareCallback<Unit>): Unit =
        toCallbackFunction(::updateDoNotDisturb)(dnd, callback)

    override suspend fun clearDoNotDisturb(): Unit = withContext(Dispatchers.IO) {
        checkPrerequisites()

        val device = checkNotNull(storedDevice)
        NotificareRequest.Builder()
            .put("/device/${device.id}/cleardnd", null)
            .response()

        // Update current device properties.
        storedDevice = device.copy(dnd = null)
    }

    override fun clearDoNotDisturb(callback: NotificareCallback<Unit>): Unit =
        toCallbackFunction(::clearDoNotDisturb)(callback)

    override suspend fun fetchUserData(): NotificareUserData = withContext(Dispatchers.IO) {
        checkPrerequisites()

        val device = checkNotNull(storedDevice)
        val userData = NotificareRequest.Builder()
            .get("/device/${device.id}/userdata")
            .responseDecodable(DeviceUserDataResponse::class)
            .userData?.filterNotNull { it.value }
            ?: mapOf()

        // Update current device properties.
        storedDevice = device.copy(userData = userData)

        return@withContext userData
    }

    override fun fetchUserData(callback: NotificareCallback<NotificareUserData>): Unit =
        toCallbackFunction(::fetchUserData)(callback)

    override suspend fun updateUserData(userData: NotificareUserData): Unit = withContext(Dispatchers.IO) {
        checkPrerequisites()

        val device = checkNotNull(storedDevice)
        NotificareRequest.Builder()
            .put("/device/${device.id}/userdata", userData)
            .response()

        // Update current device properties.
        storedDevice = device.copy(userData = userData)
    }

    override fun updateUserData(userData: NotificareUserData, callback: NotificareCallback<Unit>): Unit =
        toCallbackFunction(::updateUserData)(userData, callback)

    // endregion

    internal suspend fun delete(): Unit = withContext(Dispatchers.IO) {
        checkPrerequisites()

        val device = checkNotNull(storedDevice)

        NotificareRequest.Builder()
            .delete(
                url = "/push/${device.id}",
                body = null,
            )
            .response()

        // Remove current device.
        storedDevice = null
    }

    @Throws
    private fun checkPrerequisites() {
        if (!Notificare.isReady) {
            NotificareLogger.warning("Notificare is not ready yet.")
            throw NotificareNotReadyException()
        }
    }

    private suspend fun createDevice(): Unit = withContext(Dispatchers.IO) {
        val payload = CreateDevicePayload(
            language = getDeviceLanguage(),
            region = getDeviceRegion(),
            platform = "Android",
            osVersion = NotificareUtils.osVersion,
            sdkVersion = NOTIFICARE_VERSION,
            appVersion = NotificareUtils.applicationVersion,
            deviceString = NotificareUtils.deviceString,
            timeZoneOffset = NotificareUtils.timeZoneOffset,
            backgroundAppRefresh = true,
        )

        val response = NotificareRequest.Builder()
            .post("/push", payload)
            .responseDecodable(CreateDeviceResponse::class)

        storedDevice = StoredDevice(
            id = response.device.deviceId,
            userId = null,
            userName = null,
            timeZoneOffset = payload.timeZoneOffset,
            osVersion = payload.osVersion,
            sdkVersion = payload.sdkVersion,
            appVersion = payload.appVersion,
            deviceString = payload.deviceString,
            language = payload.language,
            region = payload.region,
            dnd = null,
            userData = mapOf(),
        )
    }

    private suspend fun updateDevice(userId: String?, userName: String?): Unit = withContext(Dispatchers.IO) {
        val storedDevice = checkNotNull(storedDevice)

        if (!registrationChanged(userId, userName)) {
            NotificareLogger.debug("Skipping device update, nothing changed.")
            return@withContext
        }

        val payload = UpdateDevicePayload(
            userId = userId,
            userName = userName,
            language = getDeviceLanguage(),
            region = getDeviceRegion(),
            platform = "Android",
            osVersion = NotificareUtils.osVersion,
            sdkVersion = NOTIFICARE_VERSION,
            appVersion = NotificareUtils.applicationVersion,
            deviceString = NotificareUtils.deviceString,
            timeZoneOffset = NotificareUtils.timeZoneOffset,
        )

        NotificareRequest.Builder()
            .put("/push/${storedDevice.id}", payload)
            .response()

        this@NotificareDeviceModuleImpl.storedDevice = storedDevice.copy(
            userId = userId,
            userName = userName,
            timeZoneOffset = payload.timeZoneOffset,
            osVersion = payload.osVersion,
            sdkVersion = payload.sdkVersion,
            appVersion = payload.appVersion,
            deviceString = payload.deviceString,
            language = payload.language,
            region = payload.region,
            dnd = storedDevice.dnd,
            userData = storedDevice.userData,
        )
    }

    private suspend fun upgradeToLongLivedDeviceWhenNeeded(): Unit = withContext(Dispatchers.IO) {
        val currentDevice = Notificare.sharedPreferences.device
            ?: return@withContext

        if (currentDevice.isLongLived) return@withContext

        NotificareLogger.info("Upgrading current device from legacy format.")

        val deviceId = currentDevice.id
        val transport = checkNotNull(currentDevice.transport)
        val subscriptionId = if (transport != "Notificare") deviceId else null

        val payload = UpgradeToLongLivedDevicePayload(
            deviceId = deviceId,
            transport = transport,
            subscriptionId = subscriptionId,
            language = currentDevice.language,
            region = currentDevice.region,
            platform = "Android",
            osVersion = currentDevice.osVersion,
            sdkVersion = currentDevice.sdkVersion,
            appVersion = currentDevice.appVersion,
            deviceString = currentDevice.deviceString,
            timeZoneOffset = currentDevice.timeZoneOffset,
        )

        val response = NotificareRequest.Builder()
            .post("/push", payload)
            .responseDecodable { responseCode ->
                when (responseCode) {
                    201 -> CreateDeviceResponse::class
                    else -> null
                }
            }

        if (response != null) {
            NotificareLogger.debug("New device identifier created.")
        }

        storedDevice = StoredDevice(
            id = response?.device?.deviceId ?: currentDevice.id,
            userId = currentDevice.userId,
            userName = currentDevice.userName,
            timeZoneOffset = currentDevice.timeZoneOffset,
            osVersion = currentDevice.osVersion,
            sdkVersion = currentDevice.sdkVersion,
            appVersion = currentDevice.appVersion,
            deviceString = currentDevice.deviceString,
            language = currentDevice.language,
            region = currentDevice.region,
            dnd = currentDevice.dnd,
            userData = currentDevice.userData,
        )
    }

    private fun registrationChanged(userId: String?, userName: String?): Boolean {
        val device = storedDevice ?: run {
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
                                deviceId = checkNotNull(storedDevice).id,
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

        val device = checkNotNull(storedDevice)

        NotificareRequest.Builder()
            .put(
                url = "/push/${device.id}",
                body = DeviceUpdateLanguagePayload(
                    language = language,
                    region = region,
                ),
            )
            .response()

        // Update current device properties.
        storedDevice = device.copy(
            language = language,
            region = region,
        )
    }

    internal suspend fun updateTimeZone() {
        checkPrerequisites()

        val device = checkNotNull(storedDevice)

        NotificareRequest.Builder()
            .put(
                url = "/push/${device.id}",
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
