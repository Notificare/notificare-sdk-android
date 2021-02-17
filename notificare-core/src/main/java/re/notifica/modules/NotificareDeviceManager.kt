package re.notifica.modules

import re.notifica.Notificare
import re.notifica.NotificareDefinitions
import re.notifica.NotificareException
import re.notifica.NotificareLogger
import re.notifica.internal.NotificareIntentEmitter
import re.notifica.internal.NotificareUtils
import re.notifica.internal.common.toByteArray
import re.notifica.internal.common.toHex
import re.notifica.internal.network.push.payloads.NotificareDeviceRegistration
import re.notifica.internal.network.push.payloads.NotificareDeviceUpdateLanguage
import re.notifica.internal.network.push.payloads.NotificareDeviceUpdateTimeZone
import re.notifica.internal.network.push.payloads.NotificareTagsPayload
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

            registerTemporary()
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

    suspend fun register(userId: String?, userName: String?) {
        val currentDevice = checkNotificareReady()
        register(currentDevice.transport, currentDevice.id, userId, userName)
    }

    suspend fun registerTemporary() {
        val token = currentDevice?.id
            ?: UUID.randomUUID()
                .toByteArray()
                .toHex()

        register(
            transport = NotificareTransport.NOTIFICARE,
            token = token,
            userId = currentDevice?.userId,
            userName = currentDevice?.userName,
        )

        // TODO updateNotificationSettings(allowedUI: false)
    }

    suspend fun registerToken(transport: NotificareTransport, token: String) {
        register(
            transport = transport,
            token = token,
            userId = currentDevice?.userId,
            userName = currentDevice?.userName,
        )
    }

    suspend fun updatePreferredLanguage(preferredLanguage: String?) {
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

    suspend fun fetchTags(): List<String> {
        val device = checkNotificareReady()

        // TODO consider wrapping this in a try catch block to rewrite the exception into a NotificareException.Network.
        val response = Notificare.pushService.getDeviceTags(device.id)
        return response.tags
    }

    suspend fun addTag(tag: String) = addTags(listOf(tag))

    suspend fun addTags(tags: List<String>) {
        val device = checkNotificareReady()
        Notificare.pushService.addDeviceTags(device.id, NotificareTagsPayload(tags))
    }

    suspend fun removeTag(tag: String) = removeTags(listOf(tag))

    suspend fun removeTags(tags: List<String>) {
        val device = checkNotificareReady()
        Notificare.pushService.removeDeviceTags(device.id, NotificareTagsPayload(tags))
    }

    suspend fun clearTags() {
        val device = checkNotificareReady()
        Notificare.pushService.clearDeviceTags(device.id)
    }

    suspend fun fetchDoNotDisturb(): NotificareDoNotDisturb? {
        val device = checkNotificareReady()
        val (dnd) = Notificare.pushService.getDeviceDoNotDisturb(device.id)

        // Update current device properties.
        currentDevice?.dnd = dnd

        return dnd
    }

    suspend fun updateDoNotDisturb(dnd: NotificareDoNotDisturb) {
        val device = checkNotificareReady()
        Notificare.pushService.updateDeviceDoNotDisturb(device.id, dnd)

        // Update current device properties.
        currentDevice?.dnd = dnd
    }

    suspend fun clearDoNotDisturb() {
        val device = checkNotificareReady()
        Notificare.pushService.clearDeviceDoNotDisturb(device.id)

        // Update current device properties.
        currentDevice?.dnd = null
    }

    suspend fun fetchUserData(): NotificareUserData? {
        val device = checkNotificareReady()
        val (userData) = Notificare.pushService.getDeviceUserData(device.id)

        // Update current device properties.
        currentDevice?.userData = userData

        return userData
    }

    suspend fun updateUserData(userData: NotificareUserData) {
        val device = checkNotificareReady()
        Notificare.pushService.updateDeviceUserData(device.id, userData)

        // Update current device properties.
        currentDevice?.userData = userData
    }

    // region Private API

    private fun checkNotificareReady(): NotificareDevice {
        val device = currentDevice

        if (!Notificare.isReady || device == null) {
            throw NotificareException.NotReady
        }

        return device
    }

    private suspend fun register(transport: NotificareTransport, token: String, userId: String?, userName: String?) {
        var currentDevice = currentDevice

        if (currentDevice == null || registrationChanged(token, userId, userName) || transport != currentDevice.transport) {
            val oldDeviceId =
                if (currentDevice?.id != null && currentDevice.id != token) currentDevice.id
                else null

            val deviceRegistration = NotificareDeviceRegistration(
                deviceId = token,
                oldDeviceId = oldDeviceId,
                userId = userId,
                userName = userName,
                language = getLanguage(),
                region = getRegion(),
                platform = "Android",
                transport = transport,
                osVersion = NotificareUtils.osVersion,
                sdkVersion = NotificareDefinitions.SDK_VERSION,
                appVersion = NotificareUtils.applicationVersion,
                deviceString = NotificareUtils.deviceString,
                timeZoneOffset = NotificareUtils.timeZoneOffset,
                backgroundAppRefresh = true,
                allowedUI = false, // TODO me,
                locationServicesAuthStatus = "none", // TODO me
                bluetoothEnabled = false, // TODO me
            )

            Notificare.pushService.createDevice(deviceRegistration)

            currentDevice = deviceRegistration.toStoredDevice(currentDevice).also {
                this.currentDevice = it
            }
        } else {
            NotificareLogger.info("Skipping device registration, nothing changed.")
        }

        // Send a device registered broadcast.
        NotificareIntentEmitter.onDeviceRegistered(currentDevice)
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

        if (device.sdkVersion != NotificareDefinitions.SDK_VERSION) {
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

    internal suspend fun updateLanguage() {
        val device = checkNotificareReady()

        Notificare.pushService.updateDevice(
            device.id,
            NotificareDeviceUpdateLanguage(
                language = getLanguage(),
                region = getRegion()
            )
        )
    }

    internal suspend fun updateTimeZone() {
        val device = checkNotificareReady()

        Notificare.pushService.updateDevice(
            device.id,
            NotificareDeviceUpdateTimeZone(
                language = getLanguage(),
                region = getRegion(),
                timeZoneOffset = NotificareUtils.timeZoneOffset,
            )
        )
    }

    // endregion
}

private fun NotificareDeviceRegistration.toStoredDevice(previous: NotificareDevice?): NotificareDevice {
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
        userData = previous?.userData,
//        location = previous?.location,
        lastRegistered = Date(),
        allowedUI = this.allowedUI,
        bluetoothEnabled = this.bluetoothEnabled,
    )
}
