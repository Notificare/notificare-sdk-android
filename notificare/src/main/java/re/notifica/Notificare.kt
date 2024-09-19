package re.notifica

import android.app.Activity
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.net.Uri
import android.os.Build
import android.os.RemoteException
import androidx.annotation.MainThread
import androidx.core.app.NotificationManagerCompat
import com.android.installreferrer.api.InstallReferrerClient
import com.android.installreferrer.api.InstallReferrerStateListener
import com.android.installreferrer.api.ReferrerDetails
import java.lang.ref.WeakReference
import java.net.URLEncoder
import java.util.regex.Pattern
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import re.notifica.internal.NOTIFICARE_VERSION
import re.notifica.internal.NotificareLaunchState
import re.notifica.internal.NotificareLogger
import re.notifica.internal.NotificareModule
import re.notifica.internal.NotificareOptions
import re.notifica.internal.NotificareUtils
import re.notifica.utilities.onMainThread
import re.notifica.internal.network.push.ApplicationResponse
import re.notifica.internal.network.push.CreateNotificationReplyPayload
import re.notifica.internal.network.push.DynamicLinkResponse
import re.notifica.internal.network.push.NotificareUploadResponse
import re.notifica.internal.network.push.NotificationResponse
import re.notifica.internal.network.request.NotificareRequest
import re.notifica.internal.storage.SharedPreferencesMigration
import re.notifica.internal.storage.database.NotificareDatabase
import re.notifica.internal.storage.preferences.NotificareSharedPreferences
import re.notifica.ktx.device
import re.notifica.ktx.deviceImplementation
import re.notifica.models.NotificareApplication
import re.notifica.models.NotificareDynamicLink
import re.notifica.models.NotificareNotification
import re.notifica.utilities.ktx.toCallbackFunction

public object Notificare {

    public const val SDK_VERSION: String = NOTIFICARE_VERSION

    // Intent actions
    internal const val INTENT_ACTION_READY = "re.notifica.intent.action.Ready"
    internal const val INTENT_ACTION_UNLAUNCHED = "re.notifica.intent.action.Unlaunched"
    internal const val INTENT_ACTION_DEVICE_REGISTERED = "re.notifica.intent.action.DeviceRegistered"

    // Intent extras
    public const val INTENT_EXTRA_APPLICATION: String = "re.notifica.intent.extra.Application"
    public const val INTENT_EXTRA_DEVICE: String = "re.notifica.intent.extra.Device"
    public const val INTENT_EXTRA_NOTIFICATION: String = "re.notifica.intent.extra.Notification"
    public const val INTENT_EXTRA_ACTION: String = "re.notifica.intent.extra.Action"

    // Internal modules
    internal lateinit var database: NotificareDatabase
        private set
    internal lateinit var sharedPreferences: NotificareSharedPreferences
        private set

    // internal var reachability: NotificareReachability? = null
    //     private set

    // Configurations
    private var context: WeakReference<Context>? = null

    @JvmStatic
    public var servicesInfo: NotificareServicesInfo? = null
        private set

    @JvmStatic
    public var options: NotificareOptions? = null
        private set

    // Launch / application state
    private var state: NotificareLaunchState = NotificareLaunchState.NONE

    // Listeners
    private val listeners = hashSetOf<WeakReference<Listener>>()

    private var installReferrerDetails: ReferrerDetails? = null

    // region Public API

    @JvmStatic
    public var intentReceiver: Class<out NotificareIntentReceiver> = NotificareIntentReceiver::class.java

    @JvmStatic
    public val isConfigured: Boolean
        get() = state >= NotificareLaunchState.CONFIGURED

    @JvmStatic
    public val isReady: Boolean
        get() = state == NotificareLaunchState.READY

    @JvmStatic
    public var application: NotificareApplication?
        get() {
            return if (::sharedPreferences.isInitialized) {
                sharedPreferences.application
            } else {
                NotificareLogger.warning("Calling this method requires Notificare to have been configured.")
                null
            }
        }
        private set(value) {
            if (::sharedPreferences.isInitialized) {
                sharedPreferences.application = value
            } else {
                NotificareLogger.warning("Calling this method requires Notificare to have been configured.")
            }
        }

    @JvmStatic
    public fun configure(context: Context) {
        val servicesInfo = loadServicesInfoFromResources(context)

        configure(context, servicesInfo)
    }

    @JvmStatic
    public fun configure(context: Context, applicationKey: String, applicationSecret: String) {
        val servicesInfo = NotificareServicesInfo(
            applicationKey = applicationKey,
            applicationSecret = applicationSecret,
        )

        configure(context, servicesInfo)
    }

    @JvmStatic
    public fun requireContext(): Context {
        return context?.get() ?: throw IllegalStateException("Cannot find context for Notificare.")
    }

    public suspend fun launch(): Unit = withContext(Dispatchers.IO) {
        if (state == NotificareLaunchState.NONE) {
            NotificareLogger.warning("Notificare.configure() has never been called. Cannot launch.")
            throw NotificareNotConfiguredException()
        }

        if (state > NotificareLaunchState.CONFIGURED) {
            NotificareLogger.warning("Notificare has already been launched. Skipping...")
            return@withContext
        }

        NotificareLogger.info("Launching Notificare.")
        state = NotificareLaunchState.LAUNCHING

        try {
            val application = fetchApplication()

            // Loop all possible modules and launch the available ones.
            NotificareModule.Module.entries.forEach { module ->
                module.instance?.run {
                    NotificareLogger.debug("Launching module: ${module.name.lowercase()}")
                    try {
                        this.launch()
                    } catch (e: Exception) {
                        NotificareLogger.debug("Failed to launch '${module.name.lowercase()}': $e")
                        throw e
                    }
                }
            }

            state = NotificareLaunchState.READY
            printLaunchSummary(application)

            // We're done launching. Send a broadcast.
            requireContext().sendBroadcast(
                Intent(requireContext(), intentReceiver)
                    .setAction(INTENT_ACTION_READY)
                    .putExtra(INTENT_EXTRA_APPLICATION, application)
            )

            onMainThread {
                listeners.forEach { it.get()?.onReady(application) }
            }
        } catch (e: Exception) {
            NotificareLogger.error("Failed to launch Notificare.", e)
            state = NotificareLaunchState.CONFIGURED
            throw e
        }

        launch {
            // Loop all possible modules and post-launch the available ones.
            NotificareModule.Module.entries.forEach { module ->
                module.instance?.run {
                    NotificareLogger.debug("Post-launching module: ${module.name.lowercase()}")
                    try {
                        this.postLaunch()
                    } catch (e: Exception) {
                        NotificareLogger.error("Failed to post-launch '${module.name.lowercase()}': $e")
                    }
                }
            }
        }
    }

    @JvmStatic
    public fun launch(
        callback: NotificareCallback<Unit>,
    ): Unit = toCallbackFunction(::launch)(callback::onSuccess, callback::onFailure)

    public suspend fun unlaunch(): Unit = withContext(Dispatchers.IO) {
        if (!isReady) {
            NotificareLogger.warning("Cannot un-launch Notificare before it has been launched.")
            throw NotificareNotReadyException()
        }

        NotificareLogger.info("Un-launching Notificare.")

        // Loop all possible modules and un-launch the available ones.
        NotificareModule.Module.entries.reversed().forEach { module ->
            module.instance?.run {
                NotificareLogger.debug("Un-launching module: ${module.name.lowercase()}.")

                try {
                    this.unlaunch()
                } catch (e: Exception) {
                    NotificareLogger.debug("Failed to un-launch ${module.name.lowercase()}': $e")
                    throw e
                }
            }
        }

        NotificareLogger.debug("Removing device.")
        deviceImplementation().delete()

        NotificareLogger.info("Un-launched Notificare.")
        state = NotificareLaunchState.CONFIGURED

        // We're done un-launching. Send a broadcast.
        requireContext().sendBroadcast(
            Intent(requireContext(), intentReceiver)
                .setAction(INTENT_ACTION_UNLAUNCHED)
        )

        onMainThread {
            listeners.forEach { it.get()?.onUnlaunched() }
        }
    }

    @JvmStatic
    public fun unlaunch(
        callback: NotificareCallback<Unit>,
    ): Unit = toCallbackFunction(::unlaunch)(callback::onSuccess, callback::onFailure)

    @JvmStatic
    public fun addListener(listener: Listener) {
        listeners.add(WeakReference(listener))
        NotificareLogger.debug("Added a new Notificare.Listener (${listeners.size} in total).")

        if (isReady) {
            onMainThread {
                listener.onReady(checkNotNull(application))
            }
        }
    }

    @JvmStatic
    public fun removeListener(listener: Listener) {
        val iterator = listeners.iterator()
        while (iterator.hasNext()) {
            val next = iterator.next().get()
            if (next == null || next == listener) {
                iterator.remove()
            }
        }
        NotificareLogger.debug("Removed a Notificare.Listener (${listeners.size} in total).")
    }

    public suspend fun fetchApplication(): NotificareApplication = withContext(Dispatchers.IO) {
        NotificareRequest.Builder()
            .get("/application/info")
            .responseDecodable(ApplicationResponse::class)
            .application
            .toModel()
            .also {
                // Update the cached copy.
                application = it
            }
    }

    @JvmStatic
    public fun fetchApplication(callback: NotificareCallback<NotificareApplication>): Unit =
        toCallbackFunction(::fetchApplication)(callback::onSuccess, callback::onFailure)

    public suspend fun fetchNotification(id: String): NotificareNotification = withContext(Dispatchers.IO) {
        if (!isConfigured) throw NotificareNotConfiguredException()

        NotificareRequest.Builder()
            .get("/notification/$id")
            .responseDecodable(NotificationResponse::class)
            .notification
            .toModel()
    }

    @JvmStatic
    public fun fetchNotification(id: String, callback: NotificareCallback<NotificareNotification>): Unit =
        toCallbackFunction(::fetchNotification)(id, callback::onSuccess, callback::onFailure)

    public suspend fun fetchDynamicLink(uri: Uri): NotificareDynamicLink = withContext(Dispatchers.IO) {
        if (!isConfigured) throw NotificareNotConfiguredException()

        val uriEncodedLink = URLEncoder.encode(uri.toString(), "UTF-8")

        NotificareRequest.Builder()
            .get("/link/dynamic/$uriEncodedLink")
            .query("platform", "Android")
            .query("deviceID", device().currentDevice?.id)
            .query("userID", device().currentDevice?.userId)
            .responseDecodable(DynamicLinkResponse::class)
            .link
    }

    @JvmStatic
    public fun fetchDynamicLink(uri: Uri, callback: NotificareCallback<NotificareDynamicLink>): Unit =
        toCallbackFunction(::fetchDynamicLink)(uri, callback::onSuccess, callback::onFailure)

    public suspend fun createNotificationReply(
        notification: NotificareNotification,
        action: NotificareNotification.Action,
        message: String? = null,
        media: String? = null,
        mimeType: String? = null,
    ): Unit = withContext(Dispatchers.IO) {
        if (!isConfigured) throw NotificareNotConfiguredException()

        val device = device().currentDevice
            ?: throw NotificareDeviceUnavailableException()

        NotificareRequest.Builder()
            .post(
                url = "/reply",
                body = CreateNotificationReplyPayload(
                    notificationId = notification.id,
                    deviceId = device.id,
                    userId = device.userId,
                    label = action.label,
                    data = CreateNotificationReplyPayload.Data(
                        target = action.target,
                        message = message,
                        media = media,
                        mimeType = mimeType,
                    ),
                )
            )
            .response()
    }

    public suspend fun callNotificationReplyWebhook(
        uri: Uri,
        data: Map<String, String>
    ): Unit = withContext(Dispatchers.IO) {
        val params = mutableMapOf<String, String?>()

        // Add all query parameters to the POST body.
        uri.queryParameterNames.forEach {
            params[it] = uri.getQueryParameter(it)
        }

        // Add our standard properties.
        params["userID"] = device().currentDevice?.userId
        params["deviceID"] = device().currentDevice?.id

        // Add all the items passed via data.
        params.putAll(data)

        NotificareRequest.Builder()
            .post(uri.toString(), params)
            .response()
    }

    public suspend fun uploadNotificationReplyAsset(
        payload: ByteArray,
        contentType: String
    ): String = withContext(Dispatchers.IO) {
        if (!isConfigured) throw NotificareNotConfiguredException()

        val response = NotificareRequest.Builder()
            .header("Content-Type", contentType)
            .post("/upload/reply", payload)
            .responseDecodable(NotificareUploadResponse::class)

        val host = checkNotNull(servicesInfo).hosts.restApi
        "$host/upload${response.filename}"
    }

    @InternalNotificareApi
    public fun removeNotificationFromNotificationCenter(notification: NotificareNotification) {
        cancelNotification(notification.id)
    }

    @JvmStatic
    public fun cancelNotification(id: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val notificationManager =
                requireContext().getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager

            if (notificationManager != null) {
                val groupKey = notificationManager.activeNotifications.find {
                    it != null && it.tag != null && it.tag == id
                }?.groupKey

                if (groupKey != null) {
                    // Check if there are more and if there is a summary.
                    var hasMore = false
                    var summaryTag: String? = null

                    for (statusBarNotification in notificationManager.activeNotifications) {
                        if (
                            statusBarNotification != null &&
                            statusBarNotification.groupKey != null &&
                            statusBarNotification.groupKey == groupKey
                        ) {
                            if (
                                (statusBarNotification.tag == null || statusBarNotification.tag != id) &&
                                statusBarNotification.id == 0
                            ) {
                                hasMore = true
                            } else if (statusBarNotification.id == 1) {
                                summaryTag = statusBarNotification.tag
                            }
                        }
                    }

                    if (!hasMore && summaryTag != null) {
                        notificationManager.cancel(id, 0)
                        notificationManager.cancel(summaryTag, 1)
                    } else {
                        notificationManager.cancel(id, 0)
                    }
                } else {
                    notificationManager.cancel(id, 0)
                }
            } else {
                NotificationManagerCompat.from(requireContext()).cancel(id, 0)
            }
        } else {
            NotificationManagerCompat.from(requireContext()).cancel(id, 0)
        }
    }

    @JvmStatic
    public fun handleTestDeviceIntent(intent: Intent): Boolean {
        val nonce = parseTestDeviceNonce(intent) ?: return false

        deviceImplementation().registerTestDevice(
            nonce,
            object : NotificareCallback<Unit> {
                override fun onSuccess(result: Unit) {
                    NotificareLogger.info("Device registered for testing.")
                }

                override fun onFailure(e: Exception) {
                    NotificareLogger.error("Failed to register the device for testing.", e)
                }
            }
        )

        return true
    }

    @JvmStatic
    public fun handleDynamicLinkIntent(activity: Activity, intent: Intent): Boolean {
        val uri = parseDynamicLink(intent) ?: return false

        NotificareLogger.debug("Handling a dynamic link.")
        fetchDynamicLink(
            uri,
            object : NotificareCallback<NotificareDynamicLink> {
                override fun onSuccess(result: NotificareDynamicLink) {
                    activity.startActivity(
                        Intent()
                            .setAction(Intent.ACTION_VIEW)
                            .setData(Uri.parse(result.target))
                    )
                }

                override fun onFailure(e: Exception) {
                    NotificareLogger.warning("Failed to fetch the dynamic link.", e)
                }
            }
        )

        return true
    }

    public suspend fun canEvaluateDeferredLink(): Boolean = withContext(Dispatchers.IO) {
        if (sharedPreferences.deferredLinkChecked != false) {
            return@withContext false
        }

        val context = requireContext()
        val referrerDetails = getInstallReferrerDetails(context)
        val installReferrer = referrerDetails?.installReferrer ?: return@withContext false
        val deferredLink = parseDeferredLink(installReferrer)

        return@withContext deferredLink != null
    }

    @JvmStatic
    public fun canEvaluateDeferredLink(
        callback: NotificareCallback<Boolean>,
    ): Unit = toCallbackFunction(::canEvaluateDeferredLink)(callback::onSuccess, callback::onFailure)

    public suspend fun evaluateDeferredLink(): Boolean = withContext(Dispatchers.IO) {
        if (sharedPreferences.deferredLinkChecked != false) {
            NotificareLogger.debug("Deferred link already evaluated.")
            return@withContext false
        }

        try {
            NotificareLogger.debug("Checking for a deferred link.")

            val context = requireContext()

            val referrerDetails = getInstallReferrerDetails(context)
            val installReferrer = referrerDetails?.installReferrer ?: run {
                NotificareLogger.debug("Install referrer information not found.")
                return@withContext false
            }

            val deferredLink = parseDeferredLink(installReferrer) ?: run {
                NotificareLogger.debug("Install referrer has no Notificare deferred link.")
                return@withContext false
            }

            val dynamicLink = fetchDynamicLink(deferredLink)

            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(dynamicLink.target))
                .addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
                )
                .setPackage(context.packageName)

            if (intent.resolveActivity(context.packageManager) == null) {
                NotificareLogger.warning("Cannot open a deep link that's not supported by the application.")
                return@withContext false
            }

            context.startActivity(intent)
        } finally {
            sharedPreferences.deferredLinkChecked = true
        }

        return@withContext true
    }

    @JvmStatic
    public fun evaluateDeferredLink(
        callback: NotificareCallback<Boolean>,
    ): Unit = toCallbackFunction(::evaluateDeferredLink)(callback::onSuccess, callback::onFailure)

    // endregion

    private fun loadServicesInfoFromResources(context: Context): NotificareServicesInfo {
        val applicationKey = try {
            context.getString(R.string.notificare_services_application_key)
        } catch (_: Resources.NotFoundException) {
            error("Application secret resource unavailable.")
        }

        val applicationSecret = try {
            context.getString(R.string.notificare_services_application_secret)
        } catch (_: Resources.NotFoundException) {
            error("Application secret resource unavailable.")
        }

        val hosts = try {
            val restApi = context.resources.getString(R.string.notificare_services_hosts_rest_api)
            val appLinks = context.resources.getString(R.string.notificare_services_hosts_app_links)
            val shortLinks = context.resources.getString(R.string.notificare_services_hosts_short_links)

            if (restApi.isNotBlank() && appLinks.isNotBlank() && shortLinks.isNotBlank()) {
                NotificareServicesInfo.Hosts(restApi, appLinks, shortLinks)
            } else {
                NotificareServicesInfo.Hosts()
            }
        } catch (_: Resources.NotFoundException) {
            NotificareServicesInfo.Hosts()
        }

        return NotificareServicesInfo(applicationKey, applicationSecret, hosts)
    }

    private fun printLaunchSummary(application: NotificareApplication) {
        val enabledServices = application.services.filter { it.value }.map { it.key }
        val enabledModules = NotificareUtils.getEnabledPeerModules()

        NotificareLogger.info("Notificare is ready to use for application.")
        NotificareLogger.debug("/==================================================================================/")
        NotificareLogger.debug("App name: ${application.name}")
        NotificareLogger.debug("App ID: ${application.id}")
        NotificareLogger.debug("App services: ${enabledServices.joinToString(", ")}")
        NotificareLogger.debug("/==================================================================================/")
        NotificareLogger.debug("SDK version: $SDK_VERSION")
        NotificareLogger.debug("SDK modules: ${enabledModules.joinToString(", ")}")
        NotificareLogger.debug("/==================================================================================/")
    }

    private fun configure(context: Context, servicesInfo: NotificareServicesInfo) {
        if (isConfigured) {
            NotificareLogger.warning("Notificare has already been configured. Skipping...")
            return
        }

        if (servicesInfo.applicationKey.isBlank() || servicesInfo.applicationSecret.isBlank()) {
            throw IllegalArgumentException("Notificare cannot be configured without an application key and secret.")
        }

        try {
            servicesInfo.validate()
        } catch (e: Exception) {
            NotificareLogger.error(
                "Could not validate the provided services configuration. Please check the contents are valid."
            )

            throw e
        }

        this.context = WeakReference(context.applicationContext)
        this.servicesInfo = servicesInfo
        this.options = NotificareOptions(context.applicationContext)

        // Late init modules
        this.database = NotificareDatabase.create(context.applicationContext)
        this.sharedPreferences = NotificareSharedPreferences(context.applicationContext)

        NotificareModule.Module.entries.forEach { module ->
            module.instance?.run {
                NotificareLogger.debug("Configuring module: ${module.name.lowercase()}")
                this.configure()
            }
        }

        if (!sharedPreferences.migrated) {
            NotificareLogger.debug("Checking if there is legacy data that needs to be migrated.")
            val migration = SharedPreferencesMigration(context)

            if (migration.hasLegacyData) {
                migration.migrate()
                NotificareLogger.info("Legacy data found and migrated to the new storage format.")
            }

            sharedPreferences.migrated = true
        }

        // The default value of the deferred link depends on whether Notificare has a registered device.
        // Having a registered device means the app ran at least once and we should stop checking for
        // deferred links.
        if (sharedPreferences.deferredLinkChecked == null) {
            sharedPreferences.deferredLinkChecked = sharedPreferences.device != null
        }

        NotificareLogger.debug("Notificare configured all services.")
        state = NotificareLaunchState.CONFIGURED

        if (!servicesInfo.hasDefaultHosts) {
            NotificareLogger.info("Notificare configured with customized hosts.")
            NotificareLogger.debug("REST API host: ${servicesInfo.hosts.restApi}")
            NotificareLogger.debug("AppLinks host: ${servicesInfo.hosts.appLinks}")
            NotificareLogger.debug("Short Links host: ${servicesInfo.hosts.shortLinks}")
        }
    }

    private fun parseTestDeviceNonce(intent: Intent): String? {
        val nonce = parseTestDeviceNonceLegacy(intent)
        if (nonce != null) return nonce

        val uri = intent.data ?: return null
        val pathSegments = uri.pathSegments ?: return null

        val application = Notificare.application ?: return null
        val appLinksDomain = servicesInfo?.hosts?.appLinks ?: return null

        if (
            uri.host == "${application.id}.$appLinksDomain" &&
            pathSegments.size >= 2 &&
            pathSegments[0] == "testdevice"
        ) {
            return pathSegments[1]
        }

        return null
    }

    private fun parseTestDeviceNonceLegacy(intent: Intent): String? {
        val application = application ?: return null
        val scheme = intent.data?.scheme ?: return null
        val pathSegments = intent.data?.pathSegments ?: return null

        // deep link: test.nc{applicationId}/host/testdevice/{nonce}
        if (scheme != "test.nc${application.id}") return null

        if (pathSegments.size != 2 || pathSegments[0] != "testdevice") return null

        return pathSegments[1]
    }

    private fun parseDynamicLink(intent: Intent): Uri? {
        val uri = intent.data ?: return null
        val host = uri.host ?: return null

        val servicesInfo = servicesInfo ?: run {
            NotificareLogger.warning("Unable to parse dynamic link. Notificare services have not been configured.")
            return null
        }

        if (!Pattern.matches("^([a-z0-9-])+\\.${Pattern.quote(servicesInfo.hosts.shortLinks)}$", host)) {
            NotificareLogger.debug("Domain pattern wasn't a match.")
            return null
        }

        if (uri.pathSegments?.size != 1) {
            NotificareLogger.debug("Path components length wasn't a match.")
            return null
        }

        val code = uri.pathSegments.first()
        if (!code.matches("^[a-zA-Z0-9_-]+$".toRegex())) {
            NotificareLogger.debug("First path component value wasn't a match.")
            return null
        }

        return uri
    }

    private fun parseDeferredLink(referrer: String): Uri? {
        return parseDeferredLinkFromEncodedQueryUrl(referrer)
            ?: parseDeferredLinkFromUrl(referrer)
    }

    private fun parseDeferredLinkFromEncodedQueryUrl(referrer: String): Uri? {
        val uri = Uri.Builder().encodedQuery(referrer).build()
        val link = uri.getQueryParameter("ntc_link")

        return link?.let { Uri.parse(it) }
    }

    private fun parseDeferredLinkFromUrl(referrer: String): Uri? {
        val uri = Uri.parse(referrer)
        val link = uri.getQueryParameter("ntc_link")

        return link?.let { Uri.parse(it) }
    }

    private suspend fun getInstallReferrerDetails(
        context: Context
    ): ReferrerDetails? = withContext(Dispatchers.IO) {
        val referrerDetails = installReferrerDetails
        if (referrerDetails != null) return@withContext referrerDetails

        val deferredReferrerDetails = CompletableDeferred<ReferrerDetails?>()

        val client = InstallReferrerClient.newBuilder(context.applicationContext).build()
        client.startConnection(object : InstallReferrerStateListener {
            override fun onInstallReferrerSetupFinished(responseCode: Int) {
                if (responseCode == InstallReferrerClient.InstallReferrerResponse.OK) {
                    try {
                        val referrer = client.installReferrer
                        deferredReferrerDetails.complete(referrer)
                    } catch (e: RemoteException) {
                        NotificareLogger.error("Failed to acquire the install referrer.", e)
                        deferredReferrerDetails.complete(null)
                    }
                } else {
                    NotificareLogger.error(
                        "Unable to acquire the install referrer. Play Store responded with code '$responseCode'."
                    )
                    deferredReferrerDetails.complete(null)
                }

                client.endConnection()
            }

            override fun onInstallReferrerServiceDisconnected() {
                if (!deferredReferrerDetails.isCompleted) {
                    NotificareLogger.warning("Lost connection to the Play Store before acquiring the install referrer.")
                    deferredReferrerDetails.complete(null)
                }
            }
        })

        return@withContext deferredReferrerDetails.await().also {
            installReferrerDetails = it
        }
    }

    public interface Listener {
        @MainThread
        public fun onReady(application: NotificareApplication) {
        }

        @MainThread
        public fun onUnlaunched() {
        }
    }
}
