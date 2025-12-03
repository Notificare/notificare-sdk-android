package re.notifica.internal

import re.notifica.InternalNotificareApi
import re.notifica.utilities.logging.NotificareLogger

@InternalNotificareApi
public abstract class AbstractServiceManager {

    public abstract val available: Boolean

    public object Factory {

        @PublishedApi
        internal val factoryLogger: NotificareLogger = logger

        public inline fun <reified T : AbstractServiceManager> create(gms: String): T? {
            val implementation: T? = implementation(gms)

            if (implementation != null) {
                factoryLogger.debug("Detected GMS peer dependency. Setting it as the target platform.")
                return implementation
            }

            factoryLogger.warning("No platform dependencies have been detected.")
            return null
        }

        @PublishedApi
        internal inline fun <reified T> implementation(fqn: String): T? {
            return try {
                val klass = Class.forName(fqn)
                klass.getConstructor().newInstance() as? T
            } catch (e: Exception) {
                null
            }
        }
    }
}
