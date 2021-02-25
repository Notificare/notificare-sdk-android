package re.notifica.push.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import re.notifica.NotificareLogger
import re.notifica.models.NotificareNotification
import re.notifica.push.NotificarePush
import re.notifica.push.ui.databinding.NotificareNotificationContainerFragmentBinding
import re.notifica.push.ui.fragments.NotificareAlertFragment
import re.notifica.push.ui.fragments.NotificareUrlFragment
import re.notifica.push.ui.fragments.NotificationFragment

class NotificationContainerFragment
    : Fragment(), NotificationFragment.Callback, NotificationDialog.Callback {

    private lateinit var binding: NotificareNotificationContainerFragmentBinding
    private var notification: NotificareNotification? = null
    private var callback: Callback? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        notification = savedInstanceState?.getParcelable(NotificarePush.INTENT_EXTRA_NOTIFICATION)
            ?: arguments?.getParcelable(NotificarePush.INTENT_EXTRA_NOTIFICATION)

        try {
            callback = activity as Callback
        } catch (e: ClassCastException) {
            throw ClassCastException("Parent activity must implement NotificationContainerFragment.Callback.")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = NotificareNotificationContainerFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val notification = notification ?: return

        // Inform user that this type has actions attached
        // TODO action == null && !notification.getType().equals(NotificareNotification.TYPE_ALERT) && !notification.getType().equals(NotificareNotification.TYPE_PASSBOOK) && notification.getActions().size() > 0
        // setHasOptionsMenu()

        if (savedInstanceState != null) return

        val type = NotificareNotification.NotificationType.from(notification.type)
        val fragmentClassName = when (type) {
            NotificareNotification.NotificationType.ALERT -> NotificareAlertFragment::class.java.canonicalName!!
            NotificareNotification.NotificationType.URL -> NotificareUrlFragment::class.java.canonicalName!!
            else -> NotificareAlertFragment::class.java.canonicalName!!
        }

        val fragment = try {
            val klass = Class.forName(fragmentClassName)
            klass.getConstructor().newInstance() as Fragment
        } catch (e: Exception) {
            NotificareLogger.error(
                "Failed to dynamically create the concrete notification fragment.",
                e
            )

            // Default to an Alert fragment.
            NotificareAlertFragment()
        }

        fragment.arguments = Bundle().apply {
            putParcelable(NotificarePush.INTENT_EXTRA_NOTIFICATION, notification)
        }

        childFragmentManager
            .beginTransaction()
            .add(binding.notificareNotificationFragmentContainer.id, fragment)
            .commit()

        // TODO action == null &&
        if (type == NotificareNotification.NotificationType.ALERT || type == NotificareNotification.NotificationType.PASSBOOK) {
            callback?.onNotificationFragmentCanHideActionBar(notification)
            // TODO keep an instance variable
            NotificationDialog.newInstance(notification)
                .show(childFragmentManager, "dialog")
        } else {
            callback?.onNotificationFragmentShouldShowActionBar(notification)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable(NotificarePush.INTENT_EXTRA_NOTIFICATION, notification)
    }

    // region NotificationFragment.Callback

    override fun onNotificationFragmentFinished() {
        callback?.onNotificationFragmentFinished()
    }

    override fun onNotificationFragmentShouldShowActionBar() {
        val notification = notification ?: return
        callback?.onNotificationFragmentShouldShowActionBar(notification)
    }

    override fun onNotificationFragmentCanHideActionBar() {
        val notification = notification ?: return
        callback?.onNotificationFragmentCanHideActionBar(notification)
    }

    override fun onNotificationFragmentCanHideActionsMenu() {
        // TODO
    }

    override fun onNotificationFragmentStartProgress() {
        val notification = notification ?: return
        callback?.onNotificationFragmentStartProgress(notification)
    }

    override fun onNotificationFragmentEndProgress() {
        val notification = notification ?: return
        callback?.onNotificationFragmentEndProgress(notification)
    }

    override fun onNotificationFragmentActionCanceled() {
        val notification = notification ?: return
        callback?.onNotificationFragmentActionCanceled(notification)
    }

    override fun onNotificationFragmentActionFailed(reason: String) {
        val notification = notification ?: return
        callback?.onNotificationFragmentActionFailed(notification, reason)
    }

    override fun onNotificationFragmentActionSucceeded() {
        val notification = notification ?: return
        callback?.onNotificationFragmentActionSucceeded(notification)
    }

    override fun onNotificationFragmentShowActions() {
        // TODO show alert dialog
    }

    override fun onNotificationFragmentHandleAction(action: NotificareNotification.Action) {
        // TODO handle action
    }

    override fun onNotificationFragmentStartActivity(intent: Intent) {
        try {
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            NotificareLogger.warning("No activity found to handle intent.", e)
        }
    }

    // endregion

    // region NotificationDialog.Callback

    override fun onNotificationDialogOkClick() {
        NotificareLogger.debug("User clicked the OK button.")
    }

    override fun onNotificationDialogCancelClick() {
        NotificareLogger.debug("User clicked the cancel button.")
    }

    override fun onNotificationDialogDismiss() {
        NotificareLogger.debug("User dismissed the dialog.")
        //        if (notification.getType().equals(NotificareNotification.TYPE_ALERT) || notification.getType().equals(NotificareNotification.TYPE_PASSBOOK)) {
//            if (callback != null && pendingResult == null) {

        callback?.onNotificationFragmentFinished()
    }

    override fun onNotificationDialogActionClick(position: Int) {
        NotificareLogger.debug("User clicked on action index $position.")

        val notification = notification ?: return
        if (position >= notification.actions.size) {
            // This is the cancel button
            callback?.onNotificationFragmentFinished()
        } else {
            // Perform the action
            // handleAction(position)
        }
    }

    override fun onNotificationDialogOpenPassbookClick() {
        // TODO
    }

    // endregion

    companion object {
        fun newInstance(notification: NotificareNotification): NotificationContainerFragment {
            return NotificationContainerFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(NotificarePush.INTENT_EXTRA_NOTIFICATION, notification)
                    //putParcelable(NotificarePush.INTENT_EXTRA_ACTION, action)
                }
            }
        }
    }

    interface Callback {
        fun onNotificationFragmentFinished()

        fun onNotificationFragmentShouldShowActionBar(notification: NotificareNotification)

        fun onNotificationFragmentCanHideActionBar(notification: NotificareNotification)

        fun onNotificationFragmentStartProgress(notification: NotificareNotification)

        fun onNotificationFragmentEndProgress(notification: NotificareNotification)

        fun onNotificationFragmentActionCanceled(notification: NotificareNotification)

        fun onNotificationFragmentActionFailed(
            notification: NotificareNotification,
            reason: String?
        )

        fun onNotificationFragmentActionSucceeded(notification: NotificareNotification)
    }
}
