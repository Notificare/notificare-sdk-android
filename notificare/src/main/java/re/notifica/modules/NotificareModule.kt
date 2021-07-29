package re.notifica.modules

import android.content.SharedPreferences
import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
abstract class NotificareModule {

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    open fun migrate(savedState: SharedPreferences, settings: SharedPreferences) {
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    abstract fun configure()

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    abstract suspend fun launch()

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    abstract suspend fun unlaunch()


    internal enum class Module(val fqn: String) {
        PUSH(fqn = "re.notifica.push.NotificarePush"),
        PUSH_UI(fqn = "re.notifica.push.ui.NotificarePushUI"),
        INBOX(fqn = "re.notifica.inbox.NotificareInbox"),
        ASSETS(fqn = "re.notifica.assets.NotificareAssets"),
        SCANNABLES(fqn = "re.notifica.scannables.NotificareScannables"),
        AUTHENTICATION(fqn = "re.notifica.authentication.NotificareAuthentication");

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
