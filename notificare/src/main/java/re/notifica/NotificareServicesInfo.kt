package re.notifica

public data class NotificareServicesInfo(
    val applicationKey: String,
    val applicationSecret: String,
    val environment: Environment,
) {

    public enum class Environment {
        TEST,
        PRODUCTION;

        public val pushHost: String
            get() = when (this) {
                TEST -> "https://push-test.notifica.re"
                PRODUCTION -> "https://push.notifica.re"
            }

        public val cloudHost: String
            get() = when (this) {
                TEST -> "https://cloud-test.notifica.re"
                PRODUCTION -> "https://cloud.notifica.re"
            }

        public val dynamicLinkDomain: String
            get() = when (this) {
                TEST -> "test.ntc.re"
                PRODUCTION -> "ntc.re"
            }
    }
}
