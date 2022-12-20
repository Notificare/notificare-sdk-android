package re.notifica.push.ui.notifications.fragments.base

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import re.notifica.Notificare
import re.notifica.internal.ktx.parcelable
import re.notifica.models.NotificareNotification

public open class NotificationFragment : Fragment() {

    protected lateinit var notification: NotificareNotification
    protected lateinit var callback: Callback

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            callback = parentFragment as Callback
        } catch (e: ClassCastException) {
            throw ClassCastException("Parent fragment must implement NotificationFragment.Callback.")
        }

        notification = savedInstanceState?.parcelable(Notificare.INTENT_EXTRA_NOTIFICATION)
            ?: arguments?.parcelable(Notificare.INTENT_EXTRA_NOTIFICATION)
                ?: throw IllegalArgumentException("Missing required notification parameter.")
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable(Notificare.INTENT_EXTRA_NOTIFICATION, notification)
    }

    public interface Callback {
        public fun onNotificationFragmentFinished()

        public fun onNotificationFragmentShouldShowActionBar()

        public fun onNotificationFragmentCanHideActionBar()

        public fun onNotificationFragmentCanHideActionsMenu()

        public fun onNotificationFragmentStartProgress()

        public fun onNotificationFragmentEndProgress()

        public fun onNotificationFragmentActionCanceled()

        public fun onNotificationFragmentActionFailed(reason: String)

        public fun onNotificationFragmentActionSucceeded()

        public fun onNotificationFragmentShowActions()

        public fun onNotificationFragmentHandleAction(action: NotificareNotification.Action)

        public fun onNotificationFragmentStartActivity(intent: Intent)
    }
}
