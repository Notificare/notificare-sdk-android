package re.notifica.internal

import android.content.SharedPreferences
import re.notifica.InternalNotificareApi

@InternalNotificareApi
public abstract class NotificareModule {

    public open fun migrate(savedState: SharedPreferences, settings: SharedPreferences) {}

    public open fun configure() {}

    public open suspend fun launch() {}

    public open suspend fun unlaunch() {}


    internal enum class Module(val fqn: String) {
        // Default modules
        DEVICE(fqn = "re.notifica.internal.modules.NotificareDeviceModuleImpl"),
        EVENTS(fqn = "re.notifica.internal.modules.NotificareEventsModuleImpl"),
        SESSION(fqn = "re.notifica.internal.modules.NotificareSessionModuleImpl"),
        CRASH_REPORTER(fqn = "re.notifica.internal.modules.NotificareCrashReporterModuleImpl"),

        // Peer modules
        PUSH(fqn = "re.notifica.push.NotificarePush"),
        PUSH_UI(fqn = "re.notifica.push.ui.NotificarePushUI"),
        INBOX(fqn = "re.notifica.inbox.internal.NotificareInboxImpl"),
        ASSETS(fqn = "re.notifica.assets.internal.NotificareAssetsModuleImpl"),
        SCANNABLES(fqn = "re.notifica.scannables.internal.NotificareScannablesImpl"),
        AUTHENTICATION(fqn = "re.notifica.authentication.NotificareAuthentication"),
        GEO(fqn = "re.notifica.geo.NotificareGeo");

        val isAvailable: Boolean
            get() {
                return try {
                    // Will throw unless the class can be found.
                    Class.forName(fqn)

                    true
                } catch (e: Exception) {
                    false
                }
            }

        val instance: NotificareModule?
            get() {
                return try {
                    // Will throw unless the class can be found.
                    val klass = Class.forName(fqn)

                    return klass.getDeclaredField("INSTANCE").get(null) as? NotificareModule
                } catch (e: Exception) {
                    null
                }
            }
    }

}
