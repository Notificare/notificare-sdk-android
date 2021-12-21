package re.notifica.inbox

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import re.notifica.Notificare
import re.notifica.inbox.internal.database.entities.InboxItemEntity
import re.notifica.inbox.ktx.inboxImplementation
import re.notifica.inbox.models.NotificareInboxItem
import re.notifica.internal.NotificareLogger
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
                    intent.getParcelableExtra(Notificare.INTENT_EXTRA_NOTIFICATION)
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

        val inboxItemVisible = bundle.getBoolean("inboxItemVisible", true)
        val inboxItemExpires = bundle.getLong("inboxItemExpires", -1).let {
            if (it > 0) Date(it) else null
        }

        val item = NotificareInboxItem(
            id = inboxItemId,
            _notification = notification,
            time = Date(),
            opened = false,
            visible = inboxItemVisible,
            expires = inboxItemExpires,
        )

        GlobalScope.launch {
            try {
                NotificareLogger.debug("Adding inbox item to the database.")
                Notificare.inboxImplementation().addItem(item)
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

                val item = entity.toInboxItem().copy(opened = true)

                // Mark the item as read in the local inbox.
                Notificare.inboxImplementation().database.inbox().update(InboxItemEntity.from(item))
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
