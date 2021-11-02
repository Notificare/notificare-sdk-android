package re.notifica

public data class NotificareServicesInfo(
    val applicationKey: String,
    val applicationSecret: String,
    internal val environment: Environment = Environment.PRODUCTION,
) {

    public val pushHost: String
        get() = when (environment) {
            Environment.TEST -> "https://push-test.notifica.re"
            Environment.PRODUCTION -> "https://push.notifica.re"
        }

    public val cloudHost: String
        get() = when (environment) {
            Environment.TEST -> "https://cloud-test.notifica.re"
            Environment.PRODUCTION -> "https://cloud.notifica.re"
        }

    public val dynamicLinkDomain: String
        get() = when (environment) {
            Environment.TEST -> "test.ntc.re"
            Environment.PRODUCTION -> "ntc.re"
        }

    public val appLinksDomain: String
        get() = when (environment) {
            Environment.TEST -> "applinks-test.notifica.re"
            Environment.PRODUCTION -> "applinks.notifica.re"
        }

    public enum class Environment {
        TEST,
        PRODUCTION;
    }
}
