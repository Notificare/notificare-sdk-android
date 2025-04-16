package re.notifica.internal

import android.content.SharedPreferences
import com.squareup.moshi.Moshi
import re.notifica.InternalNotificareApi

@InternalNotificareApi
public abstract class NotificareModule {

    public open fun moshi(builder: Moshi.Builder) {}

    public open fun migrate(savedState: SharedPreferences, settings: SharedPreferences) {}

    public open fun configure() {}

    public open suspend fun clearStorage() {}

    public open suspend fun launch() {}

    public open suspend fun postLaunch() {}

    public open suspend fun unlaunch() {}

    @InternalNotificareApi
    public enum class Module(private val fqn: String) {
        // Default modules
        DEVICE(fqn = "re.notifica.internal.modules.NotificareDeviceModuleImpl"),
        SESSION(fqn = "re.notifica.internal.modules.NotificareSessionModuleImpl"),
        EVENTS(fqn = "re.notifica.internal.modules.NotificareEventsModuleImpl"),
        CRASH_REPORTER(fqn = "re.notifica.internal.modules.NotificareCrashReporterModuleImpl"),

        // Peer modules
        PUSH(fqn = "re.notifica.push.internal.NotificarePushImpl"),
        PUSH_UI(fqn = "re.notifica.push.ui.internal.NotificarePushUIImpl"),
        INBOX(fqn = "re.notifica.inbox.internal.NotificareInboxImpl"),
        ASSETS(fqn = "re.notifica.assets.internal.NotificareAssetsImpl"),
        SCANNABLES(fqn = "re.notifica.scannables.internal.NotificareScannablesImpl"),
        GEO(fqn = "re.notifica.geo.internal.NotificareGeoImpl"),
        LOYALTY(fqn = "re.notifica.loyalty.internal.NotificareLoyaltyImpl"),
        IN_APP_MESSAGING(fqn = "re.notifica.iam.internal.NotificareInAppMessagingImpl"),
        USER_INBOX(fqn = "re.notifica.inbox.user.internal.NotificareUserInboxImpl");

        @InternalNotificareApi
        public val isAvailable: Boolean
            get() {
                return try {
                    // Will throw unless the class can be found.
                    Class.forName(fqn)

                    true
                } catch (e: Exception) {
                    false
                }
            }

        @InternalNotificareApi
        public val instance: NotificareModule?
            get() {
                return try {
                    // Will throw unless the class can be found.
                    val klass = Class.forName(fqn)

                    return klass.getDeclaredField("INSTANCE").get(null) as? NotificareModule
                } catch (e: Exception) {
                    null
                }
            }

        internal val isPeer: Boolean
            get() {
                return when (this) {
                    DEVICE, EVENTS, SESSION, CRASH_REPORTER -> false
                    else -> true
                }
            }
    }
}
