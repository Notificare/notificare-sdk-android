package re.notifica

internal object NotificareDefinitions {
    const val SDK_VERSION = "3.0.0"

    const val SHARED_PREFERENCES_NAME = "re.notifica.preferences.Settings"

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
    }

    object Tasks {
        const val PROCESS_EVENTS = "re.notifica.tasks.process_events"
    }
}
