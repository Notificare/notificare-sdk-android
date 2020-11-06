package re.notifica.modules

import re.notifica.Notificare
import re.notifica.NotificareDefinitions
import re.notifica.NotificareException
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

class NotificareDeviceManager : NotificareModule<Unit>() {

    var currentDevice: NotificareDevice?
        get() = Notificare.sharedPreferences.device
        private set(value) {
            Notificare.sharedPreferences.device = value
        }

    override fun configure() {}

    override suspend fun launch() {
        // TODO check registration status and act accordingly
        val device = currentDevice

        if (device != null) {
            if (device.appVersion != NotificareUtils.applicationVersion) {
                // It's not the same version, let's log it as an upgrade.
                Notificare.logger.debug("New version detected")

                // Log an application upgrade event.
                Notificare.eventsManager.logApplicationUpgrade()
            }
        } else {
            Notificare.logger.debug("New install detected")

            // Let's avoid the new registration event for a temporary device
            //NotificareUserDefaults.newRegistration = false

            // Let's logout the user in case there's an account in the keychain
            // TODO: [[NotificareAuth shared] logoutAccount]

            try {
                registerTemporary()

                // We will log the Install here since this will execute only one time at the start.
                Notificare.eventsManager.logApplicationInstall()

                // We will log the App Open this first time here.
                Notificare.eventsManager.logApplicationOpen()
            } catch (e: Exception) {
                Notificare.logger.warning("Failed to register temporary device.", e)
                throw e
            }
        }
    }

    suspend fun register(userId: String?, userName: String?) {
        TODO("Missing implementation")
    }

    val preferredLanguage: String?
        get() {
            val preferredLanguage = Notificare.sharedPreferences.preferredLanguage ?: return null
            val preferredRegion = Notificare.sharedPreferences.preferredRegion ?: return null

            return "$preferredLanguage-$preferredRegion"
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
        val response = Notificare.pushService.getDeviceTags(device.deviceId)
        return response.tags
    }

    suspend fun addTag(tag: String) = addTags(listOf(tag))

    suspend fun addTags(tags: List<String>) {
        val device = checkNotificareReady()
        Notificare.pushService.addDeviceTags(device.deviceId, NotificareTagsPayload(tags))
    }

    suspend fun removeTag(tag: String) = removeTags(listOf(tag))

    suspend fun removeTags(tags: List<String>) {
        val device = checkNotificareReady()
        Notificare.pushService.removeDeviceTags(device.deviceId, NotificareTagsPayload(tags))
    }

    suspend fun clearTags() {
        val device = checkNotificareReady()
        Notificare.pushService.clearDeviceTags(device.deviceId)
    }

    suspend fun fetchDoNotDisturb(): NotificareDoNotDisturb? {
        val device = checkNotificareReady()
        val (dnd) = Notificare.pushService.getDeviceDoNotDisturb(device.deviceId)

        // Update current device properties.
        currentDevice?.dnd = dnd

        return dnd
    }

    suspend fun updateDoNotDisturb(dnd: NotificareDoNotDisturb) {
        val device = checkNotificareReady()
        Notificare.pushService.updateDeviceDoNotDisturb(device.deviceId, dnd)

        // Update current device properties.
        currentDevice?.dnd = dnd
    }

    suspend fun clearDoNotDisturb() {
        val device = checkNotificareReady()
        Notificare.pushService.clearDeviceDoNotDisturb(device.deviceId)

        // Update current device properties.
        currentDevice?.dnd = null
    }

    suspend fun fetchUserData(): NotificareUserData? {
        val device = checkNotificareReady()
        val (userData) = Notificare.pushService.getDeviceUserData(device.deviceId)

        // Update current device properties.
        currentDevice?.userData = userData

        return userData
    }

    suspend fun updateUserData(userData: NotificareUserData) {
        val device = checkNotificareReady()
        Notificare.pushService.updateDeviceUserData(device.deviceId, userData)

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

    private suspend fun register(
        token: String,
        temporary: Boolean,
        userId: String?,
        userName: String?
    ): NotificareDevice {
        if (registrationChanged(token, userId, userName)) {
            val oldDeviceId =
                if (currentDevice?.deviceId != null && currentDevice?.deviceId != token) currentDevice?.deviceId
                else null

            val deviceRegistration = NotificareDeviceRegistration(
                deviceId = token,
                oldDeviceId = oldDeviceId,
                userId = userId,
                userName = userName,
                language = getLanguage(),
                region = getRegion(),
                platform = "Android",
                transport = NotificareTransport.NOTIFICARE, // TODO change when the push module is in place
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

            // TODO check and log an 'applicationRegistration' event

            val device = deviceRegistration.toStoredDevice(currentDevice).also {
                currentDevice = it
            }

            // Send a device registered broadcast.
            NotificareIntentEmitter.onDeviceRegistered(device)

            return device
        } else {
            Notificare.logger.info("Skipping device registration, nothing changed.")
            // Notificare.shared.delegate?.notificare(Notificare.shared, didRegisterDevice: device!)
            return requireNotNull(currentDevice)
        }
    }

    private suspend fun registerTemporary(): NotificareDevice {
        val token = currentDevice?.deviceId
            ?: UUID.randomUUID()
                .toByteArray()
                .toHex()

        val device = register(
            token = token,
            temporary = true,
            userId = currentDevice?.userId,
            userName = currentDevice?.userName,
        )

        // TODO updateNotificationSettings(allowedUI: false)

        return device
    }

    private fun registrationChanged(token: String?, userId: String?, userName: String?): Boolean {
        val device = currentDevice ?: run {
            Notificare.logger.debug("Registration check: fresh installation")
            return true
        }

        var changed = false

        if (device.userId != userId) {
            Notificare.logger.debug("Registration check: user id changed")
            changed = true
        }

        if (device.userName != userName) {
            Notificare.logger.debug("Registration check: user name changed")
            changed = true
        }

        if (device.deviceId != token) {
            Notificare.logger.debug("Registration check: device token changed")
            changed = true
        }

        if (device.deviceString != NotificareUtils.deviceString) {
            Notificare.logger.debug("Registration check: device string changed")
            changed = true
        }

        if (device.appVersion != NotificareUtils.applicationVersion) {
            Notificare.logger.debug("Registration check: application version changed")
            changed = true
        }

        if (device.osVersion != NotificareUtils.osVersion) {
            Notificare.logger.debug("Registration check: os version changed")
            changed = true
        }

        if (device.sdkVersion != NotificareDefinitions.SDK_VERSION) {
            Notificare.logger.debug("Registration check: sdk version changed")
            changed = true
        }

        if (device.timeZoneOffset != NotificareUtils.timeZoneOffset) {
            Notificare.logger.debug("Registration check: timezone offset changed")
            changed = true
        }

        if (device.language != getLanguage()) {
            Notificare.logger.debug("Registration check: language changed")
            changed = true
        }

        if (device.region != getRegion()) {
            Notificare.logger.debug("Registration check: region changed")
            changed = true
        }

        val oneDayAgo = GregorianCalendar().apply { add(Calendar.DAY_OF_YEAR, -1) }.time
        if (device.lastRegistered.before(oneDayAgo)) {
            Notificare.logger.debug("Registration check: region changed")
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
            device.deviceId,
            NotificareDeviceUpdateLanguage(
                language = getLanguage(),
                region = getRegion()
            )
        )
    }

    internal suspend fun updateTimeZone() {
        val device = checkNotificareReady()

        Notificare.pushService.updateDevice(
            device.deviceId,
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
        deviceId = this.deviceId,
        userId = this.userId,
        userName = this.userName,
        timeZoneOffset = this.timeZoneOffset,
        osVersion = this.osVersion,
        sdkVersion = this.sdkVersion,
        appVersion = this.appVersion,
        deviceString = this.deviceString,
        country = previous?.country, // country code (NL)
        language = this.language,
        region = this.region,
        transport = this.transport,
        dnd = previous?.dnd,
        userData = previous?.userData,
        latitude = previous?.latitude,
        longitude = previous?.longitude,
        altitude = previous?.altitude,
        accuracy = previous?.accuracy,
        floor = previous?.floor,
        speed = previous?.speed,
        course = previous?.course,
        lastRegistered = Date(),
        locationServicesAuthStatus = this.locationServicesAuthStatus,
        allowedUI = this.allowedUI,
        bluetoothEnabled = this.bluetoothEnabled,
    )
}
