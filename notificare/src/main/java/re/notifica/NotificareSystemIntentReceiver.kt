package re.notifica

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import java.util.Locale
import kotlinx.coroutines.launch
import re.notifica.utilities.logging.NotificareLogger
import re.notifica.ktx.deviceImplementation
import re.notifica.utilities.coroutines.notificareCoroutineScope

internal class NotificareSystemIntentReceiver : BroadcastReceiver() {

    private val logger = NotificareLogger(
        Notificare.options?.debugLoggingEnabled ?: false,
        "NotificareSystemIntentReceiver"
    )

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_TIMEZONE_CHANGED -> onTimeZoneChanged()
            Intent.ACTION_LOCALE_CHANGED -> onLocaleChanged()
        }
    }

    private fun onTimeZoneChanged() {
        logger.info(
            "Received a time zone change: ${Locale.getDefault().language}-${Locale.getDefault().country}"
        )

        notificareCoroutineScope.launch {
            try {
                Notificare.deviceImplementation().updateTimeZone()
                logger.debug("Successfully updated device time zone.")
            } catch (e: Exception) {
                logger.error("Failed to update device time zone.", e)
            }
        }
    }

    private fun onLocaleChanged() {
        logger.info(
            "Received a locale change: ${Locale.getDefault().language}-${Locale.getDefault().country}"
        )

        notificareCoroutineScope.launch {
            try {
                Notificare.deviceImplementation().updateLanguage(
                    language = Notificare.deviceImplementation().getDeviceLanguage(),
                    region = Notificare.deviceImplementation().getDeviceRegion(),
                )

                logger.debug("Successfully updated device locale.")
            } catch (e: Exception) {
                logger.error("Failed to update device locale.", e)
            }
        }
    }
}
