package re.notifica.internal

import re.notifica.InternalNotificareApi

@InternalNotificareApi
public abstract class AbstractServiceManager {

    public abstract val available: Boolean

    public object Factory {

        public inline fun <reified T : AbstractServiceManager> create(gms: String): T {
            val implementation: T? = implementation(gms)

            if (implementation != null && implementation.available) {
                NotificareLogger.debug("Detected GMS peer dependency. Setting it as the target platform.")
                return implementation
            }

            NotificareLogger.warning(
                "No platform dependencies have been detected. Please include one of the platform-specific packages."
            )
            throw IllegalStateException(
                "No platform dependencies have been detected. Please include one of the platform-specific packages."
            )
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
