package re.notifica.modules

abstract class NotificareModule<LaunchResult> {

    internal abstract fun configure()

    internal abstract suspend fun launch(): LaunchResult
}
