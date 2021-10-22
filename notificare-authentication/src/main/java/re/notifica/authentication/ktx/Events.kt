package re.notifica.authentication.ktx

import re.notifica.Notificare
import re.notifica.NotificareEventsModule

@Suppress("unused")
public fun NotificareEventsModule.logUserLogin(): Unit =
    Notificare.eventsInternal().log("re.notifica.event.oauth2.Signin")

@Suppress("unused")
public fun NotificareEventsModule.logUserLogout(): Unit =
    Notificare.eventsInternal().log("re.notifica.event.oauth2.Signout")

@Suppress("unused")
public fun NotificareEventsModule.logCreateUserAccount(): Unit =
    Notificare.eventsInternal().log("re.notifica.event.oauth2.Signup")

@Suppress("unused")
public fun NotificareEventsModule.logSendPasswordReset(): Unit =
    Notificare.eventsInternal().log("re.notifica.event.oauth2.SendPassword")

@Suppress("unused")
public fun NotificareEventsModule.logResetPassword(): Unit =
    Notificare.eventsInternal().log("re.notifica.event.oauth2.ResetPassword")

@Suppress("unused")
public fun NotificareEventsModule.logChangePassword(): Unit =
    Notificare.eventsInternal().log("re.notifica.event.oauth2.NewPassword")

@Suppress("unused")
public fun NotificareEventsModule.logValidateUser(): Unit =
    Notificare.eventsInternal().log("re.notifica.event.oauth2.Validate")

@Suppress("unused")
public fun NotificareEventsModule.logFetchUserDetails(): Unit =
    Notificare.eventsInternal().log("re.notifica.event.oauth2.Account")

@Suppress("unused")
public fun NotificareEventsModule.logGeneratePushEmailAddress(): Unit =
    Notificare.eventsInternal().log("re.notifica.event.oauth2.AccessToken")
