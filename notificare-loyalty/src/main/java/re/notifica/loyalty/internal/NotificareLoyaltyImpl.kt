package re.notifica.loyalty.internal

import android.app.Activity
import android.app.PendingIntent
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Bitmap
import android.location.Location
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.os.bundleOf
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.work.*
import kotlinx.coroutines.*
import re.notifica.*
import re.notifica.internal.NotificareLogger
import re.notifica.internal.NotificareModule
import re.notifica.internal.NotificareUtils
import re.notifica.internal.ktx.toCallbackFunction
import re.notifica.internal.modules.integrations.NotificareGeoIntegration
import re.notifica.internal.modules.integrations.NotificareLoyaltyIntegration
import re.notifica.internal.network.NetworkException
import re.notifica.internal.network.request.NotificareRequest
import re.notifica.ktx.device
import re.notifica.loyalty.*
import re.notifica.loyalty.R
import re.notifica.loyalty.internal.ktx.getUpdatedFields
import re.notifica.loyalty.internal.network.push.*
import re.notifica.loyalty.internal.storage.LocalStorage
import re.notifica.loyalty.internal.storage.database.LoyaltyDatabase
import re.notifica.loyalty.internal.storage.database.entities.PassEntity
import re.notifica.loyalty.internal.workers.PassRelevanceUpdateWorker
import re.notifica.loyalty.ktx.INTENT_ACTION_PASSBOOK_OPENED
import re.notifica.loyalty.ktx.INTENT_EXTRA_PASSBOOK
import re.notifica.loyalty.ktx.geoIntegration
import re.notifica.loyalty.ktx.loyalty
import re.notifica.loyalty.models.NotificarePass
import re.notifica.models.NotificareApplication
import re.notifica.models.NotificareNotification
import java.text.DateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

internal object NotificareLoyaltyImpl : NotificareModule(), NotificareLoyalty, NotificareLoyaltyIntegration,
    Notificare.OnReadyListener {

    private lateinit var database: LoyaltyDatabase
    private lateinit var localStorage: LocalStorage
    private val notificationSequence = AtomicInteger()
    private val passesBySerial = mutableMapOf<String, NotificarePass>()
    private val _observablePasses = MutableLiveData<List<NotificarePass>>(listOf())
    private var scheduledRelevanceDate: Date? = null
    private val relevantPasses = mutableSetOf<String>()
    private val relevantTexts = mutableMapOf<String, String>()

    private val livePassEntitiesObserver = Observer<List<PassEntity>> { entities ->
        if (entities == null) {
            passesBySerial.clear()
            _observablePasses.postValue(emptyList())
            return@Observer
        }

        passesBySerial.clear()
        passesBySerial.putAll(
            entities
                .map { it.toModel() }
                .associateBy { it.serial }
        )

        _observablePasses.postValue(passesBySerial.values.toList())
    }

    // region Notificare Module

    override fun configure() {
        val context = Notificare.requireContext()

        database = LoyaltyDatabase.create(context)
        localStorage = LocalStorage(context)

        Handler(Looper.getMainLooper()).post {
            database.passes().getObservablePasses().observeForever(livePassEntitiesObserver)
        }
    }

    override suspend fun launch(): Unit = withContext(Dispatchers.IO) {
        // Do not halt the launch flow.
        // Instead, side load the updated passes.

        // Ensure the passes have been loaded.
        if (passesBySerial.isEmpty()) {
            passesBySerial.putAll(
                database.passes().getPasses()
                    .map { it.toModel() }
                    .associateBy { it.serial }
            )
        }

        Notificare.addOnReadyListener(this@NotificareLoyaltyImpl)
    }

    override suspend fun unlaunch() {
        Notificare.removeOnReadyListener(this)
    }

    // endregion

    // region Notificare Loyalty

    override var passbookActivity: Class<out PassbookActivity> = PassbookActivity::class.java

    override val passes: List<NotificarePass>
        get() = passesBySerial.values.toList()

    override val observablePasses: LiveData<List<NotificarePass>>
        get() = _observablePasses

    override suspend fun fetchPassBySerial(serial: String): NotificarePass = withContext(Dispatchers.IO) {
        checkPrerequisites()

        val pass = NotificareRequest.Builder()
            .get("/pass/forserial/$serial")
            .responseDecodable(FetchPassResponse::class)
            .pass

        enhancePass(pass)
    }

    override fun fetchPassBySerial(serial: String, callback: NotificareCallback<NotificarePass>): Unit =
        toCallbackFunction(::fetchPassBySerial)(serial, callback)

    override suspend fun fetchPassByBarcode(barcode: String): NotificarePass = withContext(Dispatchers.IO) {
        checkPrerequisites()

        val pass = NotificareRequest.Builder()
            .get("/pass/forbarcode/$barcode")
            .responseDecodable(FetchPassResponse::class)
            .pass

        enhancePass(pass)
    }

    override fun fetchPassByBarcode(barcode: String, callback: NotificareCallback<NotificarePass>): Unit =
        toCallbackFunction(::fetchPassByBarcode)(barcode, callback)

    override fun isInWallet(pass: NotificarePass): Boolean {
        return passesBySerial[pass.serial] != null
    }

    override suspend fun addPass(pass: NotificarePass): Unit = withContext(Dispatchers.IO) {
        checkPrerequisites()

        if (pass.version != 1) {
            NotificareLogger.warning("Wallet functionality only supports v1 passes.")
            return@withContext
        }

        if (isInWallet(pass)) {
            updatePass(pass)
            return@withContext
        }

        val entity = PassEntity.from(pass)
        database.passes().insert(entity)

        NotificareLogger.debug("Registering the pass to the current device.")
        registerPass(pass)

        if (!pass.isExpired) {
            updatePassRelevance(pass)
        }
    }

    override fun addPass(pass: NotificarePass, callback: NotificareCallback<Unit>): Unit =
        toCallbackFunction(::addPass)(pass, callback)

    override suspend fun removePass(pass: NotificarePass): Unit = withContext(Dispatchers.IO) {
        checkPrerequisites()

        if (pass.version != 1) {
            NotificareLogger.warning("Wallet functionality only supports v1 passes.")
            return@withContext
        }

        // Remove from the database.
        val entity = PassEntity.from(pass)
        database.passes().remove(entity)

        // Stop receiving updates for this pass.
        unregisterPass(pass)

        endPassRelevance(pass)
    }

    override fun removePass(pass: NotificarePass, callback: NotificareCallback<Unit>): Unit =
        toCallbackFunction(::removePass)(pass, callback)

    override fun present(activity: Activity, pass: NotificarePass) {
        present(activity, pass, null)
    }

    // endregion

    // region Notificare Loyalty Integration

    override fun onPassbookSystemNotificationReceived() {
        NotificareLogger.debug("Received a system notification to update the wallet.")

        GlobalScope.launch {
            try {
                refreshPasses()
            } catch (e: Exception) {
                NotificareLogger.error("Failed to update the wallet.", e)
            }
        }
    }

    override fun onPassbookLocationRelevanceChanged() {
        updateRelevantPasses()
    }

    override fun handlePassPresentation(
        activity: Activity,
        notification: NotificareNotification,
        callback: NotificareCallback<Unit>,
    ) {
        val serial = extractPassSerial(notification) ?: run {
            NotificareLogger.warning("Unable to extract the pass' serial from the notification.")

            val error = IllegalArgumentException("Unable to extract the pass' serial from the notification.")
            callback.onFailure(error)

            return
        }

        fetchPassBySerial(serial, object : NotificareCallback<NotificarePass> {
            override fun onSuccess(result: NotificarePass) {
                present(activity, result, callback)
            }

            override fun onFailure(e: Exception) {
                NotificareLogger.error("Failed to fetch the pass with serial '$serial'.", e)
                callback.onFailure(e)
            }
        })
    }

    // endregion

    // region Notificare.OnReadyListener

    override fun onReady(application: NotificareApplication) {
        GlobalScope.launch {
            try {
                // Update relevant passes.
                updateRelevantPasses()

                // Refresh passes that have been updated in the meantime.
                refreshPasses()
            } catch (e: Exception) {
                NotificareLogger.error("Failed to update the wallet.", e)
            }
        }
    }

    // endregion

    @Throws
    private fun checkPrerequisites() {
        if (!Notificare.isReady) {
            NotificareLogger.warning("Notificare is not ready yet.")
            throw NotificareNotReadyException()
        }

        if (Notificare.device().currentDevice == null) {
            NotificareLogger.warning("Notificare device is not yet available.")
            throw NotificareDeviceUnavailableException()
        }

        val application = Notificare.application ?: run {
            NotificareLogger.warning("Notificare application is not yet available.")
            throw NotificareApplicationUnavailableException()
        }

        if (application.services[NotificareApplication.ServiceKeys.PASSBOOK] != true) {
            NotificareLogger.warning("Notificare passes functionality is not enabled.")
            throw NotificareServiceUnavailableException(service = NotificareApplication.ServiceKeys.PASSBOOK)
        }
    }

    private fun extractPassSerial(notification: NotificareNotification): String? {
        if (notification.type != NotificareNotification.TYPE_PASSBOOK) return null

        val content = notification.content
            .firstOrNull { it.type == NotificareNotification.Content.TYPE_PK_PASS }
            ?: return null

        val url = content.data as? String ?: return null

        val parts = url.split("/")
        if (parts.isEmpty()) return null

        return parts.last()
    }

    private suspend fun enhancePass(
        pass: FetchPassResponse.Pass,
    ): NotificarePass = withContext(Dispatchers.IO) {
        val passType = when (pass.version) {
            1 -> pass.passbook?.let { fetchPassType(it) }
            else -> null
        }

        val googlePaySaveLink = when (pass.version) {
            2 -> fetchGooglePaySaveLink(pass.serial)
            else -> null
        }

        NotificarePass(
            id = pass._id,
            type = passType,
            version = pass.version,
            passbook = pass.passbook,
            template = pass.template,
            serial = pass.serial,
            barcode = pass.barcode,
            redeem = pass.redeem,
            redeemHistory = pass.redeemHistory,
            limit = pass.limit,
            token = pass.token,
            data = pass.data ?: emptyMap(),
            date = pass.date,
            googlePaySaveLink = googlePaySaveLink,
        )
    }

    private suspend fun fetchPassType(passbook: String): NotificarePass.PassType = withContext(Dispatchers.IO) {
        NotificareRequest.Builder()
            .get("/passbook/$passbook")
            .responseDecodable(FetchPassbookTemplateResponse::class)
            .passbook
            .passStyle
    }

    private suspend fun fetchGooglePaySaveLink(serial: String): String? = withContext(Dispatchers.IO) {
        NotificareRequest.Builder()
            .get("/pass/savelinks/$serial")
            .responseDecodable(FetchSaveLinksResponse::class)
            .saveLinks
            ?.googlePay
    }

    private suspend fun refreshPasses(): Unit = withContext(Dispatchers.IO) {
        try {
            val updatedSerials = fetchUpdatedSerials()
            updatedSerials.forEach { serial ->
                try {
                    val pass = fetchPassBySerial(serial)
                    updatePass(pass)
                } catch (e: Exception) {
                    NotificareLogger.error("Failed to update pass '$serial'.")
                }
            }
        } catch (e: Exception) {
            NotificareLogger.error("Failed to refresh the wallet.", e)
        }
    }

    private suspend fun updatePass(pass: NotificarePass): Unit = withContext(Dispatchers.IO) {
        checkPrerequisites()

        val currentPass = passesBySerial[pass.serial] ?: run {
            addPass(pass)
            return@withContext
        }

        if (!currentPass.date.before(pass.date)) {
            NotificareLogger.debug("Not updating pass '${pass.serial}'.")
            return@withContext
        }

        val entity = PassEntity.from(pass)
        database.passes().update(entity)

        NotificareLogger.debug("Registering the pass to the current device.")
        registerPass(pass)
        updatePassRelevance(pass)

        if (!pass.isExpired) {
            getPassUpdateMessages(oldPass = currentPass, newPass = pass)
                .forEach { generateRelevanceUpdateNotification(pass, it) }
        }
    }

    private suspend fun registerPass(pass: NotificarePass): Unit = withContext(Dispatchers.IO) {
        checkPrerequisites()

        val device = checkNotNull(Notificare.device().currentDevice)
        val payload = RegisterPassPayload(
            transport = device.transport,
        )

        NotificareRequest.Builder()
            .post("/pass/registration/${device.id}/forserial/${pass.serial}", payload)
            .response()
    }

    private suspend fun unregisterPass(pass: NotificarePass): Unit = withContext(Dispatchers.IO) {
        checkPrerequisites()

        val device = checkNotNull(Notificare.device().currentDevice)

        NotificareRequest.Builder()
            .delete("/pass/registration/${device.id}/forserial/${pass.serial}", null)
            .response()
    }

    private suspend fun fetchUpdatedSerials(): List<String> = withContext(Dispatchers.IO) {
        checkPrerequisites()

        val device = checkNotNull(Notificare.device().currentDevice)

        try {
            NotificareRequest.Builder()
                .get("/pass/registration/${device.id}")
                .query("passesUpdatedSince", localStorage.passesLastUpdate)
                .validate(200..200)
                .responseDecodable(FetchUpdatedSerialsResponse::class)
                .also { localStorage.passesLastUpdate = it.lastUpdated }
                .serialNumbers
        } catch (e: Exception) {
            if (e is NetworkException.ValidationException) {
                when (e.response.code) {
                    204 -> {
                        NotificareLogger.debug("The current device has no passes to update.")
                        return@withContext emptyList()
                    }
                    404 -> {
                        NotificareLogger.debug("The current device has no passes in the wallet.")
                        return@withContext emptyList()
                    }
                }
            }

            throw e
        }
    }

    private fun present(activity: Activity, pass: NotificarePass, callback: NotificareCallback<Unit>?) {
        when (pass.version) {
            1 -> {
                activity.startActivity(
                    Intent(activity, passbookActivity)
                        .putExtra(Notificare.INTENT_EXTRA_PASSBOOK, pass)
                )

                callback?.onSuccess(Unit)
            }
            2 -> {
                val url = pass.googlePaySaveLink ?: run {
                    NotificareLogger.warning("Cannot present the pass without a Google Pay link.")

                    val error = IllegalArgumentException("Cannot present the pass without a Google Pay link.")
                    callback?.onFailure(error)

                    return
                }

                try {
                    val intent = Intent().setAction(Intent.ACTION_VIEW)
                        .setData(Uri.parse(url))
                        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

                    activity.startActivity(intent)
                    callback?.onSuccess(Unit)
                } catch (e: ActivityNotFoundException) {
                    NotificareLogger.error("Failed to present the pass.", e)
                    callback?.onFailure(e)
                }
            }
            else -> {
                NotificareLogger.error("Unsupported pass version: ${pass.version}")

                val error = IllegalArgumentException("Unsupported pass version: ${pass.version}")
                callback?.onFailure(error)
            }
        }
    }

    // region Pass relevance

    private fun updateRelevantPasses() {
        passesBySerial.values.forEach {
            if (it.isExpired) {
                endPassRelevance(it)
            } else {
                updatePassRelevance(it)
            }
        }
    }

    private fun updatePassRelevance(pass: NotificarePass) {
        NotificareLogger.debug("Updating relevancy for pass '${pass.serial}'.")

        if (pass.isExpired) {
            endPassRelevance(pass)
            return
        }

        if (pass.relevantDate != null) {
            if (!checkPassRelevanceDate(pass)) {
                endPassRelevance(pass)
                schedulePassRelevanceUpdate(pass)
                return
            }
        }

        val relevantBeacon = checkPassRelevanceBeacon(pass)
        if (relevantBeacon != null) {
            NotificareLogger.debug("Pass ${pass.serial} is relevant for beacon.")
            startPassRelevance(pass, getBeaconRelevanceText(relevantBeacon))
            schedulePassRelevanceUpdate(pass)
            return
        }

        val relevantLocation = checkPassRelevanceLocation(pass)
        if (relevantLocation != null) {
            NotificareLogger.debug("Pass ${pass.serial} is relevant for location.")
            startPassRelevance(pass, getLocationRelevanceText(relevantLocation))
            schedulePassRelevanceUpdate(pass)
            return
        }

        if (checkPassRelevanceDate(pass)) {
            NotificareLogger.debug("Pass ${pass.serial} is relevant for date.")
            startPassRelevance(pass, getDateRelevanceText(pass))
            schedulePassRelevanceUpdate(pass)
            return
        }

        NotificareLogger.debug("Pass ${pass.serial} is not relevant for date nor location.")
        endPassRelevance(pass)
        schedulePassRelevanceUpdate(pass)
    }

    private fun startPassRelevance(pass: NotificarePass, relevanceText: String) {
        if (!relevantPasses.contains(pass.serial) || relevanceText != relevantTexts[pass.serial]) {
            relevantPasses.add(pass.serial)
            relevantTexts[pass.serial] = relevanceText

            NotificareLogger.debug("Starting relevance for pass '${pass.serial}'.")
            generateRelevanceNotification(pass, relevanceText)
        }
    }

    private fun endPassRelevance(pass: NotificarePass) {
        relevantPasses.remove(pass.serial)
        relevantTexts.remove(pass.serial)

        NotificareLogger.debug("Ending relevance for pass '${pass.serial}'.")
        Notificare.cancelNotification(getRelevanceNotificationTag(pass))
    }

    private fun checkPassRelevanceDate(pass: NotificarePass): Boolean {
        val relevantDate = pass.relevantDate ?: return false

        val calendar = Calendar.getInstance()
        var hours = checkNotNull(Notificare.options).passRelevanceHours
        if (pass.locations.isNotEmpty() || pass.beacons.isNotEmpty()) {
            // Double hours to take travel into account
            hours *= 2
        }

        val relevanceStart = calendar.let {
            it.add(Calendar.HOUR, -hours)
            it.time
        }

        val relevanceEnd = calendar.let {
            it.add(Calendar.HOUR, 2 * hours)
            it.time
        }

        return relevantDate.after(relevanceStart) && relevantDate.before(relevanceEnd)
    }

    private fun checkPassRelevanceBeacon(pass: NotificarePass): NotificarePass.PassbookBeacon? {
        if (Notificare.application?.regionConfig?.proximityUUID == null) {
            NotificareLogger.debug("Cannot check location relevance for pass ${pass.serial} because the application has no region config.")
            return null
        }

        if (pass.isExpired) {
            NotificareLogger.debug("Skipping location relevance for pass ${pass.serial} because the pass has expired.")
            return null
        }

        val enteredBeacons = Notificare.geoIntegration()?.geoEnteredBeacons ?: run {
            NotificareLogger.debug("Skipping location relevance for pass ${pass.serial} because the geo module is not available.")
            return null
        }

        return pass.beacons.firstOrNull { beacon ->
            enteredBeacons.any { nearbyBeacon ->
                checkPassRelevanceBeacon(beacon, nearbyBeacon)
            }
        }
    }

    /**
     * Checks if the given passbook beacon is relevant taking into consideration the given nearby beacon.
     * Relevance criteria:
     * - Passbook beacon contains no major.
     * - Passbook beacon's major matches nearby beacon's major and contains no minor.
     * - Passbook beacon's major matches nearby beacon's both major and minor.
     */
    private fun checkPassRelevanceBeacon(
        passbookBeacon: NotificarePass.PassbookBeacon,
        nearbyBeacon: NotificareGeoIntegration.Beacon,
    ): Boolean {
        val proximityUUID = Notificare.application?.regionConfig?.proximityUUID ?: return false

        if (passbookBeacon.proximityUUID != proximityUUID) return false
        if (passbookBeacon.major == null) return true

        return passbookBeacon.major == nearbyBeacon.major && (passbookBeacon.minor == null || passbookBeacon.minor == nearbyBeacon.minor)
    }

    private fun checkPassRelevanceLocation(pass: NotificarePass): NotificarePass.PassbookLocation? {
        if (pass.isExpired) {
            NotificareLogger.debug("Skipping location relevance for pass ${pass.serial} because the pass has expired.")
            return null
        }

        val lastKnownLocation = Notificare.geoIntegration()?.geoLastKnownLocation ?: run {
            NotificareLogger.debug("Skipping location relevance for pass ${pass.serial} because there is no last known location.")
            return null
        }

        var maxDistance: Double = when (pass.type) {
            NotificarePass.PassType.BOARDING,
            NotificarePass.PassType.TICKET -> checkNotNull(Notificare.options).passRelevanceLargeRadius
            else -> checkNotNull(Notificare.options).passRelevanceSmallRadius
        }

        // Compare the passbook max distance to the default distance and use the smallest value.
        pass.maxDistance?.run {
            if (this < maxDistance) {
                maxDistance = this
            }
        }

        return pass.locations.firstOrNull { location ->
            val l = Location("").apply {
                latitude = location.latitude
                longitude = location.longitude
            }

            l.distanceTo(lastKnownLocation) <= maxDistance
        }
    }

    private fun getDateRelevanceText(pass: NotificarePass): String {
        val dateStr: String = pass.relevantDate?.let {
            DateFormat.getTimeInstance(DateFormat.SHORT).format(it)
        } ?: ""

        return Notificare.requireContext().getString(R.string.notificare_passbook_absolute_date_relevance_text, dateStr)
    }

    private fun getBeaconRelevanceText(beacon: NotificarePass.PassbookBeacon): String {
        return if (!beacon.relevantText.isNullOrBlank()) beacon.relevantText
        else checkNotNull(Notificare.options).passRelevanceText
    }

    private fun getLocationRelevanceText(location: NotificarePass.PassbookLocation): String {
        return if (!location.relevantText.isNullOrBlank()) location.relevantText
        else checkNotNull(Notificare.options).passRelevanceText
    }

    private fun schedulePassRelevanceUpdate(pass: NotificarePass) {
        NotificareLogger.debug("Scheduling relevance pass update for pass '${pass.serial}'.")
        if (pass.isExpired) return

        val now = Date()

        val relevantDate = pass.relevantDate
        if (relevantDate != null) {
            val relevantCalendar = Calendar.getInstance().apply {
                time = relevantDate
            }

            var hours = checkNotNull(Notificare.options).passRelevanceHours
            if (pass.locations.isNotEmpty() || pass.beacons.isNotEmpty()) hours *= 2

            relevantCalendar.add(Calendar.HOUR, -hours)
            if (now.before(relevantCalendar.time)) {
                schedulePassRelevanceUpdate(relevantCalendar.time)
            } else {
                relevantCalendar.add(Calendar.HOUR, hours * 2)
                if (now.before(relevantCalendar.time)) {
                    schedulePassRelevanceUpdate(relevantCalendar.time)
                }
            }
        }

        val expirationDate = pass.expirationDate
        if (expirationDate != null && expirationDate.after(now)) {
            schedulePassRelevanceUpdate(expirationDate)
        }
    }

    private fun schedulePassRelevanceUpdate(date: Date) {
        if (scheduledRelevanceDate != null && date.after(scheduledRelevanceDate)) return

        scheduledRelevanceDate = date

        val start: Long
        val now = Date()

        if (now.after(date)) {
            start = 0L
            NotificareLogger.debug("Scheduling the next relevance update as soon as possible.")
        } else {
            start = (date.time - now.time) / 1000
            NotificareLogger.debug("Scheduling the next relevance update in $start seconds")
        }

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val task = OneTimeWorkRequest.Builder(PassRelevanceUpdateWorker::class.java)
            .setConstraints(constraints)
            .setInitialDelay(start, TimeUnit.SECONDS)
            .build()

        WorkManager.getInstance(Notificare.requireContext())
            .enqueueUniqueWork("re.notifica.loyalty.tasks.PassRelevanceUpdate", ExistingWorkPolicy.REPLACE, task)
    }

    internal fun handleScheduledPassRelevanceUpdate() {
        scheduledRelevanceDate = null
        updateRelevantPasses()
    }

    internal fun handleRelevanceNotificationRemoved(pass: NotificarePass) {
        relevantPasses.remove(pass.serial)
        relevantTexts.remove(pass.serial)
    }

    private fun createUniqueNotificationId(): Int {
        return notificationSequence.incrementAndGet()
    }

    private fun generateRelevanceNotification(pass: NotificarePass, relevanceText: String) {
        val now = System.currentTimeMillis()

        // Create an intent for passbook open
        val notificationIntent = Intent()
            .setAction(Notificare.INTENT_ACTION_PASSBOOK_OPENED)
            .setClass(Notificare.requireContext(), Notificare.loyalty().passbookActivity)
            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            .putExtra(Notificare.INTENT_EXTRA_PASSBOOK, pass)

        // Wrap the intent into a PendingIntent
        val pendingIntent = PendingIntent.getActivity(
            Notificare.requireContext(),
            createUniqueNotificationId(),
            notificationIntent,
            PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Create an intent for notification delete
        val deleteIntent = Intent(Notificare.requireContext(), NotificareLoyaltyIntentReceiver::class.java)
            .setAction(NotificareLoyaltyIntentReceiver.INTENT_ACTION_RELEVANCE_NOTIFICATION_DELETED)
            .putExtras(notificationIntent.extras ?: bundleOf())

        val deletePendingIntent = PendingIntent.getBroadcast(
            Notificare.requireContext(),
            createUniqueNotificationId(),
            deleteIntent,
            PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val options = checkNotNull(Notificare.options)
        val notificationManager = NotificationManagerCompat.from(Notificare.requireContext())

        val builder = NotificationCompat.Builder(Notificare.requireContext(), options.passNotificationChannel).apply {
            setSmallIcon(options.passNotificationSmallIcon)
            setContentTitle(NotificareUtils.applicationName)
            setContentText(relevanceText)
            setWhen(now)
            setContentIntent(pendingIntent)
            setDeleteIntent(deletePendingIntent)
            setTicker(relevanceText)
            setOngoing(options.passNotificationOngoing)

            setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(relevanceText)
            )

            options.passNotificationAccentColor?.let { color -> setColor(color) }

            // Extend for Android Wear
            val wearableExtender = NotificationCompat.WearableExtender()
            extend(wearableExtender)
        }

        GlobalScope.launch {
            val icon: Bitmap? = pass.icon?.let {
                try {
                    NotificareUtils.loadBitmap(it)
                } catch (e: Exception) {
                    NotificareLogger.debug("Failed to load passbook icon.", e)
                    null
                }
            }

            builder.setLargeIcon(icon)
            notificationManager.notify(getRelevanceNotificationTag(pass), 0, builder.build())
        }
    }

    private fun generateRelevanceUpdateNotification(pass: NotificarePass, updateText: String) {
        val now = System.currentTimeMillis()

        // Create an intent for passbook open
        val notificationIntent: Intent = Intent()
            .setAction(Notificare.INTENT_ACTION_PASSBOOK_OPENED)
            .setClass(Notificare.requireContext(), Notificare.loyalty().passbookActivity)
            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            .putExtra(Notificare.INTENT_EXTRA_PASSBOOK, pass)

        // Wrap the intent into a PendingIntent
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            Notificare.requireContext(),
            createUniqueNotificationId(),
            notificationIntent,
            PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Create an intent for notification delete
//        val deleteIntent = Intent(Notificare.shared().getApplicationContext(), Notificare.shared().getIntentReceiver())
//        deleteIntent.action = Notificare.INTENT_ACTION_RELEVANCE_NOTIFICATION_DELETED
//        if (notificationIntent.extras != null) {
//            deleteIntent.putExtras(notificationIntent.extras!!)
//        }
//        // Wrap the intent into a PendingIntent
//        val deleteBroadcast: PendingIntent
//        deleteBroadcast = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
//            PendingIntent.getBroadcast(
//                Notificare.shared().getApplicationContext(),
//                Notificare.shared().getNotificationSequence(),
//                deleteIntent,
//                PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
//            )
//        } else {
//            PendingIntent.getBroadcast(
//                Notificare.shared().getApplicationContext(),
//                Notificare.shared().getNotificationSequence(),
//                deleteIntent,
//                PendingIntent.FLAG_CANCEL_CURRENT
//            )
//        }

        val options = checkNotNull(Notificare.options)
        val notificationManager = NotificationManagerCompat.from(Notificare.requireContext())

        val builder = NotificationCompat.Builder(Notificare.requireContext(), options.passNotificationChannel).apply {
            setAutoCancel(true)
            setSmallIcon(options.passNotificationSmallIcon)
            setContentTitle(pass.description)
            setContentText(updateText)
            setWhen(now)
            setContentIntent(pendingIntent)
            // setDeleteIntent()
            setTicker(updateText)

            setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(updateText)
            )

            options.passNotificationAccentColor?.let { color -> setColor(color) }

            // Extend for Android Wear
            val wearableExtender = NotificationCompat.WearableExtender()
            extend(wearableExtender)
        }

        GlobalScope.launch {
            val icon: Bitmap? = pass.icon?.let {
                try {
                    NotificareUtils.loadBitmap(it)
                } catch (e: Exception) {
                    NotificareLogger.debug("Failed to load passbook icon.", e)
                    null
                }
            }

            builder.setLargeIcon(icon)
            notificationManager.notify(getRelevanceNotificationTag(pass), 0, builder.build())
        }
    }

    private fun getRelevanceNotificationTag(pass: NotificarePass): String {
        return "Relevance:${pass.serial}"
    }

    private fun getPassUpdateMessages(oldPass: NotificarePass, newPass: NotificarePass): List<String> {
        return newPass.getUpdatedFields(oldPass)
            .mapNotNull { it.parsedChangeMessage }
    }

    // endregion
}
