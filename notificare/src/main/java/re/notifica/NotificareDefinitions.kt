package re.notifica

import re.notifica.modules.NotificareModule

internal object NotificareDefinitions {
    object Events {
        const val APPLICATION_INSTALL = "re.notifica.event.application.Install"
        const val APPLICATION_REGISTRATION = "re.notifica.event.application.Registration"
        const val APPLICATION_UPGRADE = "re.notifica.event.application.Upgrade"
        const val APPLICATION_OPEN = "re.notifica.event.application.Open"
        const val APPLICATION_CLOSE = "re.notifica.event.application.Close"
        const val APPLICATION_EXCEPTION = "re.notifica.event.application.Exception"
    }

    object Tasks {
        const val PROCESS_EVENTS = "re.notifica.tasks.process_events"
    }

    enum class Module(val fqn: String) {
        PUSH(fqn = "re.notifica.push.NotificarePush"),
        PUSH_UI(fqn = "re.notifica.push.ui.NotificarePushUI"),
        INBOX(fqn = "re.notifica.inbox.NotificareInbox");

        val isAvailable: Boolean
            get() {
                return try {
                    // Will throw unless the class can be found.
                    Class.forName(fqn)

                    true
                } catch (e: Exception) {
                    false
                }
            }

        val instance: NotificareModule?
            get() {
                return try {
                    // Will throw unless the class can be found.
                    val klass = Class.forName(fqn)

                    return klass.getDeclaredField("INSTANCE").get(null) as? NotificareModule
                } catch (e: Exception) {
                    null
                }
            }
    }
}
