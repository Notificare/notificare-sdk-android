package re.notifica

import android.content.Context
import android.content.res.Resources
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import re.notifica.app.NotificareIntentReceiver
import re.notifica.internal.*
import re.notifica.internal.network.push.NotificareBasicAuthenticator
import re.notifica.internal.network.push.NotificareHeadersInterceptor
import re.notifica.internal.network.push.NotificarePushService
import re.notifica.internal.storage.database.NotificareDatabase
import re.notifica.internal.storage.preferences.NotificareSharedPreferences
import re.notifica.models.NotificareApplication
import re.notifica.modules.*
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.lang.ref.WeakReference

object Notificare {

    // Internal modules
    internal val moshi = NotificareUtils.createMoshi()
    internal lateinit var database: NotificareDatabase
        private set
    internal lateinit var sharedPreferences: NotificareSharedPreferences
        private set
    val crashReporter = NotificareCrashReporter()
    internal val sessionManager = NotificareSessionManager()

    // internal var reachability: NotificareReachability? = null
    //     private set
    internal lateinit var pushService: NotificarePushService
        private set

    // Consumer modules
    val eventsManager = NotificareEventsManager()
    val deviceManager = NotificareDeviceManager()

    // Configurations
    private var context: WeakReference<Context>? = null
    private var applicationKey: String? = null
    private var applicationSecret: String? = null

    // Launch / application state
    private var state: NotificareLaunchState = NotificareLaunchState.NONE
    private var application: NotificareApplication? = null

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

        runBlocking(Dispatchers.IO) {
            try {
                val (application) = pushService.fetchApplication()

                deviceManager.launch()
                eventsManager.launch()
                crashReporter.launch()


                Notificare.application = application
                state = NotificareLaunchState.READY

                val enabledServices = application.services.filter { it.value }.map { it.key }

                NotificareLogger.debug("/==================================================================================/")
                NotificareLogger.debug("Notificare SDK is ready to use for application")
                NotificareLogger.debug("App name: ${application.name}")
                NotificareLogger.debug("App ID: ${application.id}")
                NotificareLogger.debug("App services: ${enabledServices.joinToString(", ")}")
                NotificareLogger.debug("/==================================================================================/")
                NotificareLogger.debug("SDK version: ${NotificareDefinitions.SDK_VERSION}")
                NotificareLogger.debug("SDK modules: ${NotificareUtils.availableModules.joinToString(", ")}")
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

    // endregion

    internal fun requireContext(): Context {
        return context?.get() ?: throw IllegalStateException("Cannot find context for Notificare.")
    }

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
        this.applicationKey = applicationKey
        this.applicationSecret = applicationSecret

        // Late init modules
        this.database = NotificareDatabase.create(context.applicationContext)
        this.sharedPreferences = NotificareSharedPreferences(context.applicationContext)

        NotificareLogger.debug("Configuring network services.")
        configureNetworking(applicationKey, applicationSecret, services)

        NotificareLogger.debug("Loading available modules.")
        createAvailableModules(applicationKey, applicationSecret)

        NotificareLogger.debug("Configuring available modules.")
        sessionManager.configure()
        crashReporter.configure()
        // database.configure()
        eventsManager.configure()
        // deviceManager.configure()
        // pushManager?.configure()

        NotificareLogger.debug("Notificare configured for '${services.name}' services.")
        state = NotificareLaunchState.CONFIGURED
    }

    private fun configureNetworking(
        applicationKey: String,
        applicationSecret: String,
        services: NotificareServices
    ) {
        val httpLogger = HttpLoggingInterceptor().apply {
            if (BuildConfig.DEBUG) {
                level = HttpLoggingInterceptor.Level.BASIC
            }
        }

        val httpClient = OkHttpClient.Builder()
            .authenticator(NotificareBasicAuthenticator(applicationKey, applicationSecret))
            .addInterceptor(NotificareHeadersInterceptor())
            .addInterceptor(httpLogger)
            .build()

        pushService = Retrofit.Builder()
            .client(httpClient)
            .baseUrl(services.pushHost)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(NotificarePushService::class.java)
    }

    private fun createAvailableModules(applicationKey: String, applicationSecret: String) {
//        val factory = NotificareModuleFactory(applicationKey, applicationSecret)
//        pushManager = factory.createPushManager()
    }
}
