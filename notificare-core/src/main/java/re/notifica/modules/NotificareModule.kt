package re.notifica.modules

abstract class NotificareModule {

    abstract fun configure()

    abstract suspend fun launch()

    abstract suspend fun unlaunch()
}
