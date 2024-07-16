package re.notifica

public data class NotificareServicesInfo @JvmOverloads constructor(
    val applicationKey: String,
    val applicationSecret: String,
    val hosts: Hosts = Hosts(),
) {

    internal val hasDefaultHosts: Boolean
        get() = hosts.restApi == DEFAULT_REST_API_HOST &&
            hosts.appLinks == DEFAULT_APP_LINKS_HOST &&
            hosts.shortLinks == DEFAULT_SHORT_LINKS_HOST

    internal fun validate() {
        check(HOST_REGEX.matches(hosts.restApi)) {
            "Invalid REST API host."
        }

        check(HOST_REGEX.matches(hosts.appLinks)) {
            "Invalid AppLinks host."
        }

        check(HOST_REGEX.matches(hosts.shortLinks)) {
            "Invalid short links host."
        }
    }

    public data class Hosts(
        val restApi: String,
        val appLinks: String,
        val shortLinks: String,
    ) {

        public constructor() : this(
            restApi = DEFAULT_REST_API_HOST,
            appLinks = DEFAULT_APP_LINKS_HOST,
            shortLinks = DEFAULT_SHORT_LINKS_HOST,
        )
    }

    public companion object {
        private const val DEFAULT_REST_API_HOST = "push.notifica.re"
        private const val DEFAULT_SHORT_LINKS_HOST = "ntc.re"
        private const val DEFAULT_APP_LINKS_HOST = "applinks.notifica.re"

        @Suppress("detekt:MaxLineLength")
        private val HOST_REGEX = Regex(
            "^(([a-zA-Z0-9]|[a-zA-Z0-9][a-zA-Z0-9\\-]*[a-zA-Z0-9])\\.)*([a-zA-Z0-9]|[a-zA-Z0-9][a-zA-Z0-9\\-]*[a-zA-Z0-9])$"
        )
    }
}
