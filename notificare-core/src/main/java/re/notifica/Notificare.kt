package re.notifica

import android.app.NotificationManager
import android.content.Context
import android.content.res.Resources
import android.net.Uri
import android.os.Build
import androidx.annotation.RestrictTo
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import re.notifica.app.NotificareIntentReceiver
import re.notifica.internal.*
import re.notifica.internal.network.push.ApplicationResponse
import re.notifica.internal.network.push.CreateNotificationReplyPayload
import re.notifica.internal.network.push.NotificareUploadResponse
import re.notifica.internal.network.push.NotificationResponse
import re.notifica.internal.network.request.NotificareRequest
import re.notifica.internal.storage.database.NotificareDatabase
import re.notifica.internal.storage.preferences.NotificareSharedPreferences
import re.notifica.models.NotificareApplication
import re.notifica.models.NotificareNotification
import re.notifica.modules.NotificareCrashReporter
import re.notifica.modules.NotificareDeviceManager
import re.notifica.modules.NotificareEventsManager
import re.notifica.modules.NotificareSessionManager
import java.lang.ref.WeakReference
import java.util.*

object Notificare {

    const val SDK_VERSION = BuildConfig.SDK_VERSION

    const val INTENT_EXTRA_NOTIFICATION = "re.notifica.intent.extra.Notification"
    const val INTENT_EXTRA_ACTION = "re.notifica.intent.extra.Action"

    // Internal modules
    val moshi = NotificareUtils.createMoshi()
    internal lateinit var database: NotificareDatabase
        private set
    internal lateinit var sharedPreferences: NotificareSharedPreferences
        private set
    internal val sessionManager = NotificareSessionManager()

    // internal var reachability: NotificareReachability? = null
    //     private set

    // Consumer modules
    val eventsManager = NotificareEventsManager()
    val deviceManager = NotificareDeviceManager()
    val crashReporter = NotificareCrashReporter()

    // Configurations
    private var context: WeakReference<Context>? = null
    internal var servicesConfig: NotificareServices? = null
        private set
    internal var applicationKey: String? = null
        private set
    internal var applicationSecret: String? = null
        private set
    var options: NotificareOptions? = null
        private set

    // Launch / application state
    private var state: NotificareLaunchState = NotificareLaunchState.NONE
    var application: NotificareApplication? = null
        private set

    // region Public API

    var intentReceiver: Class<out NotificareIntentReceiver> = NotificareIntentReceiver::class.java

    val isConfigured: Boolean
        get() = state >= NotificareLaunchState.CONFIGURED

    val isReady: Boolean
        get() = state == NotificareLaunchState.READY

    var useAdvancedLogging: Boolean
        get() = NotificareLogger.useAdvancedLogging
        set(value) {
            NotificareLogger.useAdvancedLogging = value
        }

    fun configure(context: Context) {
        val applicationKey = context.getString(R.string.notificare_services_application_key)
        val applicationSecret = context.getString(R.string.notificare_services_application_secret)

        configure(context, applicationKey, applicationSecret)
    }

    fun configure(context: Context, applicationKey: String, applicationSecret: String) {
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

    fun requireContext(): Context {
        return context?.get() ?: throw IllegalStateException("Cannot find context for Notificare.")
    }

    fun launch() {
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

        GlobalScope.launch(Dispatchers.IO) {
            try {
                val application = fetchApplication()

                sessionManager.launch()
                deviceManager.launch()
                eventsManager.launch()
                crashReporter.launch()

                // Loop all possible modules and launch the available ones.
                NotificareDefinitions.Module.values().forEach { module ->
                    module.instance?.run {
                        NotificareLogger.debug("Launching '${module.name.toLowerCase(Locale.ROOT)}' plugin.")
                        try {
                            this.launch()
                            NotificareLogger.debug("Launched '${module.name.toLowerCase(Locale.ROOT)}' plugin.")
                        } catch (e: Exception) {
                            NotificareLogger.debug("Failed to launch ${module.name.toLowerCase(Locale.ROOT)}': $e")
                            throw e
                        }
                    }
                }


                Notificare.application = application
                state = NotificareLaunchState.READY

                val enabledServices = application.services.filter { it.value }.map { it.key }
                val enabledModules = NotificareUtils.getLoadedModules()

                NotificareLogger.debug("/==================================================================================/")
                NotificareLogger.debug("Notificare SDK is ready to use for application")
                NotificareLogger.debug("App name: ${application.name}")
                NotificareLogger.debug("App ID: ${application.id}")
                NotificareLogger.debug("App services: ${enabledServices.joinToString(", ")}")
                NotificareLogger.debug("/==================================================================================/")
                NotificareLogger.debug("SDK version: ${NotificareDefinitions.SDK_VERSION}")
                NotificareLogger.debug("SDK modules: ${enabledModules.joinToString(", ")}")
                NotificareLogger.debug("/==================================================================================/")

                // We're done launching. Send a broadcast.
                NotificareIntentEmitter.onReady()
            } catch (e: Exception) {
                NotificareLogger.error("Failed to launch Notificare.", e)
                state = NotificareLaunchState.CONFIGURED
            }
        }
    }

    fun unlaunch() {

    }

    suspend fun fetchApplication(): NotificareApplication = withContext(Dispatchers.IO) {
        NotificareRequest.Builder()
            .get("/application/info")
            .responseDecodable(ApplicationResponse::class)
            .application
    }

    suspend fun fetchNotification(id: String): NotificareNotification = withContext(Dispatchers.IO) {
        if (!isConfigured) {
            throw NotificareException.NotReady()
        }

        NotificareRequest.Builder()
            .get("/notification/$id")
            .responseDecodable(NotificationResponse::class)
            .notification
    }

    suspend fun createNotificationReply(
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

    suspend fun callNotificationReplyWebhook(uri: Uri, data: Map<String, String>): Unit = withContext(Dispatchers.IO) {
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

    suspend fun uploadNotificationReplyAsset(payload: ByteArray, contentType: String): String =
        withContext(Dispatchers.IO) {
            if (!isConfigured) throw NotificareException.NotReady()

            val response = NotificareRequest.Builder()
                .header("Content-Type", contentType)
                .post("/upload/reply", payload)
                .responseDecodable(NotificareUploadResponse::class)

            "https://push.notifica.re/upload${response.filename}"
        }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    fun removeNotificationFromNotificationCenter(notification: NotificareNotification) {
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

        NotificareDefinitions.Module.values().forEach { module ->
            module.instance?.run {
                NotificareLogger.debug("Configuring plugin: ${module.name.toLowerCase(Locale.ROOT)}")
                this.configure()
            }
        }

        NotificareLogger.debug("Notificare configured all services.")
        state = NotificareLaunchState.CONFIGURED
    }
}
