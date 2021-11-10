package re.notifica.loyalty

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import re.notifica.Notificare
import re.notifica.internal.NotificareLogger
import re.notifica.loyalty.ktx.INTENT_EXTRA_PASSBOOK
import re.notifica.loyalty.ktx.loyaltyImplementation
import re.notifica.loyalty.models.NotificarePass

internal class NotificareLoyaltyIntentReceiver : BroadcastReceiver() {
    internal companion object {
        internal const val INTENT_ACTION_RELEVANCE_NOTIFICATION_DELETED =
            "re.notifica.intent.action.RelevanceNotificationDeleted"
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            INTENT_ACTION_RELEVANCE_NOTIFICATION_DELETED -> {
                val pass: NotificarePass = requireNotNull(
                    intent.getParcelableExtra(Notificare.INTENT_EXTRA_PASSBOOK)
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
