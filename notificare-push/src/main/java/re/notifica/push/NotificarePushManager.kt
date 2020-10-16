package re.notifica.push

import re.notifica.modules.NotificarePushModule

class NotificarePushManager(
    private val applicationKey: String,
    private val applicationSecret: String
) : NotificarePushModule {

    override fun enableRemoteNotifications() {
        TODO("Not yet implemented")
    }
}
