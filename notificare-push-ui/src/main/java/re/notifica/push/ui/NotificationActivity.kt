package re.notifica.push.ui

import android.os.Build
import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import re.notifica.Notificare
import re.notifica.utilities.threading.onMainThread
import re.notifica.utilities.parcel.parcelable
import re.notifica.models.NotificareNotification
import re.notifica.push.ui.databinding.NotificareNotificationActivityBinding
import re.notifica.push.ui.ktx.pushUIImplementation
import re.notifica.push.ui.notifications.NotificationContainerFragment

public open class NotificationActivity : AppCompatActivity(), NotificationContainerFragment.Callback {

    private lateinit var binding: NotificareNotificationActivityBinding
    private lateinit var notification: NotificareNotification
    private var action: NotificareNotification.Action? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        notification = savedInstanceState?.parcelable(Notificare.INTENT_EXTRA_NOTIFICATION)
            ?: intent.parcelable(Notificare.INTENT_EXTRA_NOTIFICATION)
            ?: throw IllegalArgumentException("Missing required notification parameter.")

        action = savedInstanceState?.parcelable(Notificare.INTENT_EXTRA_ACTION)
            ?: intent.parcelable(Notificare.INTENT_EXTRA_ACTION)

        super.onCreate(savedInstanceState)
        binding = NotificareNotificationActivityBinding.inflate(layoutInflater).also {
            setContentView(it.root)
        }

        if (savedInstanceState != null) return

        supportActionBar?.hide()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

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
            onMainThread {
                Notificare.pushUIImplementation().lifecycleListeners.forEach {
                    it.get()?.onNotificationFinishedPresenting(notification)
                }
            }

            onBackPressedDispatcher.onBackPressed()
            return true
        }

        return super.onOptionsItemSelected(item)
    }

    override fun finish() {
        super.finish()

        onMainThread {
            Notificare.pushUIImplementation().lifecycleListeners.forEach {
                it.get()?.onNotificationFinishedPresenting(notification)
            }
        }
    }

    // region NotificationContainerFragment.Callback

    override fun onNotificationFragmentFinished() {
        finish()

        if (supportActionBar == null || supportActionBar?.isShowing == false) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                overrideActivityTransition(OVERRIDE_TRANSITION_CLOSE, 0, 0)
            } else {
                @Suppress("DEPRECATION")
                overridePendingTransition(0, 0)
            }
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

    override fun onNotificationFragmentActionFailed(notification: NotificareNotification, reason: String?) {
        binding.notificareProgress.isVisible = false

        if (checkNotNull(Notificare.options).showNotificationToasts) {
            Toast.makeText(this, reason, Toast.LENGTH_LONG).show()
        }
    }

    override fun onNotificationFragmentActionSucceeded(notification: NotificareNotification) {
        binding.notificareProgress.isVisible = false

        if (checkNotNull(Notificare.options).showNotificationToasts) {
            Toast.makeText(this, R.string.notificare_action_success, Toast.LENGTH_SHORT).show()
        }
    }

    // endregion
}
