package re.notifica.geo.internal.models

internal sealed class NotificareBeaconSupport {
    internal data object Enabled : NotificareBeaconSupport()
    internal data class Disabled(val reason: String) : NotificareBeaconSupport()

    internal val isEnabled: Boolean
        get() = this is Enabled
}
