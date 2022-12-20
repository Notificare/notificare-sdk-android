package re.notifica.inbox

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import re.notifica.Notificare
import re.notifica.inbox.ktx.inboxImplementation
import re.notifica.inbox.models.NotificareInboxItem
import re.notifica.internal.NotificareLogger
import re.notifica.internal.ktx.parcelable
import re.notifica.models.NotificareNotification
import java.util.*

internal class NotificareInboxSystemReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            INTENT_ACTION_INBOX_RELOAD -> {
                onReload()
            }
            INTENT_ACTION_INBOX_NOTIFICATION_RECEIVED -> {
                val notification: NotificareNotification = checkNotNull(
                    intent.parcelable(Notificare.INTENT_EXTRA_NOTIFICATION)
                )

                val inboxBundle: Bundle = checkNotNull(
                    intent.getBundleExtra(INTENT_EXTRA_INBOX_NOTIFICATION_RECEIVED_BUNDLE)
                )

                onNotificationReceived(notification, inboxBundle)
            }
            INTENT_ACTION_INBOX_MARK_ITEM_AS_READ -> {
                val inboxItemId = checkNotNull(
                    intent.getStringExtra(INTENT_EXTRA_INBOX_ITEM_ID)
                )

                onMarkItemAsRead(inboxItemId)
            }
        }
    }

    private fun onReload() {
        Notificare.inboxImplementation().refresh()
    }

    private fun onNotificationReceived(notification: NotificareNotification, bundle: Bundle) {
        NotificareLogger.debug("Received a signal to add an item to the inbox.")

        val inboxItemId = bundle.getString("inboxItemId") ?: run {
            NotificareLogger.warning("Cannot create inbox item. Missing inbox item id.")
            return
        }

        val inboxItemTime = bundle.getLong("inboxItemTime", -1)
        if (inboxItemTime <= 0) {
            NotificareLogger.warning("Cannot create inbox item. Invalid time.")
            return
        }

        val inboxItemVisible = bundle.getBoolean("inboxItemVisible", true)
        val inboxItemExpires = bundle.getLong("inboxItemExpires", -1).let {
            if (it > 0) Date(it) else null
        }

        val item = NotificareInboxItem(
            id = inboxItemId,
            notification = notification,
            time = Date(inboxItemTime),
            opened = false,
            expires = inboxItemExpires,
        )

        GlobalScope.launch {
            try {
                NotificareLogger.debug("Adding inbox item to the database.")
                Notificare.inboxImplementation().addItem(item, inboxItemVisible)
            } catch (e: Exception) {
                NotificareLogger.error("Failed to save inbox item to the database.", e)
            }
        }
    }

    // NOTE: we purposely do not use NotificareInbox.markAsRead(item) as that method also logs a notification open,
    // which in this case already happened in the Push module. This is to prevent duplicate events.
    private fun onMarkItemAsRead(id: String) {
        GlobalScope.launch(Dispatchers.IO) {
            try {
                val entity = Notificare.inboxImplementation().database.inbox().findById(id) ?: run {
                    NotificareLogger.warning("Unable to find item '$id' in the local database.")
                    return@launch
                }

                // Mark the item as read in the local inbox.
                entity.opened = true
                Notificare.inboxImplementation().database.inbox().update(entity)
            } catch (e: Exception) {
                NotificareLogger.error("Failed to mark item '$id' as read.", e)
            }
        }
    }

    companion object {
        // Intent actions
        private const val INTENT_ACTION_INBOX_RELOAD = "re.notifica.inbox.intent.action.Reload"
        private const val INTENT_ACTION_INBOX_NOTIFICATION_RECEIVED =
            "re.notifica.inbox.intent.action.NotificationReceived"
        private const val INTENT_ACTION_INBOX_MARK_ITEM_AS_READ = "re.notifica.inbox.intent.action.MarkItemAsRead"

        // Intent extras
        private const val INTENT_EXTRA_INBOX_NOTIFICATION_RECEIVED_BUNDLE = "re.notifica.inbox.intent.extra.InboxBundle"
        private const val INTENT_EXTRA_INBOX_ITEM_ID = "re.notifica.inbox.intent.extra.InboxItemId"
    }
}
