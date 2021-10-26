package re.notifica.authentication.ktx

import re.notifica.Notificare
import re.notifica.NotificareCallback
import re.notifica.NotificareEventsModule
import re.notifica.internal.ktx.toCallbackFunction

@Suppress("unused")
public suspend fun NotificareEventsModule.logUserLogin() {
    Notificare.eventsInternal().log("re.notifica.event.oauth2.Signin")
}

public fun NotificareEventsModule.logUserLogin(callback: NotificareCallback<Unit>): Unit =
    toCallbackFunction(::logUserLogin)(callback)

@Suppress("unused")
public suspend fun NotificareEventsModule.logUserLogout() {
    Notificare.eventsInternal().log("re.notifica.event.oauth2.Signout")
}

public fun NotificareEventsModule.logUserLogout(callback: NotificareCallback<Unit>): Unit =
    toCallbackFunction(::logUserLogout)(callback)

@Suppress("unused")
public suspend fun NotificareEventsModule.logCreateUserAccount() {
    Notificare.eventsInternal().log("re.notifica.event.oauth2.Signup")
}

public fun NotificareEventsModule.logCreateUserAccount(callback: NotificareCallback<Unit>): Unit =
    toCallbackFunction(::logCreateUserAccount)(callback)

@Suppress("unused")
public suspend fun NotificareEventsModule.logSendPasswordReset() {
    Notificare.eventsInternal().log("re.notifica.event.oauth2.SendPassword")
}

public fun NotificareEventsModule.logSendPasswordReset(callback: NotificareCallback<Unit>): Unit =
    toCallbackFunction(::logSendPasswordReset)(callback)

@Suppress("unused")
public suspend fun NotificareEventsModule.logResetPassword() {
    Notificare.eventsInternal().log("re.notifica.event.oauth2.ResetPassword")
}

public fun NotificareEventsModule.logResetPassword(callback: NotificareCallback<Unit>): Unit =
    toCallbackFunction(::logResetPassword)(callback)

@Suppress("unused")
public suspend fun NotificareEventsModule.logChangePassword() {
    Notificare.eventsInternal().log("re.notifica.event.oauth2.NewPassword")
}

public fun NotificareEventsModule.logChangePassword(callback: NotificareCallback<Unit>): Unit =
    toCallbackFunction(::logChangePassword)(callback)

@Suppress("unused")
public suspend fun NotificareEventsModule.logValidateUser() {
    Notificare.eventsInternal().log("re.notifica.event.oauth2.Validate")
}

public fun NotificareEventsModule.logValidateUser(callback: NotificareCallback<Unit>): Unit =
    toCallbackFunction(::logValidateUser)(callback)

@Suppress("unused")
public suspend fun NotificareEventsModule.logFetchUserDetails() {
    Notificare.eventsInternal().log("re.notifica.event.oauth2.Account")
}

public fun NotificareEventsModule.logFetchUserDetails(callback: NotificareCallback<Unit>): Unit =
    toCallbackFunction(::logFetchUserDetails)(callback)

@Suppress("unused")
public suspend fun NotificareEventsModule.logGeneratePushEmailAddress() {
    Notificare.eventsInternal().log("re.notifica.event.oauth2.AccessToken")
}

public fun NotificareEventsModule.logGeneratePushEmailAddress(callback: NotificareCallback<Unit>): Unit =
    toCallbackFunction(::logGeneratePushEmailAddress)(callback)
