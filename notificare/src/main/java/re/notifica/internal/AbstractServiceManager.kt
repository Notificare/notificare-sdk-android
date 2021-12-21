package re.notifica.internal

import re.notifica.InternalNotificareApi
import re.notifica.Notificare

@InternalNotificareApi
public abstract class AbstractServiceManager {

    public abstract val available: Boolean

    public object Factory {

        @PublishedApi
        internal const val GOOGLE_MOBILE_SERVICES: String = "google"

        @PublishedApi
        internal const val HUAWEI_MOBILE_SERVICES: String = "huawei"

        public inline fun <reified T : AbstractServiceManager> create(gms: String, hms: String): T {
            val preferredMobileServices = checkNotNull(Notificare.options)
                .preferredMobileServices
                ?.lowercase()

            var implementation: T? = when (preferredMobileServices) {
                GOOGLE_MOBILE_SERVICES -> implementation(gms)
                HUAWEI_MOBILE_SERVICES -> implementation(hms)
                else -> null
            }

            if (implementation != null) {
                if (implementation.available) {
                    return implementation
                }

                NotificareLogger.debug("Preferred peer dependency and its mobile services counterpart is not available.")
            }

            if (preferredMobileServices != GOOGLE_MOBILE_SERVICES) {
                implementation = implementation(gms)

                if (implementation != null && implementation.available) {
                    NotificareLogger.debug("Detected GMS peer dependency. Setting it as the target platform.")
                    return implementation
                }
            }

            if (preferredMobileServices != HUAWEI_MOBILE_SERVICES) {
                implementation = implementation(hms)

                if (implementation != null && implementation.available) {
                    NotificareLogger.debug("Detected HMS peer dependency. Setting it as the target platform.")
                    return implementation
                }
            }

            NotificareLogger.warning("No platform dependencies have been detected. Please include one of the platform-specific packages.")
            throw IllegalStateException("No platform dependencies have been detected. Please include one of the platform-specific packages.")
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
