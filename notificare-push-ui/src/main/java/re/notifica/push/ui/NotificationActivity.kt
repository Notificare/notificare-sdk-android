package re.notifica.push.ui

import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import re.notifica.Notificare
import re.notifica.models.NotificareNotification
import re.notifica.push.ui.databinding.NotificareNotificationActivityBinding
import re.notifica.push.ui.notifications.NotificationContainerFragment

open class NotificationActivity : AppCompatActivity(), NotificationContainerFragment.Callback {

    private lateinit var binding: NotificareNotificationActivityBinding
    private lateinit var notification: NotificareNotification
    private var action: NotificareNotification.Action? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        notification = savedInstanceState?.getParcelable(Notificare.INTENT_EXTRA_NOTIFICATION)
            ?: intent.getParcelableExtra(Notificare.INTENT_EXTRA_NOTIFICATION)
                ?: throw IllegalArgumentException("Missing required notification parameter.")

        action = savedInstanceState?.getParcelable(Notificare.INTENT_EXTRA_ACTION)
            ?: intent.getParcelableExtra(Notificare.INTENT_EXTRA_ACTION)

        super.onCreate(savedInstanceState)
        binding = NotificareNotificationActivityBinding.inflate(layoutInflater).also {
            setContentView(it.root)
        }

        supportActionBar?.hide()

        val fragment = NotificationContainerFragment.newInstance(notification, action)
        supportFragmentManager
            .beginTransaction()
            .add(binding.notificareNotificationContainer.id, fragment, "notification_container")
            .commit()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable(Notificare.INTENT_EXTRA_NOTIFICATION, notification)
        outState.putParcelable(Notificare.INTENT_EXTRA_ACTION, action)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            NotificarePushUI.lifecycleListeners.forEach { it.onNotificationFinishedPresenting(notification) }
        }

        return super.onOptionsItemSelected(item)
    }

    override fun finish() {
        super.finish()

        NotificarePushUI.lifecycleListeners.forEach { it.onNotificationFinishedPresenting(notification) }
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
        if (checkNotNull(Notificare.options).showNotificationProgress) {
            binding.notificareProgress.isVisible = true
        }
    }

    override fun onNotificationFragmentEndProgress(notification: NotificareNotification) {
        binding.notificareProgress.isVisible = false
    }

    override fun onNotificationFragmentActionCanceled(notification: NotificareNotification) {
        binding.notificareProgress.isVisible = false

        if (checkNotNull(Notificare.options).showNotificationToasts) {
            Toast.makeText(this, R.string.notificare_action_canceled, Toast.LENGTH_LONG).show()
        }
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
