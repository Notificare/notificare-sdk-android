package re.notifica.modules.factory

import android.util.Log
import re.notifica.Notificare
import re.notifica.modules.NotificarePushModule

private const val PUSH_MODULE_CLASS = "re.notifica.push.NotificarePushManager"

internal class NotificareModuleFactory(
    private val applicationKey: String,
    private val applicationSecret: String
) {

    fun createPushManager(): NotificarePushModule? {
        val instance = try {
            Class.forName(PUSH_MODULE_CLASS)
                .getConstructor(String::class.java, String::class.java)
                .newInstance(applicationKey, applicationSecret) as? NotificarePushModule
        } catch (e: Exception) {
            null
        }

        if (instance == null) {
            Notificare.logger.debug("Could not load $PUSH_MODULE_CLASS.")
        }

        return instance
    }
}
