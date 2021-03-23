package re.notifica.inbox.internal

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import re.notifica.Notificare
import re.notifica.NotificareLogger
import re.notifica.inbox.NotificareInbox
import re.notifica.inbox.models.NotificareInboxItem
import re.notifica.models.NotificareNotification
import java.util.*

internal class NotificareInboxSystemReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            "re.notifica.inbox.intent.action.NotificationReceived" -> {
                val notification: NotificareNotification = checkNotNull(
                    intent.getParcelableExtra(Notificare.INTENT_EXTRA_NOTIFICATION)
                )

                val inboxBundle: Bundle = checkNotNull(
                    intent.getBundleExtra("re.notifica.inbox.intent.extra.InboxBundle")
                )

                onNotificationReceived(notification, inboxBundle)
            }
        }
    }

    private fun onNotificationReceived(notification: NotificareNotification, bundle: Bundle) {
        NotificareLogger.debug("Received a signal to add an item to the inbox.")

        val inboxItemId = bundle.getString("inboxItemId") ?: run {
            NotificareLogger.warning("Cannot create inbox item. Missing inbox item id.")
            return
        }

        val inboxItemVisible = bundle.getBoolean("inboxItemVisible", true)
        val inboxItemExpires = bundle.getLong("inboxItemExpires", -1).let {
            if (it > 0) Date(it) else null
        }

        val item = NotificareInboxItem(
            id = inboxItemId,
            _notification = notification,
            time = Date(),
            _opened = false,
            visible = inboxItemVisible,
            expires = inboxItemExpires,
        )

        GlobalScope.launch {
            try {
                NotificareLogger.debug("Adding inbox item to the database.")
                NotificareInbox.addItem(item)
            } catch (e: Exception) {
                NotificareLogger.error("Failed to save inbox item to the database.", e)
            }
        }
    }
}
