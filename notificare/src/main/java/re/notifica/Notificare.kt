package re.notifica

import android.app.Activity
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.*
import re.notifica.internal.*
import re.notifica.internal.ktx.toCallbackFunction
import re.notifica.internal.network.push.*
import re.notifica.internal.network.request.NotificareRequest
import re.notifica.internal.storage.SharedPreferencesMigration
import re.notifica.internal.storage.database.NotificareDatabase
import re.notifica.internal.storage.preferences.NotificareSharedPreferences
import re.notifica.models.NotificareApplication
import re.notifica.models.NotificareDynamicLink
import re.notifica.models.NotificareNotification
import re.notifica.modules.NotificareModule
import java.lang.ref.WeakReference
import java.net.URLEncoder
import java.util.regex.Pattern

public object Notificare {

    public const val SDK_VERSION: String = BuildConfig.SDK_VERSION

    public const val INTENT_EXTRA_NOTIFICATION: String = "re.notifica.intent.extra.Notification"
    public const val INTENT_EXTRA_ACTION: String = "re.notifica.intent.extra.Action"

    // Internal modules
    internal lateinit var database: NotificareDatabase
        private set
    internal lateinit var sharedPreferences: NotificareSharedPreferences
        private set
    internal val sessionManager = NotificareSessionManager()
    internal val crashReporter = CrashReporter()

    // internal var reachability: NotificareReachability? = null
    //     private set

    // Consumer modules
    public val eventsManager: NotificareEventsManager = NotificareEventsManager()
    public val deviceManager: NotificareDeviceManager = NotificareDeviceManager()

    // Configurations
    private var context: WeakReference<Context>? = null
    public var servicesConfig: NotificareServices? = null
        private set
    public var applicationKey: String? = null
        private set
    public var applicationSecret: String? = null
        private set
    public var options: NotificareOptions? = null
        private set

    // Launch / application state
    private var state: NotificareLaunchState = NotificareLaunchState.NONE

    // Listeners
    private val readyListeners = hashSetOf<OnReadyListener>()

    // region Public API

    public var intentReceiver: Class<out NotificareIntentReceiver> = NotificareIntentReceiver::class.java

    public val isConfigured: Boolean
        get() = state >= NotificareLaunchState.CONFIGURED

    public val isReady: Boolean
        get() = state == NotificareLaunchState.READY

    public var useAdvancedLogging: Boolean
        get() = NotificareLogger.useAdvancedLogging
        set(value) {
            NotificareLogger.useAdvancedLogging = value
        }

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

    public fun configure(context: Context) {
        val applicationKey = context.getString(R.string.notificare_services_application_key)
        val applicationSecret = context.getString(R.string.notificare_services_application_secret)

        configure(context, applicationKey, applicationSecret)
    }

    public fun configure(context: Context, applicationKey: String, applicationSecret: String) {
        val services = try {
            val useTestApi = context.resources.getBoolean(R.bool.notificare_services_use_test_api)
            if (useTestApi) {
                NotificareServices.TEST
            } else {
                NotificareServices.PRODUCTION
            }
        } catch (e: Resources.NotFoundException) {
            NotificareServices.PRODUCTION
        }

        configure(context, applicationKey, applicationSecret, services)
    }

    public fun requireContext(): Context {
        return context?.get() ?: throw IllegalStateException("Cannot find context for Notificare.")
    }

    public fun launch() {
        if (state == NotificareLaunchState.NONE) {
            NotificareLogger.warning("Notificare.configure() has never been called. Cannot launch.")
            return
        }

        if (state > NotificareLaunchState.CONFIGURED) {
            NotificareLogger.warning("Notificare has already been launched. Skipping...")
            return
        }

        NotificareLogger.info("Launching Notificare.")
        state = NotificareLaunchState.LAUNCHING

        @OptIn(DelicateCoroutinesApi::class)
        GlobalScope.launch(Dispatchers.IO) {
            try {
                val application = fetchApplication()

                sessionManager.launch()
                deviceManager.launch()
                eventsManager.launch()
                crashReporter.launch()

                // Loop all possible modules and launch the available ones.
                NotificareModule.Module.values().forEach { module ->
                    module.instance?.run {
                        NotificareLogger.debug("Launching '${module.name.lowercase()}' plugin.")
                        try {
                            this.launch()
                            NotificareLogger.debug("Launched '${module.name.lowercase()}' plugin.")
                        } catch (e: Exception) {
                            NotificareLogger.debug("Failed to launch ${module.name.lowercase()}': $e")
                            throw e
                        }
                    }
                }

                state = NotificareLaunchState.READY

                val enabledServices = application.services.filter { it.value }.map { it.key }
                val enabledModules = NotificareUtils.getLoadedModules()

                NotificareLogger.debug("/==================================================================================/")
                NotificareLogger.debug("Notificare SDK is ready to use for application")
                NotificareLogger.debug("App name: ${application.name}")
                NotificareLogger.debug("App ID: ${application.id}")
                NotificareLogger.debug("App services: ${enabledServices.joinToString(", ")}")
                NotificareLogger.debug("/==================================================================================/")
                NotificareLogger.debug("SDK version: $SDK_VERSION")
                NotificareLogger.debug("SDK modules: ${enabledModules.joinToString(", ")}")
                NotificareLogger.debug("/==================================================================================/")

                // We're done launching. Send a broadcast.
                requireContext().sendBroadcast(
                    Intent(requireContext(), intentReceiver)
                        .setAction(NotificareIntentReceiver.INTENT_ACTION_READY)
                        .putExtra(NotificareIntentReceiver.INTENT_EXTRA_APPLICATION, application)
                )

                // Notify the listeners.
                readyListeners.forEach { it.onReady(application) }
            } catch (e: Exception) {
                NotificareLogger.error("Failed to launch Notificare.", e)
                state = NotificareLaunchState.CONFIGURED
            }
        }
    }

    public fun unlaunch() {
        if (!isReady) {
            NotificareLogger.warning("Cannot un-launch Notificare before it has been launched.")
            return
        }

        NotificareLogger.info("Un-launching Notificare.")

        @OptIn(DelicateCoroutinesApi::class)
        GlobalScope.launch {
            try {
                NotificareLogger.debug("Registering a temporary device.")
                deviceManager.registerTemporary()

                // Loop all possible modules and un-launch the available ones.
                NotificareModule.Module.values().reversed().forEach { module ->
                    module.instance?.run {
                        NotificareLogger.debug("Un-launching '${module.name.lowercase()}' plugin.")

                        try {
                            this.unlaunch()
                            NotificareLogger.debug("Un-launched '${module.name.lowercase()}' plugin.")
                        } catch (e: Exception) {
                            NotificareLogger.debug("Failed to un-launch ${module.name.lowercase()}': $e")
                            throw e
                        }
                    }
                }

                NotificareLogger.debug("Clearing device tags.")
                deviceManager.clearTags()

                NotificareLogger.debug("Removing device.")
                deviceManager.delete()

                NotificareLogger.info("Un-launched Notificare.")
                state = NotificareLaunchState.CONFIGURED
            } catch (e: Exception) {
                NotificareLogger.error("Failed to un-launch Notificare.", e)
            }
        }
    }

    public fun addOnReadyListener(listener: OnReadyListener) {
        readyListeners.add(listener)
        NotificareLogger.debug("Added a new OnReadyListener (${readyListeners.size} in total).")

        if (isReady) {
            listener.onReady(checkNotNull(application))
        }
    }

    public fun removeOnReadyListener(listener: OnReadyListener) {
        readyListeners.remove(listener)
        NotificareLogger.debug("Removed an OnReadyListener (${readyListeners.size} in total).")
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

    public fun fetchApplication(callback: NotificareCallback<NotificareApplication>): Unit =
        toCallbackFunction(::fetchApplication)(callback)

    public suspend fun fetchNotification(id: String): NotificareNotification = withContext(Dispatchers.IO) {
        if (!isConfigured) {
            throw NotificareException.NotReady()
        }

        NotificareRequest.Builder()
            .get("/notification/$id")
            .responseDecodable(NotificationResponse::class)
            .notification
            .toModel()
    }

    public fun fetchNotification(id: String, callback: NotificareCallback<NotificareNotification>): Unit =
        toCallbackFunction(::fetchNotification)(id, callback)

    public suspend fun fetchDynamicLink(uri: Uri): NotificareDynamicLink = withContext(Dispatchers.IO) {
        if (!isConfigured) {
            throw NotificareException.NotReady()
        }

        @Suppress("BlockingMethodInNonBlockingContext")
        val uriEncodedLink = URLEncoder.encode(uri.toString(), "UTF-8")

        NotificareRequest.Builder()
            .get("/link/dynamic/${uriEncodedLink}")
            .query("platform", "Android")
            .query("deviceID", deviceManager.currentDevice?.id)
            .query("userID", deviceManager.currentDevice?.userId)
            .responseDecodable(DynamicLinkResponse::class)
            .link
    }

    public fun fetchDynamicLink(uri: Uri, callback: NotificareCallback<NotificareDynamicLink>): Unit =
        toCallbackFunction(::fetchDynamicLink)(uri, callback)

    public suspend fun createNotificationReply(
        notification: NotificareNotification,
        action: NotificareNotification.Action,
        message: String? = null,
        media: String? = null,
        mimeType: String? = null,
    ): Unit = withContext(Dispatchers.IO) {
        val device = deviceManager.currentDevice
        if (!isReady || device == null) {
            throw NotificareException.NotReady()
        }

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
        params["userID"] = deviceManager.currentDevice?.userId
        params["deviceID"] = deviceManager.currentDevice?.id

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
        if (!isConfigured) throw NotificareException.NotReady()

        val response = NotificareRequest.Builder()
            .header("Content-Type", contentType)
            .post("/upload/reply", payload)
            .responseDecodable(NotificareUploadResponse::class)

        "https://push.notifica.re/upload${response.filename}"
    }

    @InternalNotificareApi
    public fun removeNotificationFromNotificationCenter(notification: NotificareNotification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val notificationManager =
                requireContext().getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager

            if (notificationManager != null) {
                val groupKey = notificationManager.activeNotifications.find {
                    it != null && it.tag != null && it.tag == notification.id
                }?.groupKey

                if (groupKey != null) {
                    // Check if there are more and if there is a summary.
                    var hasMore = false
                    var summaryTag: String? = null

                    for (statusBarNotification in notificationManager.activeNotifications) {
                        if (statusBarNotification != null && statusBarNotification.groupKey != null && statusBarNotification.groupKey == groupKey) {
                            if ((statusBarNotification.tag == null || statusBarNotification.tag != notification.id) && statusBarNotification.id == 0) {
                                hasMore = true
                            } else if (statusBarNotification.id == 1) {
                                summaryTag = statusBarNotification.tag
                            }
                        }
                    }

                    if (!hasMore && summaryTag != null) {
                        notificationManager.cancel(notification.id, 0)
                        notificationManager.cancel(summaryTag, 1)
                    } else {
                        notificationManager.cancel(notification.id, 0)
                    }
                } else {
                    notificationManager.cancel(notification.id, 0)
                }
            } else {
                NotificationManagerCompat.from(requireContext()).cancel(notification.id, 0)
            }
        } else {
            NotificationManagerCompat.from(requireContext()).cancel(notification.id, 0)
        }
    }

    public fun handleTestDeviceIntent(intent: Intent): Boolean {
        val nonce = parseTestDeviceNonce(intent) ?: return false

        deviceManager.registerTestDevice(nonce, object : NotificareCallback<Unit> {
            override fun onSuccess(result: Unit) {
                NotificareLogger.info("Device registered for testing.")
            }

            override fun onFailure(e: Exception) {
                NotificareLogger.error("Failed to register the device for testing.", e)
            }
        })

        return true
    }

    public fun handleDynamicLinkIntent(activity: Activity, intent: Intent): Boolean {
        val uri = parseDynamicLink(intent) ?: return false

        NotificareLogger.debug("Handling a dynamic link.")
        fetchDynamicLink(uri, object : NotificareCallback<NotificareDynamicLink> {
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
        })

        return true
    }

    // endregion

    private fun configure(
        context: Context,
        applicationKey: String,
        applicationSecret: String,
        services: NotificareServices
    ) {
        if (applicationKey.isBlank() || applicationSecret.isBlank()) {
            throw IllegalArgumentException("Notificare cannot be configured without an application key and secret.")
        }

        this.context = WeakReference(context.applicationContext)
        this.servicesConfig = services
        this.applicationKey = applicationKey
        this.applicationSecret = applicationSecret
        this.options = NotificareOptions(context.applicationContext)

        // Late init modules
        this.database = NotificareDatabase.create(context.applicationContext)
        this.sharedPreferences = NotificareSharedPreferences(context.applicationContext)

        NotificareLogger.debug("Configuring available modules.")
        sessionManager.configure()
        crashReporter.configure()
        // database.configure()
        eventsManager.configure()
        deviceManager.configure()

        NotificareModule.Module.values().forEach { module ->
            module.instance?.run {
                NotificareLogger.debug("Configuring plugin: ${module.name.lowercase()}")
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

        NotificareLogger.debug("Notificare configured all services.")
        state = NotificareLaunchState.CONFIGURED
    }

    private fun parseTestDeviceNonce(intent: Intent): String? {
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

        val services = servicesConfig ?: run {
            NotificareLogger.warning("Unable to parse dynamic link. Notificare services have not been configured.")
            return null
        }

        if (!Pattern.matches("^([a-z0-9-])+\\.${Pattern.quote(services.dynamicLinkDomain)}$", host)) {
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

    public interface OnReadyListener {
        public fun onReady(application: NotificareApplication)
    }
}
