package re.notifica.loyalty

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import re.notifica.Notificare
import re.notifica.internal.NotificareLogger
import re.notifica.internal.ktx.parcelable
import re.notifica.loyalty.ktx.INTENT_ACTION_RELEVANCE_NOTIFICATION_DELETED
import re.notifica.loyalty.ktx.INTENT_EXTRA_PASSBOOK
import re.notifica.loyalty.ktx.loyaltyImplementation
import re.notifica.loyalty.models.NotificarePass

internal class NotificareLoyaltyIntentReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Notificare.INTENT_ACTION_RELEVANCE_NOTIFICATION_DELETED -> {
                val pass: NotificarePass = requireNotNull(
                    intent.parcelable(Notificare.INTENT_EXTRA_PASSBOOK)
                )

                onRelevanceNotificationDeleted(pass)
            }
        }
    }

    private fun onRelevanceNotificationDeleted(pass: NotificarePass) {
        NotificareLogger.debug("Passbook relevance notification removed.")
        Notificare.loyaltyImplementation().handleRelevanceNotificationRemoved(pass)
    }
}
