package re.notifica.push.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import re.notifica.models.NotificareNotification
import re.notifica.push.NotificarePush
import re.notifica.push.ui.databinding.NotificareNotificationActivityBinding

class NotificationActivity : AppCompatActivity(), NotificationContainerFragment.Callback {

    private lateinit var binding: NotificareNotificationActivityBinding
    private lateinit var notification: NotificareNotification

    override fun onCreate(savedInstanceState: Bundle?) {
        notification = savedInstanceState?.getParcelable(NotificarePush.INTENT_EXTRA_NOTIFICATION)
            ?: intent.getParcelableExtra(NotificarePush.INTENT_EXTRA_NOTIFICATION)
                    ?: throw IllegalArgumentException("Missing required notification parameter.")

        super.onCreate(savedInstanceState)
        binding = NotificareNotificationActivityBinding.inflate(layoutInflater).also {
            setContentView(it.root)
        }

        supportActionBar?.hide()

        val fragment = NotificationContainerFragment.newInstance(notification)
        supportFragmentManager
            .beginTransaction()
            .add(binding.notificareNotificationContainer.id, fragment, "notification_container")
            .commit()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable(NotificarePush.INTENT_EXTRA_NOTIFICATION, notification)
    }

    // region NotificationContainerFragment.Callback

    override fun onNotificationFragmentFinished() {
        finish()

        if (supportActionBar == null || supportActionBar?.isShowing == false) {
            overridePendingTransition(0, 0)
        }
    }

    override fun onNotificationFragmentShouldShowActionBar(notification: NotificareNotification) {
        notification.title?.run {
            supportActionBar?.title = this
        }
        supportActionBar?.show()
    }

    override fun onNotificationFragmentCanHideActionBar(notification: NotificareNotification) {
        supportActionBar?.hide()
    }

    override fun onNotificationFragmentStartProgress(notification: NotificareNotification) {
        // TODO if (Notificare.shared().getNotificationActivityShowProgress()) {
        binding.notificareProgress.isVisible = true
    }

    override fun onNotificationFragmentEndProgress(notification: NotificareNotification) {
        binding.notificareProgress.isVisible = false
    }

    override fun onNotificationFragmentActionCanceled(notification: NotificareNotification) {
        binding.notificareProgress.isVisible = false

//        if (Notificare.shared().getNotificationActivityShowToasts()) {
//            Toast.makeText(this, R.string.notificare_action_canceled, Toast.LENGTH_LONG).show()
//        }
    }

    override fun onNotificationFragmentActionFailed(
        notification: NotificareNotification,
        reason: String?
    ) {
        binding.notificareProgress.isVisible = false
    }

    override fun onNotificationFragmentActionSucceeded(notification: NotificareNotification) {
        binding.notificareProgress.isVisible = false
    }

    // endregion
}
