package re.notifica.authentication

import re.notifica.NotificareEventsManager

public fun NotificareEventsManager.logUserLogin(): Unit = log("re.notifica.event.oauth2.Signin")

public fun NotificareEventsManager.logUserLogout(): Unit = log("re.notifica.event.oauth2.Signout")

public fun NotificareEventsManager.logCreateUserAccount(): Unit = log("re.notifica.event.oauth2.Signup")

public fun NotificareEventsManager.logSendPasswordReset(): Unit = log("re.notifica.event.oauth2.SendPassword")

public fun NotificareEventsManager.logResetPassword(): Unit = log("re.notifica.event.oauth2.ResetPassword")

public fun NotificareEventsManager.logChangePassword(): Unit = log("re.notifica.event.oauth2.NewPassword")

public fun NotificareEventsManager.logValidateUser(): Unit = log("re.notifica.event.oauth2.Validate")

public fun NotificareEventsManager.logFetchUserDetails(): Unit = log("re.notifica.event.oauth2.Account")

public fun NotificareEventsManager.logGeneratePushEmailAddress(): Unit = log("re.notifica.event.oauth2.AccessToken")
