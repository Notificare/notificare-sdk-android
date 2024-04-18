package re.notifica.sample

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.IntentCompat
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.elevation.SurfaceColors
import com.google.android.material.snackbar.Snackbar
import re.notifica.Notificare
import re.notifica.geo.ktx.INTENT_ACTION_BEACON_NOTIFICATION_OPENED
import re.notifica.iam.NotificareInAppMessaging
import re.notifica.iam.ktx.inAppMessaging
import re.notifica.iam.models.NotificareInAppMessage
import re.notifica.models.NotificareNotification
import re.notifica.monetize.NotificareMonetize
import re.notifica.push.ktx.INTENT_ACTION_ACTION_OPENED
import re.notifica.push.ktx.INTENT_ACTION_NOTIFICATION_OPENED
import re.notifica.push.ktx.push
import re.notifica.push.ui.NotificarePushUI
import re.notifica.push.ui.ktx.pushUI
import re.notifica.sample.databinding.ActivitySampleBinding
import timber.log.Timber

class SampleActivity :
    AppCompatActivity(),
    NotificarePushUI.NotificationLifecycleListener,
    NotificareMonetize.Listener {

    private lateinit var binding: ActivitySampleBinding

    private val navController: NavController
        get() {
            // Access the nested NavController.
            // Using findNavController will yield a reference to the parent's NavController.
            val fragmentContainer = binding.root.findViewById<View>(R.id.nav_host_fragment)
            return Navigation.findNavController(fragmentContainer)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySampleBinding.inflate(layoutInflater).also {
            setContentView(it.root)
        }

        window.navigationBarColor = SurfaceColors.SURFACE_2.getColor(this)
        this.setSupportActionBar(binding.toolbar)
        binding.toolbar.setupWithNavController(navController)

        if (intent != null) handleIntent(intent)

        Notificare.pushUI().addLifecycleListener(this)
        Notificare.inAppMessaging().addLifecycleListener(messageLifecycleListener)
    }

    override fun onDestroy() {
        super.onDestroy()

        Notificare.pushUI().removeLifecycleListener(this)
        Notificare.inAppMessaging().removeLifecycleListener(messageLifecycleListener)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)

        if (intent != null) handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        if (Notificare.push().handleTrampolineIntent(intent)) return
        if (Notificare.handleTestDeviceIntent(intent)) return
        if (Notificare.handleDynamicLinkIntent(this, intent)) return

        when (intent.action) {
            Notificare.INTENT_ACTION_NOTIFICATION_OPENED -> {
                val notification = requireNotNull(
                    IntentCompat.getParcelableExtra(
                        intent,
                        Notificare.INTENT_EXTRA_NOTIFICATION,
                        NotificareNotification::class.java
                    )
                )

                Notificare.pushUI().presentNotification(this, notification)
                return
            }

            Notificare.INTENT_ACTION_ACTION_OPENED -> {
                val notification = requireNotNull(
                    IntentCompat.getParcelableExtra(
                        intent,
                        Notificare.INTENT_EXTRA_NOTIFICATION,
                        NotificareNotification::class.java
                    )
                )

                val action = requireNotNull(
                    IntentCompat.getParcelableExtra(
                        intent,
                        Notificare.INTENT_EXTRA_ACTION,
                        NotificareNotification.Action::class.java
                    )
                )

                Notificare.pushUI().presentAction(this, notification, action)
                return
            }

            Notificare.INTENT_ACTION_BEACON_NOTIFICATION_OPENED -> {
                Snackbar.make(binding.root, "Beacon notification opened.", Snackbar.LENGTH_SHORT).show()
                return
            }
        }

        val uri = intent.data ?: return
        Timber.i("Received deep link with uri = $uri")
        Toast.makeText(this, "Deep link = $uri", Toast.LENGTH_SHORT).show()
    }

    // Lifecycle Listeners
    private val messageLifecycleListener = object : NotificareInAppMessaging.MessageLifecycleListener {
        override fun onMessagePresented(message: NotificareInAppMessage) {
            Timber.i("---> message presented '$message.name'")
            Toast.makeText(this@SampleActivity, "Message presented", Toast.LENGTH_SHORT).show()
        }

        override fun onMessageFinishedPresenting(message: NotificareInAppMessage) {
            Timber.i("---> message finished presenting '$message.name'")
            Toast.makeText(this@SampleActivity, "Message finished presenting", Toast.LENGTH_SHORT).show()
        }

        override fun onMessageFailedToPresent(message: NotificareInAppMessage) {
            Timber.i("---> message failed to present '$message.name'")
            Toast.makeText(this@SampleActivity, "Message failed to present", Toast.LENGTH_SHORT).show()
        }

        override fun onActionExecuted(message: NotificareInAppMessage, action: NotificareInAppMessage.Action) {
            Timber.i("---> action executed '$message.name'")
            Toast.makeText(this@SampleActivity, "Action executed", Toast.LENGTH_SHORT).show()
        }

        override fun onActionFailedToExecute(
            message: NotificareInAppMessage,
            action: NotificareInAppMessage.Action,
            error: Exception?
        ) {
            Timber.i(error, "---> action failed to execute '$message.name'")
            Toast.makeText(this@SampleActivity, "Action failed to execute", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onNotificationWillPresent(notification: NotificareNotification) {
        Timber.i("---> notification will present '$notification.id'")
        Toast.makeText(this, "Notification will present", Toast.LENGTH_SHORT).show()
    }

    override fun onNotificationPresented(notification: NotificareNotification) {
        Timber.i("---> notification presented '$notification.id'")
        Toast.makeText(this, "Notification presented", Toast.LENGTH_SHORT).show()
    }

    override fun onNotificationFinishedPresenting(notification: NotificareNotification) {
        Timber.i("---> notification finished presenting '$notification.id'")
        Toast.makeText(this, "Notification finished presenting", Toast.LENGTH_SHORT).show()
    }

    override fun onNotificationFailedToPresent(notification: NotificareNotification) {
        Timber.i("---> notification failed to present '$notification.id'")
        Toast.makeText(this, "Notification failed to present", Toast.LENGTH_SHORT).show()
    }

    override fun onNotificationUrlClicked(notification: NotificareNotification, uri: Uri) {
        Timber.i("---> notification url clicked '$notification.id'")
        Toast.makeText(this, "Notification URL clicked", Toast.LENGTH_SHORT).show()
    }

    override fun onActionWillExecute(notification: NotificareNotification, action: NotificareNotification.Action) {
        Timber.i("---> action will execute '$notification.id'")
        Toast.makeText(this, "Action will execute", Toast.LENGTH_SHORT).show()
    }

    override fun onActionExecuted(notification: NotificareNotification, action: NotificareNotification.Action) {
        Timber.i("---> action executed '$notification.id'")
        Toast.makeText(this, "Action executed", Toast.LENGTH_SHORT).show()
    }

    override fun onActionFailedToExecute(
        notification: NotificareNotification,
        action: NotificareNotification.Action,
        error: Exception?
    ) {
        Timber.i("---> action failed to execute '${action.label}'")
        Toast.makeText(this, "Action failed to execute", Toast.LENGTH_SHORT).show()
    }

    override fun onCustomActionReceived(
        notification: NotificareNotification,
        action: NotificareNotification.Action,
        uri: Uri
    ) {
        Timber.i("---> custom action received '$uri'")
        Toast.makeText(this, "Custom action received", Toast.LENGTH_SHORT).show()
    }
}
