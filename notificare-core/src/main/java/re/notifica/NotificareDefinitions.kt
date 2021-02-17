package re.notifica

import re.notifica.modules.NotificareModule

internal object NotificareDefinitions {
    const val SDK_VERSION = "3.0.0"

    const val SHARED_PREFERENCES_NAME = "re.notifica.preferences.Settings"

    object Intent {
        object Actions {
            const val READY = "re.notifica.intent.action.Ready"
            const val DEVICE_REGISTERED = "re.notifica.intent.action.DeviceRegistered"
        }

        object Extras {
            const val DEVICE = "re.notifica.intent.extra.Device"
        }
    }

    object Events {
        const val APPLICATION_INSTALL = "re.notifica.event.application.Install"
        const val APPLICATION_REGISTRATION = "re.notifica.event.application.Registration"
        const val APPLICATION_UPGRADE = "re.notifica.event.application.Upgrade"
        const val APPLICATION_OPEN = "re.notifica.event.application.Open"
        const val APPLICATION_CLOSE = "re.notifica.event.application.Close"
        const val APPLICATION_EXCEPTION = "re.notifica.event.application.Exception"
    }

    object Preferences {
        const val DEVICE = "re.notifica.preferences.device"
        const val PREFERRED_LANGUAGE = "re.notifica.preferences.preferred_language"
        const val PREFERRED_REGION = "re.notifica.preferences.preferred_region"
        const val CRASH_REPORT = "re.notifica.preferences.crash_report"
    }

    object Tasks {
        const val PROCESS_EVENTS = "re.notifica.tasks.process_events"
    }

    enum class Module(val fqn: String) {
        PUSH(fqn = "re.notifica.push.NotificarePush");

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
