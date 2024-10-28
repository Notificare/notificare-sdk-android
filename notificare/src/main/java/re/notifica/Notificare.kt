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
import re.notifica.internal.NotificareModule
import re.notifica.internal.NotificareOptions
import re.notifica.internal.NotificareUtils
import re.notifica.internal.logger
import re.notifica.utilities.threading.onMainThread
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
import re.notifica.utilities.coroutines.toCallbackFunction

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

    /**
     * Specifies the intent receiver class for handling general Notificare intents.
     *
     * This property defines the class that will receive and process intents related to Notificare services.
     * The class must extend [NotificareIntentReceiver].
     */
    @JvmStatic
    public var intentReceiver: Class<out NotificareIntentReceiver> = NotificareIntentReceiver::class.java

    /**
     * Indicates whether Notificare has been configured.
     *
     * This property returns `true` if Notificare is successfully configured with the required context and credentials,
     * and `false` otherwise.
     */
    @JvmStatic
    public val isConfigured: Boolean
        get() = state >= NotificareLaunchState.CONFIGURED

    /**
     * Indicates whether Notificare is ready to process notifications and other events.
     *
     * This property returns `true` once the SDK has completed the initialization process and is ready for use.
     */
    @JvmStatic
    public val isReady: Boolean
        get() = state == NotificareLaunchState.READY

    /**
     * Provides the current application metadata, if available.
     *
     * This property returns the [NotificareApplication] object representing the configured application,
     * or `null` if the application is not yet available.
     *
     * @see [NotificareApplication]
     */
    @JvmStatic
    public var application: NotificareApplication?
        get() {
            return if (::sharedPreferences.isInitialized) {
                sharedPreferences.application
            } else {
                logger.warning("Calling this method requires Notificare to have been configured.")
                null
            }
        }
        private set(value) {
            if (::sharedPreferences.isInitialized) {
                sharedPreferences.application = value
            } else {
                logger.warning("Calling this method requires Notificare to have been configured.")
            }
        }

    /**
     * Configures Notificare with the application context using the services info in the provided configuration file.
     *
     * This method initializes the SDK using the given context and the services info in the provided
     * `notificare-services.json` file, and prepares it for use.
     *
     * @param context The [Context] to use for configuration.
     */
    @JvmStatic
    public fun configure(context: Context) {
        val servicesInfo = loadServicesInfoFromResources(context)

        configure(context, servicesInfo)
    }

    /**
     * Configures Notificare with a specific application key and secret.
     *
     * This method initializes the SDK using the given context along with the provided application Key and
     * application Secret.
     *
     * @param context The [Context] to use for configuration.
     * @param applicationKey The key for the Notificare application.
     * @param applicationSecret The secret for the Notificare application.
     */
    @JvmStatic
    public fun configure(context: Context, applicationKey: String, applicationSecret: String) {
        val servicesInfo = NotificareServicesInfo(
            applicationKey = applicationKey,
            applicationSecret = applicationSecret,
        )

        configure(context, servicesInfo)
    }

    /**
     * Returns the application context required by Notificare.
     *
     * This method throws an exception if Notificare is not properly configured.
     *
     * @throws IllegalStateException if Notificare is not configured.
     * @return The [Context] used by Notificare.
     */
    @JvmStatic
    public fun requireContext(): Context {
        return context?.get() ?: throw IllegalStateException("Cannot find context for Notificare.")
    }

    /**
     * Launches the Notificare SDK, and all the additional available modules, preparing them for use.
     * This registers the device as a non-push device.
     *
     * This method suspends until the SDK is fully launched and ready to process events.
     */
    public suspend fun launch(): Unit = withContext(Dispatchers.IO) {
        if (state == NotificareLaunchState.NONE) {
            logger.warning("Notificare.configure() has never been called. Cannot launch.")
            throw NotificareNotConfiguredException()
        }

        if (state > NotificareLaunchState.CONFIGURED) {
            logger.warning("Notificare has already been launched. Skipping...")
            return@withContext
        }

        logger.info("Launching Notificare.")
        state = NotificareLaunchState.LAUNCHING

        try {
            val application = fetchApplication(saveToSharedPreferences = false)
            val storedApplication = sharedPreferences.application

            if (storedApplication != null && storedApplication.id != application.id) {
                logger.warning("Incorrect application keys detected. Resetting Notificare to a clean state.")

                NotificareModule.Module.entries.forEach { module ->
                    module.instance?.run {
                        logger.debug("Resetting module: ${module.name.lowercase()}")
                        try {
                            this.clearStorage()
                        } catch (e: Exception) {
                            logger.debug("Failed to reset '${module.name.lowercase()}': $e")
                            throw e
                        }
                    }
                }

                database.events().clear()
                sharedPreferences.clear()
            }

            sharedPreferences.application = application

            // Loop all possible modules and launch the available ones.
            NotificareModule.Module.entries.forEach { module ->
                module.instance?.run {
                    logger.debug("Launching module: ${module.name.lowercase()}")
                    try {
                        this.launch()
                    } catch (e: Exception) {
                        logger.debug("Failed to launch '${module.name.lowercase()}': $e")
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
            logger.error("Failed to launch Notificare.", e)
            state = NotificareLaunchState.CONFIGURED
            throw e
        }

        launch {
            // Loop all possible modules and post-launch the available ones.
            NotificareModule.Module.entries.forEach { module ->
                module.instance?.run {
                    logger.debug("Post-launching module: ${module.name.lowercase()}")
                    try {
                        this.postLaunch()
                    } catch (e: Exception) {
                        logger.error("Failed to post-launch '${module.name.lowercase()}': $e")
                    }
                }
            }
        }
    }

    /**
     * Launches the Notificare SDK, and all the additional available modules, with a callback.
     * This registers the device as a non-push device.
     *
     * This method prepares the SDK for use and invokes the provided callback once the launch is complete.
     *
     * @param callback The callback to invoke when the launch is complete.
     */
    @JvmStatic
    public fun launch(
        callback: NotificareCallback<Unit>,
    ): Unit = toCallbackFunction(::launch)(callback::onSuccess, callback::onFailure)

    /**
     * Unlaunches the Notificare SDK, shutting down its operations and removing all data, both locally and remotely in
     * the servers.
     *
     * This method suspends until the SDK has completed its shutdown process. It destroys all the device's data
     * permanently.
     */
    public suspend fun unlaunch(): Unit = withContext(Dispatchers.IO) {
        if (!isReady) {
            logger.warning("Cannot un-launch Notificare before it has been launched.")
            throw NotificareNotReadyException()
        }

        logger.info("Un-launching Notificare.")

        // Loop all possible modules and un-launch the available ones.
        NotificareModule.Module.entries.reversed().forEach { module ->
            module.instance?.run {
                logger.debug("Un-launching module: ${module.name.lowercase()}.")

                try {
                    this.unlaunch()
                } catch (e: Exception) {
                    logger.debug("Failed to un-launch ${module.name.lowercase()}': $e")
                    throw e
                }
            }
        }

        logger.debug("Removing device.")
        deviceImplementation().delete()

        logger.info("Un-launched Notificare.")
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

    /**
     * Unlaunches the Notificare SDK with a callback.
     *
     * This method shuts down the SDK and invokes the provided [callback] once the shutdown is complete. It destroys all
     * the device's data permanently.
     *
     * @param callback The callback to invoke when the SDK is unlaunched.
     */
    @JvmStatic
    public fun unlaunch(
        callback: NotificareCallback<Unit>,
    ): Unit = toCallbackFunction(::unlaunch)(callback::onSuccess, callback::onFailure)

    /**
     * Registers a listener to receive SDK lifecycle events.
     *
     * This method adds a [Listener] to receive callbacks for important SDK events such as when it is launched,
     * unlaunched, or encounters errors.
     *
     * @param listener The [Listener] to register for receiving events.
     */
    @JvmStatic
    public fun addListener(listener: Listener) {
        listeners.add(WeakReference(listener))
        logger.debug("Added a new Notificare.Listener (${listeners.size} in total).")

        if (isReady) {
            onMainThread {
                listener.onReady(checkNotNull(application))
            }
        }
    }

    /**
     * Unregisters a previously added listener.
     *
     * This method removes the specified [Listener] to stop receiving callbacks for SDK events.
     *
     * @param listener The [Listener] to remove.
     */
    @JvmStatic
    public fun removeListener(listener: Listener) {
        val iterator = listeners.iterator()
        while (iterator.hasNext()) {
            val next = iterator.next().get()
            if (next == null || next == listener) {
                iterator.remove()
            }
        }
        logger.debug("Removed a Notificare.Listener (${listeners.size} in total).")
    }

    /**
     * Fetches the application metadata.
     *
     * This method suspends until the [NotificareApplication] object is fetched.
     *
     * @return The [NotificareApplication] metadata.
     */
    public suspend fun fetchApplication(): NotificareApplication = withContext(Dispatchers.IO) {
        fetchApplication(saveToSharedPreferences = true)
    }

    /**
     * Fetches the application metadata with a callback.
     *
     * This method fetches the [NotificareApplication] metadata and invokes the provided callback once the data is
     * available.
     *
     * @param callback The callback to invoke with the fetched application metadata.
     */
    @JvmStatic
    public fun fetchApplication(callback: NotificareCallback<NotificareApplication>): Unit =
        toCallbackFunction<NotificareApplication>(::fetchApplication)(callback::onSuccess, callback::onFailure)

    /**
     * Fetches a notification by its ID.
     *
     * This method suspends until the [NotificareNotification] object is fetched.
     *
     * @param id The ID of the notification to fetch.
     * @return The [NotificareNotification] object associated with the provided ID.
     */
    public suspend fun fetchNotification(id: String): NotificareNotification = withContext(Dispatchers.IO) {
        if (!isConfigured) throw NotificareNotConfiguredException()

        NotificareRequest.Builder()
            .get("/notification/$id")
            .responseDecodable(NotificationResponse::class)
            .notification
            .toModel()
    }

    /**
     * Fetches a notification by its ID with a callback.
     *
     * This method fetches the [NotificareNotification] and invokes the provided callback once the data is available.
     *
     * @param id The ID of the notification to fetch.
     * @param callback The callback to invoke with the fetched notification.
     */
    @JvmStatic
    public fun fetchNotification(id: String, callback: NotificareCallback<NotificareNotification>): Unit =
        toCallbackFunction(::fetchNotification)(id, callback::onSuccess, callback::onFailure)

    /**
     * Fetches a dynamic link from a URI.
     *
     * This method suspends until the [NotificareDynamicLink] object is fetched based on the provided URI.
     *
     * @param uri The URI to fetch the dynamic link from.
     * @return The [NotificareDynamicLink] object.
     */
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

    /**
     * Fetches a dynamic link from a URI with a callback.
     *
     * This method fetches the [NotificareDynamicLink] object based on the provided URI and invokes the provided
     * callback once the data is available.
     *
     * @param uri The URI to fetch the dynamic link from.
     * @param callback The callback to invoke with the fetched dynamic link.
     */
    @JvmStatic
    public fun fetchDynamicLink(uri: Uri, callback: NotificareCallback<NotificareDynamicLink>): Unit =
        toCallbackFunction(::fetchDynamicLink)(uri, callback::onSuccess, callback::onFailure)

    /**
     * Sends a reply to a notification action.
     *
     * This method sends a reply to the specified [NotificareNotification] and [NotificareNotification.Action],
     * optionally including a message and media.
     *
     * @param notification The notification to reply to.
     * @param action The action associated with the reply.
     * @param message An optional message to include with the reply.
     * @param media An optional media file to attach with the reply.
     * @param mimeType The MIME type of the media.
     */
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

    /**
     * Calls a notification reply webhook.
     *
     * This method sends data to the specified webhook [uri].
     *
     * @param uri The webhook URI.
     * @param data The data to send in the request.
     */
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

    /**
     * Uploads an asset for a notification reply.
     *
     * This method uploads a binary payload as part of a notification reply.
     *
     * @param payload The byte array containing the asset data.
     * @param contentType The MIME type of the asset.
     * @return The URL of the uploaded asset.
     */
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
        "https://$host/upload${response.filename}"
    }

    @InternalNotificareApi
    public fun removeNotificationFromNotificationCenter(notification: NotificareNotification) {
        cancelNotification(notification.id)
    }

    /**
     * Cancels a notification by its ID.
     *
     * This method cancels a notification with the given ID, removing it from the notification center.
     *
     * @param id The ID of the notification to cancel.
     */
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

    /**
     * Handles an intent to register the current device as a test device for Notificare services.
     *
     * This method processes the provided intent and attempts to register the device
     * for testing purposes.
     *
     * @param intent The intent containing the test device nonce.
     * @return `true` if the device registration process was initiated, or `false` if no valid nonce was found in the
     * intent.
     */
    @JvmStatic
    public fun handleTestDeviceIntent(intent: Intent): Boolean {
        val nonce = parseTestDeviceNonce(intent) ?: return false

        deviceImplementation().registerTestDevice(
            nonce,
            object : NotificareCallback<Unit> {
                override fun onSuccess(result: Unit) {
                    logger.info("Device registered for testing.")
                }

                override fun onFailure(e: Exception) {
                    logger.error("Failed to register the device for testing.", e)
                }
            }
        )

        return true
    }

    /**
     * Handles an intent for dynamic links.
     *
     * This method processes the provided [intent] in the context of an [Activity] for handling dynamic links.
     *
     * @param activity The activity in which the intent is handled.
     * @param intent The intent to handle.
     * @return `true` if the intent was handled, `false` otherwise.
     */
    @JvmStatic
    public fun handleDynamicLinkIntent(activity: Activity, intent: Intent): Boolean {
        val uri = parseDynamicLink(intent) ?: return false

        logger.debug("Handling a dynamic link.")
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
                    logger.warning("Failed to fetch the dynamic link.", e)
                }
            }
        )

        return true
    }

    /**
     * Determines if a deferred link can be evaluated.
     *
     * This method suspends until it is determined whether the deferred link can be evaluated.
     *
     * @return `true` if a deferred link can be evaluated, `false` otherwise.
     */
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

    /**
     * Determines if a deferred link can be evaluated with a callback.
     *
     * This method checks if a deferred link can be evaluated and invokes the provided [callback] with the result.
     *
     * @param callback The callback to invoke with the result.
     */
    @JvmStatic
    public fun canEvaluateDeferredLink(
        callback: NotificareCallback<Boolean>,
    ): Unit = toCallbackFunction(::canEvaluateDeferredLink)(callback::onSuccess, callback::onFailure)

    /**
     * Evaluates the deferred link, triggering an intent with the resolved deferred link.
     *
     * This method suspends until the deferred link is evaluated.
     *
     * @return `true` if the deferred link was successfully evaluated, `false` otherwise.
     */
    public suspend fun evaluateDeferredLink(): Boolean = withContext(Dispatchers.IO) {
        if (sharedPreferences.deferredLinkChecked != false) {
            logger.debug("Deferred link already evaluated.")
            return@withContext false
        }

        try {
            logger.debug("Checking for a deferred link.")

            val context = requireContext()

            val referrerDetails = getInstallReferrerDetails(context)
            val installReferrer = referrerDetails?.installReferrer ?: run {
                logger.debug("Install referrer information not found.")
                return@withContext false
            }

            val deferredLink = parseDeferredLink(installReferrer) ?: run {
                logger.debug("Install referrer has no Notificare deferred link.")
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
                logger.warning("Cannot open a deep link that's not supported by the application.")
                return@withContext false
            }

            context.startActivity(intent)
        } finally {
            sharedPreferences.deferredLinkChecked = true
        }

        return@withContext true
    }

    /**
     * Evaluates the deferred and triggers an intent with the resolved deferred link with a callback.
     *
     * This method evaluates the deferred link and invokes the provided [callback] with the result.
     *
     * @param callback The callback to invoke with the result.
     */
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

        logger.info("Notificare is ready to use for application.")
        logger.debug("/==================================================================================/")
        logger.debug("App name: ${application.name}")
        logger.debug("App ID: ${application.id}")
        logger.debug("App services: ${enabledServices.joinToString(", ")}")
        logger.debug("/==================================================================================/")
        logger.debug("SDK version: $SDK_VERSION")
        logger.debug("SDK modules: ${enabledModules.joinToString(", ")}")
        logger.debug("/==================================================================================/")
    }

    private fun configure(context: Context, servicesInfo: NotificareServicesInfo) {
        if (state > NotificareLaunchState.CONFIGURED) {
            logger.warning("Unable to reconfigure Notificare once launched.")
            return
        }

        if (state == NotificareLaunchState.CONFIGURED) {
            logger.info("Reconfiguring Notificare with another set of application keys.")
        }

        if (servicesInfo.applicationKey.isBlank() || servicesInfo.applicationSecret.isBlank()) {
            throw IllegalArgumentException("Notificare cannot be configured without an application key and secret.")
        }

        try {
            servicesInfo.validate()
        } catch (e: Exception) {
            logger.error(
                "Could not validate the provided services configuration. Please check the contents are valid."
            )

            throw e
        }

        this.context = WeakReference(context.applicationContext)
        this.servicesInfo = servicesInfo
        this.options = NotificareOptions(context.applicationContext)

        logger.hasDebugLoggingEnabled = checkNotNull(this.options).debugLoggingEnabled

        // Late init modules
        this.database = NotificareDatabase.create(context.applicationContext)
        this.sharedPreferences = NotificareSharedPreferences(context.applicationContext)

        NotificareModule.Module.entries.forEach { module ->
            module.instance?.run {
                logger.debug("Configuring module: ${module.name.lowercase()}")
                this.configure()
            }
        }

        if (!sharedPreferences.migrated) {
            logger.debug("Checking if there is legacy data that needs to be migrated.")
            val migration = SharedPreferencesMigration(context)

            if (migration.hasLegacyData) {
                migration.migrate()
                logger.info("Legacy data found and migrated to the new storage format.")
            }

            sharedPreferences.migrated = true
        }

        // The default value of the deferred link depends on whether Notificare has a registered device.
        // Having a registered device means the app ran at least once and we should stop checking for
        // deferred links.
        if (sharedPreferences.deferredLinkChecked == null) {
            sharedPreferences.deferredLinkChecked = sharedPreferences.device != null
        }

        logger.debug("Notificare configured all services.")
        state = NotificareLaunchState.CONFIGURED

        if (!servicesInfo.hasDefaultHosts) {
            logger.info("Notificare configured with customized hosts.")
            logger.debug("REST API host: ${servicesInfo.hosts.restApi}")
            logger.debug("AppLinks host: ${servicesInfo.hosts.appLinks}")
            logger.debug("Short Links host: ${servicesInfo.hosts.shortLinks}")
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
            logger.warning("Unable to parse dynamic link. Notificare services have not been configured.")
            return null
        }

        if (!Pattern.matches("^([a-z0-9-])+\\.${Pattern.quote(servicesInfo.hosts.shortLinks)}$", host)) {
            logger.debug("Domain pattern wasn't a match.")
            return null
        }

        if (uri.pathSegments?.size != 1) {
            logger.debug("Path components length wasn't a match.")
            return null
        }

        val code = uri.pathSegments.first()
        if (!code.matches("^[a-zA-Z0-9_-]+$".toRegex())) {
            logger.debug("First path component value wasn't a match.")
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

    private suspend fun fetchApplication(saveToSharedPreferences: Boolean): NotificareApplication =
        withContext(Dispatchers.IO) {
            val application = NotificareRequest.Builder()
                .get("/application/info")
                .responseDecodable(ApplicationResponse::class)
                .application
                .toModel()

            if (saveToSharedPreferences) {
                this@Notificare.application = application
            }

            return@withContext application
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
                        logger.error("Failed to acquire the install referrer.", e)
                        deferredReferrerDetails.complete(null)
                    }
                } else {
                    logger.error(
                        "Unable to acquire the install referrer. Play Store responded with code '$responseCode'."
                    )
                    deferredReferrerDetails.complete(null)
                }

                client.endConnection()
            }

            override fun onInstallReferrerServiceDisconnected() {
                if (!deferredReferrerDetails.isCompleted) {
                    logger.warning("Lost connection to the Play Store before acquiring the install referrer.")
                    deferredReferrerDetails.complete(null)
                }
            }
        })

        return@withContext deferredReferrerDetails.await().also {
            installReferrerDetails = it
        }
    }

    /**
     * Interface definition for a listener to be notified of Notificare SDK state changes.
     *
     * This interface provides callback methods to listen for when the SDK is ready or unlaunched.
     */
    public interface Listener {

        /**
         * Called when the Notificare SDK is fully ready and the application metadata is available.
         *
         * This method is invoked after the SDK has been successfully launched and the [NotificareApplication]
         * is available for use. You can override this method to perform actions after the SDK is initialized.
         *
         * @param application The [NotificareApplication] containing the application's metadata.
         */
        @MainThread
        public fun onReady(application: NotificareApplication) {
        }

        /**
         * Called when the Notificare SDK has been unlaunched.
         *
         * This method is invoked after the SDK has been shut down (unlaunched) and is no longer in use.
         * You can override this method to perform cleanup or handle state changes accordingly.
         */
        @MainThread
        public fun onUnlaunched() {
        }
    }
}
