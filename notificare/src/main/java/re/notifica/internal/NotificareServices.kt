package re.notifica.internal

internal enum class NotificareServices {
    TEST,
    PRODUCTION;

    internal val pushHost: String
        get() = when (this) {
            TEST -> "https://push-test.notifica.re"
            PRODUCTION -> "https://push.notifica.re"
        }

    internal val cloudHost: String
        get() = when (this) {
            TEST -> "https://cloud-test.notifica.re"
            PRODUCTION -> "https://cloud.notifica.re"
        }

    internal val webPassHost: String
        get() = when (this) {
            TEST -> "https://pass-test.notifica.re"
            PRODUCTION -> "https://pass.notifica.re"
        }
}
