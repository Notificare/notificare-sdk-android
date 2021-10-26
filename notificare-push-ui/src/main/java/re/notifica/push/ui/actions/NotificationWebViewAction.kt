package re.notifica.push.ui.actions

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import re.notifica.Notificare
import re.notifica.models.NotificareNotification
import re.notifica.push.ui.*
import re.notifica.push.ui.actions.base.NotificationAction
import re.notifica.push.ui.ktx.pushUIInternal
import re.notifica.push.ui.models.NotificarePendingResult

internal class NotificationWebViewAction(
    context: Context,
    notification: NotificareNotification,
    action: NotificareNotification.Action
) : NotificationAction(context, notification, action) {

    override suspend fun execute(): NotificarePendingResult? = withContext(Dispatchers.IO) {
        val uri = action.target?.let { Uri.parse(it) }

        if (uri != null) {
            val intent = Intent(Intent.ACTION_VIEW, uri)

            if (context !is Activity) {
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }

            val colorScheme = when (Notificare.options?.customTabsColorScheme) {
                "light" -> CustomTabsIntent.COLOR_SCHEME_LIGHT
                "dark" -> CustomTabsIntent.COLOR_SCHEME_DARK
                else -> CustomTabsIntent.COLOR_SCHEME_SYSTEM
            }

            val colorSchemeParams = CustomTabColorSchemeParams.Builder()
                .apply {
                    val toolbarColor = Notificare.options?.customTabsToolbarColor
                    if (toolbarColor != null) setToolbarColor(toolbarColor)

                    val navigationBarColor = Notificare.options?.customTabsNavigationBarColor
                    if (navigationBarColor != null) setNavigationBarColor(navigationBarColor)

                    val navigationBarDividerColor = Notificare.options?.customTabsNavigationBarDividerColor
                    if (navigationBarDividerColor != null) setNavigationBarDividerColor(navigationBarDividerColor)
                }
                .build()

            CustomTabsIntent.Builder()
                .setShowTitle(checkNotNull(Notificare.options).customTabsShowTitle)
                .setColorScheme(colorScheme)
                .setColorSchemeParams(CustomTabsIntent.COLOR_SCHEME_LIGHT, colorSchemeParams)
                .setColorSchemeParams(CustomTabsIntent.COLOR_SCHEME_DARK, colorSchemeParams)
                .build()
                .launchUrl(context, uri)

            Notificare.createNotificationReply(notification, action)

            withContext(Dispatchers.Main) {
                Notificare.pushUIInternal().lifecycleListeners.forEach {
                    it.onActionExecuted(notification, action)
                }
            }
        } else {
            throw Exception(context.getString(R.string.notificare_action_failed))
        }

        return@withContext null
    }
}
