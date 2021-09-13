package re.notifica.authentication

import re.notifica.NotificareEventsManager

fun NotificareEventsManager.logUserLogin() = log("re.notifica.event.oauth2.Signin")

fun NotificareEventsManager.logUserLogout() = log("re.notifica.event.oauth2.Signout")

fun NotificareEventsManager.logCreateUserAccount() = log("re.notifica.event.oauth2.Signup")

fun NotificareEventsManager.logSendPasswordReset() = log("re.notifica.event.oauth2.SendPassword")

fun NotificareEventsManager.logResetPassword() = log("re.notifica.event.oauth2.ResetPassword")

fun NotificareEventsManager.logChangePassword() = log("re.notifica.event.oauth2.NewPassword")

fun NotificareEventsManager.logValidateUser() = log("re.notifica.event.oauth2.Validate")

fun NotificareEventsManager.logFetchUserDetails() = log("re.notifica.event.oauth2.Account")

fun NotificareEventsManager.logGeneratePushEmailAddress() = log("re.notifica.event.oauth2.AccessToken")
