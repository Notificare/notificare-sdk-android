package re.notifica.push.ui.notifications.fragments.base

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import re.notifica.Notificare
import re.notifica.models.NotificareNotification

open class NotificationFragment : Fragment() {

    protected lateinit var notification: NotificareNotification
    protected lateinit var callback: Callback

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            callback = parentFragment as Callback
        } catch (e: ClassCastException) {
            throw ClassCastException("Parent fragment must implement NotificationFragment.Callback.")
        }

        notification = savedInstanceState?.getParcelable(Notificare.INTENT_EXTRA_NOTIFICATION)
            ?: arguments?.getParcelable(Notificare.INTENT_EXTRA_NOTIFICATION)
                    ?: throw IllegalArgumentException("Missing required notification parameter.")
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable(Notificare.INTENT_EXTRA_NOTIFICATION, notification)
    }

    interface Callback {
        fun onNotificationFragmentFinished()

        fun onNotificationFragmentShouldShowActionBar()

        fun onNotificationFragmentCanHideActionBar()

        fun onNotificationFragmentCanHideActionsMenu()

        fun onNotificationFragmentStartProgress()

        fun onNotificationFragmentEndProgress()

        fun onNotificationFragmentActionCanceled()

        fun onNotificationFragmentActionFailed(reason: String)

        fun onNotificationFragmentActionSucceeded()

        fun onNotificationFragmentShowActions()

        fun onNotificationFragmentHandleAction(action: NotificareNotification.Action)

        fun onNotificationFragmentStartActivity(intent: Intent)
    }
}
