package re.notifica.push.ui.notifications

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import re.notifica.utilities.parcel.parcelable
import re.notifica.models.NotificareNotification
import re.notifica.push.ui.R
import re.notifica.push.ui.internal.logger

public class NotificationActionsDialog : DialogFragment() {

    private lateinit var callback: Callback
    private var notification: NotificareNotification? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState != null) {
            notification = savedInstanceState.parcelable(SAVED_STATE_NOTIFICATION)
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val notification = notification ?: run {
            logger.warning("Notification action dialog created without a notification.")
            return super.onCreateDialog(savedInstanceState)
        }

        try {
            callback = parentFragment as Callback
        } catch (e: Exception) {
            throw ClassCastException("Parent fragment must implement NotificationActionsDialog.Callback.")
        }

        val builder = AlertDialog.Builder(requireContext())

        if (notification.actions.isEmpty()) {
            builder.setMessage(R.string.notificare_action_error_no_actions)
        } else {
            val labels = notification.actions
                .map { it.getLocalizedLabel(requireContext()) }
                .toTypedArray()

            builder.setItems(labels) { _, which ->
                callback.onActionDialogActionClick(which)
            }
        }

        builder.setNeutralButton(R.string.notificare_dialog_cancel_button) { _, _ ->
            callback.onActionDialogCancelClick()
        }

        builder.setNegativeButton(R.string.notificare_dialog_close_button) { _, _ ->
            callback.onActionDialogCloseClick()
        }

        return builder.create()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable(SAVED_STATE_NOTIFICATION, notification)
    }

    public companion object {
        private const val SAVED_STATE_NOTIFICATION = "re.notifica.ui.Notification"

        public fun newInstance(notification: NotificareNotification): NotificationActionsDialog {
            return NotificationActionsDialog().apply {
                this.notification = notification
            }
        }
    }

    public interface Callback {
        public fun onActionDialogCloseClick()

        public fun onActionDialogCancelClick()

        public fun onActionDialogActionClick(which: Int)
    }
}
