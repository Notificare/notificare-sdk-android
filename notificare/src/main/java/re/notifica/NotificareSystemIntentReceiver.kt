package re.notifica

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import re.notifica.internal.NotificareLogger
import re.notifica.ktx.deviceImplementation
import java.util.*

internal class NotificareSystemIntentReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent == null) return

        when (intent.action) {
            Intent.ACTION_TIMEZONE_CHANGED -> onTimeZoneChanged()
            Intent.ACTION_LOCALE_CHANGED -> onLocaleChanged()
        }
    }

    private fun onTimeZoneChanged() {
        NotificareLogger.info("Received a time zone change: ${Locale.getDefault().language}-${Locale.getDefault().country}")

        GlobalScope.launch {
            try {
                Notificare.deviceImplementation().updateTimeZone()
                NotificareLogger.debug("Successfully updated device time zone.")
            } catch (e: Exception) {
                NotificareLogger.error("Failed to update device time zone.", e)
            }
        }
    }

    private fun onLocaleChanged() {
        NotificareLogger.info("Received a locale change: ${Locale.getDefault().language}-${Locale.getDefault().country}")

        GlobalScope.launch {
            try {
                Notificare.deviceImplementation().updateLanguage()
                NotificareLogger.debug("Successfully updated device locale.")
            } catch (e: Exception) {
                NotificareLogger.error("Failed to update device locale.", e)
            }
        }
    }
}
