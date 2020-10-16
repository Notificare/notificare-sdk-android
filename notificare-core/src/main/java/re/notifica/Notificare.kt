package re.notifica

import android.content.Context
import android.util.Log
import com.squareup.moshi.Moshi
import kotlinx.coroutines.*
import okhttp3.*
import okhttp3.logging.HttpLoggingInterceptor
import re.notifica.internal.NotificareLaunchState
import re.notifica.internal.NotificareLogger
import re.notifica.internal.NotificareServices
import re.notifica.internal.network.push.NotificareBasicAuthenticator
import re.notifica.internal.network.push.NotificareHeadersInterceptor
import re.notifica.internal.network.push.NotificarePushService
import re.notifica.models.NotificareApplication
import re.notifica.modules.NotificareCrashReporter
import re.notifica.modules.NotificarePushModule
import re.notifica.modules.NotificareSessionManager
import re.notifica.modules.factory.NotificareModuleFactory
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.lang.ref.WeakReference
import java.util.*

object Notificare {

    // Internal modules
    val logger = NotificareLogger()
    internal val crashReporter = NotificareCrashReporter()
    internal val sessionManager = NotificareSessionManager()

    // internal val database =
    // internal var reachability: NotificareReachability? = null
    //     private set
    internal var pushService: NotificarePushService? = null
        private set

    // Consumer modules
    var pushManager: NotificarePushModule? = null
        private set

    // Configurations
    private var context: WeakReference<Context>? = null
    private var applicationKey: String? = null
    private var applicationSecret: String? = null

    // Launch / application state
    private var state: NotificareLaunchState = NotificareLaunchState.NONE
    private var application: NotificareApplication? = null

    // region Public API

    val isReady: Boolean
        get() = state == NotificareLaunchState.READY

    fun configure(context: Context) {
        val applicationKey = context.getString(R.string.notificare_application_key)
        val applicationSecret = context.getString(R.string.notificare_application_secret)

        configure(context, applicationKey, applicationSecret)
    }

    fun configure(context: Context, applicationKey: String, applicationSecret: String) {
        val services = context.getString(R.string.notificare_services).let {
            try {
                NotificareServices.valueOf(it.toUpperCase(Locale.ROOT))
            } catch (e: IllegalArgumentException) {
                NotificareServices.PRODUCTION
            }
        }

        configure(context, applicationKey, applicationSecret, services)
    }

    fun launch() {
        GlobalScope.launch {
            val application = pushService!!.fetchApplication()
            Log.i("Notificare", application.toString())
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

        logger.debug("Configuring network services.")
        configureNetworking(applicationKey, applicationSecret, services)

        logger.debug("Loading available modules.")
        createAvailableModules(applicationKey, applicationSecret)

        logger.debug("Configuring available modules.")
        // sessionManager.configure()
        // crashReporter.configure()
        // database.configure()
        // eventsManager.configure()
        // deviceManager.configure()
        // pushManager?.configure()

        logger.debug("Notificare configured for '${services.name}' services.")
        state = NotificareLaunchState.CONFIGURED
    }

    private fun configureNetworking(
        applicationKey: String,
        applicationSecret: String,
        services: NotificareServices
    ) {
        val moshi = Moshi.Builder().build()

        val httpLogger = HttpLoggingInterceptor().apply {
            if (BuildConfig.DEBUG) {
                level = HttpLoggingInterceptor.Level.HEADERS
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
        val factory = NotificareModuleFactory(applicationKey, applicationSecret)
        pushManager = factory.createPushManager()
    }
}
