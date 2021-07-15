package re.notifica.internal

enum class NotificareServices {
    TEST,
    PRODUCTION;

    val pushHost: String
        get() = when (this) {
            TEST -> "https://push-test.notifica.re"
            PRODUCTION -> "https://push.notifica.re"
        }

    val cloudHost: String
        get() = when (this) {
            TEST -> "https://cloud-test.notifica.re"
            PRODUCTION -> "https://cloud.notifica.re"
        }

    val webPassHost: String
        get() = when (this) {
            TEST -> "https://pass-test.notifica.re"
            PRODUCTION -> "https://pass.notifica.re"
        }

    val dynamicLinkDomain: String
        get() = when (this) {
            TEST -> "test.ntc.re"
            PRODUCTION -> "ntc.re"
        }
}
