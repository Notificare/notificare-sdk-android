package re.notifica.push.ui

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.ArrayAdapter
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import re.notifica.NotificareLogger
import re.notifica.internal.NotificareUtils
import re.notifica.models.NotificareNotification
import re.notifica.push.ui.databinding.NotificareAlertDialogBinding

class NotificationDialog : DialogFragment() {

    private var callback: Callback? = null
    private var notification: NotificareNotification? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState != null) {
            notification = savedInstanceState.getParcelable(SAVED_STATE_NOTIFICATION)
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val notification = notification ?: run {
            NotificareLogger.warning("Notification dialog created without a notification.")
            return super.onCreateDialog(savedInstanceState)
        }

        try {
            callback = parentFragment as Callback
        } catch (e: Exception) {
            throw ClassCastException("Parent fragment must implement NotificationDialog.Callback.")
        }

        val builder = AlertDialog.Builder(requireContext())

        val icon = context?.applicationInfo?.icon
        if (icon != null) builder.setIcon(icon)

        builder.setTitle(notification.title ?: NotificareUtils.applicationName)

        val type = NotificareNotification.NotificationType.from(notification.type)
        if (type == NotificareNotification.NotificationType.ALERT && notification.actions.isNotEmpty()) {
            val binding = NotificareAlertDialogBinding.inflate(LayoutInflater.from(context))

            binding.message.text = notification.message

            binding.list.adapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_list_item_1,
                notification.actions.map {
                    // TODO get localised text
                    it.label
                }
            )

            binding.list.setOnItemClickListener { _, _, position, _ ->
                callback?.onNotificationDialogActionClick(
                    position
                )
            }

            builder.setView(binding.root)
            builder.setNeutralButton(R.string.notificare_dialog_cancel_button) { _, _ -> callback?.onNotificationDialogCancelClick() }
        } else if (type == NotificareNotification.NotificationType.PASSBOOK) {
            builder.setMessage(notification.message)
            builder.setNeutralButton(R.string.notificare_dialog_open_pass_button) { _, _ -> callback?.onNotificationDialogOpenPassbookClick() }
        } else {
            builder.setMessage(notification.message)
            builder.setNeutralButton(R.string.notificare_dialog_ok_button) { _, _ -> callback?.onNotificationDialogOkClick() }
        }

        return builder.create()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable(SAVED_STATE_NOTIFICATION, notification)
    }

    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
        callback?.onNotificationDialogCancelClick()
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        callback?.onNotificationDialogDismiss()
    }

    companion object {
        private const val SAVED_STATE_NOTIFICATION = "re.notifica.ui.Notification"

        fun newInstance(notification: NotificareNotification): NotificationDialog {
            return NotificationDialog().apply {
                this.notification = notification
            }
        }
    }

    interface Callback {
        fun onNotificationDialogOkClick()

        fun onNotificationDialogCancelClick()

        fun onNotificationDialogDismiss()

        fun onNotificationDialogActionClick(position: Int)

        fun onNotificationDialogOpenPassbookClick()
    }
}