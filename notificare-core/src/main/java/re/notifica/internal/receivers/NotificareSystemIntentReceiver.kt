package re.notifica.internal.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.runBlocking
import re.notifica.Notificare
import java.util.*

class NotificareSystemIntentReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent == null) return

        when (intent.action) {
            Intent.ACTION_TIMEZONE_CHANGED -> onTimeZoneChanged()
            Intent.ACTION_LOCALE_CHANGED -> onLocaleChanged()
        }
    }

    private fun onTimeZoneChanged() {
        Notificare.logger.info("Received a time zone change: ${Locale.getDefault().language}-${Locale.getDefault().country}")

        runBlocking {
            try {
                Notificare.deviceManager.updateTimeZone()
                Notificare.logger.debug("Successfully updated device time zone.")
            } catch (e: Exception) {
                Notificare.logger.error("Failed to update device time zone.", e)
            }
        }
    }

    private fun onLocaleChanged() {
        Notificare.logger.info("Received a locale change: ${Locale.getDefault().language}-${Locale.getDefault().country}")

        runBlocking {
            try {
                Notificare.deviceManager.updateLanguage()
                Notificare.logger.debug("Successfully updated device locale.")
            } catch (e: Exception) {
                Notificare.logger.error("Failed to update device locale.", e)
            }
        }
    }
}
