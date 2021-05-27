package re.notifica.sample

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import re.notifica.Notificare
import re.notifica.NotificareCallback
import re.notifica.NotificareLogger
import re.notifica.models.*
import re.notifica.push.NotificarePush
import re.notifica.push.models.NotificareNotificationRemoteMessage
import re.notifica.push.ui.NotificarePushUI
import re.notifica.sample.databinding.ActivityMainBinding
import re.notifica.sample.ui.inbox.InboxActivity
import java.util.*

class MainActivity : AppCompatActivity(), Notificare.OnReadyListener, NotificarePushUI.NotificationLifecycleListener {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater).also {
            setContentView(it.root)
        }

        if (intent != null) handleNotificareIntent(intent)

        Notificare.addOnReadyListener(this)
        NotificarePushUI.addLifecycleListener(this)
    }

    override fun onDestroy() {
        super.onDestroy()

        Notificare.addOnReadyListener(this)
        NotificarePushUI.removeLifecycleListener(this)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)

        if (intent != null) handleNotificareIntent(intent)
    }

    private fun handleNotificareIntent(intent: Intent) {
        when (intent.action) {
            NotificarePush.INTENT_ACTION_REMOTE_MESSAGE_OPENED -> {
                val remoteMessage: NotificareNotificationRemoteMessage = requireNotNull(
                    intent.getParcelableExtra(NotificarePush.INTENT_EXTRA_REMOTE_MESSAGE)
                )

                val notification: NotificareNotification = requireNotNull(
                    intent.getParcelableExtra(Notificare.INTENT_EXTRA_NOTIFICATION)
                )

                val action: NotificareNotification.Action? = intent.getParcelableExtra(Notificare.INTENT_EXTRA_ACTION)

                NotificarePush.handleTrampolineMessage(remoteMessage, notification, action)
            }
            NotificarePush.INTENT_ACTION_NOTIFICATION_OPENED -> {
                val notification: NotificareNotification = requireNotNull(
                    intent.getParcelableExtra(Notificare.INTENT_EXTRA_NOTIFICATION)
                )

                val type = NotificareNotification.NotificationType.from(notification.type)
                if (type == NotificareNotification.NotificationType.ALERT && notification.actions.isEmpty()) {
                    Snackbar.make(binding.root, notification.message, Snackbar.LENGTH_LONG).show()
                } else {
                    NotificarePushUI.presentNotification(this, notification)
                }
            }
            NotificarePush.INTENT_ACTION_ACTION_OPENED -> {
                val notification: NotificareNotification = requireNotNull(
                    intent.getParcelableExtra(Notificare.INTENT_EXTRA_NOTIFICATION)
                )

                val action: NotificareNotification.Action = requireNotNull(
                    intent.getParcelableExtra(Notificare.INTENT_EXTRA_ACTION)
                )

                NotificarePushUI.presentAction(this, notification, action)
            }
        }

        when {
            Notificare.handleTestDeviceIntent(intent) -> {
                NotificareLogger.info("Handled the test device registration intent.")
                return
            }
        }

        val uri = intent.data ?: return
        NotificareLogger.info("Received deep link with uri = $uri")
    }

    fun onLaunchClicked(@Suppress("UNUSED_PARAMETER") view: View) {
        Notificare.launch()
    }

    fun onUnlaunchClicked(@Suppress("UNUSED_PARAMETER") view: View) {
        Notificare.unlaunch()
    }

    fun onEnableRemoteNotificationsClicked(@Suppress("UNUSED_PARAMETER") view: View) {
        NotificarePush.enableRemoteNotifications()
    }

    fun onDisableRemoteNotificationsClicked(@Suppress("UNUSED_PARAMETER") view: View) {
        NotificarePush.disableRemoteNotifications()
    }

    fun onOpenInboxClicked(@Suppress("UNUSED_PARAMETER") view: View) {
        startActivity(Intent(this, InboxActivity::class.java))
    }

    fun onFetchTagsClicked(@Suppress("UNUSED_PARAMETER") view: View) {
        Notificare.deviceManager.fetchTags(object : NotificareCallback<List<String>> {
            override fun onSuccess(result: List<String>) {
                Snackbar.make(binding.root, "$result", Snackbar.LENGTH_LONG).show()
            }

            override fun onFailure(e: Exception) {
                Snackbar.make(binding.root, "Failed to fetch device tags", Snackbar.LENGTH_LONG)
                    .show()
            }
        })
    }

    fun onAddTagsClicked(@Suppress("UNUSED_PARAMETER") view: View) {
        val tags = listOf(
            "hpinhal",
            "android",
            "remove-me",
        )

        Notificare.deviceManager.addTags(tags, object : NotificareCallback<Unit> {
            override fun onSuccess(result: Unit) {
                Snackbar.make(binding.root, "Done.", Snackbar.LENGTH_SHORT).show()
            }

            override fun onFailure(e: Exception) {
                Snackbar.make(binding.root, "Error: ${e.message}", Snackbar.LENGTH_SHORT).show()
            }
        })
    }

    fun onRemoveTagsClicked(@Suppress("UNUSED_PARAMETER") view: View) {
        Notificare.deviceManager.removeTag("remove-me", object : NotificareCallback<Unit> {
            override fun onSuccess(result: Unit) {
                Snackbar.make(binding.root, "Done.", Snackbar.LENGTH_SHORT).show()
            }

            override fun onFailure(e: Exception) {
                Snackbar.make(binding.root, "Error: ${e.message}", Snackbar.LENGTH_SHORT).show()
            }
        })
    }

    fun onClearTagsClicked(@Suppress("UNUSED_PARAMETER") view: View) {
        Notificare.deviceManager.clearTags(object : NotificareCallback<Unit> {
            override fun onSuccess(result: Unit) {
                Snackbar.make(binding.root, "Done.", Snackbar.LENGTH_SHORT).show()
            }

            override fun onFailure(e: Exception) {
                Snackbar.make(binding.root, "Error: ${e.message}", Snackbar.LENGTH_SHORT).show()
            }
        })
    }

    fun onFetchDndClicked(@Suppress("UNUSED_PARAMETER") view: View) {
        Notificare.deviceManager.fetchDoNotDisturb(object :
            NotificareCallback<NotificareDoNotDisturb?> {
            override fun onSuccess(result: NotificareDoNotDisturb?) {
                Log.i(TAG, "Do not disturb: $result")
                Snackbar.make(binding.root, "$result", Snackbar.LENGTH_SHORT).show()
            }

            override fun onFailure(e: Exception) {
                Snackbar.make(binding.root, "Error: ${e.message}", Snackbar.LENGTH_SHORT).show()
            }
        })
    }

    fun onUpdateDndClicked(@Suppress("UNUSED_PARAMETER") view: View) {
        val dnd = NotificareDoNotDisturb(
            start = NotificareTime("00:00"),
            end = NotificareTime(8, Calendar.getInstance().get(Calendar.MINUTE))
        )

        Notificare.deviceManager.updateDoNotDisturb(dnd, object : NotificareCallback<Unit> {
            override fun onSuccess(result: Unit) {
                Snackbar.make(binding.root, "Done.", Snackbar.LENGTH_SHORT).show()
            }

            override fun onFailure(e: Exception) {
                Snackbar.make(binding.root, "Error: ${e.message}", Snackbar.LENGTH_SHORT).show()
            }
        })
    }

    fun onClearDndClicked(@Suppress("UNUSED_PARAMETER") view: View) {
        Notificare.deviceManager.clearDoNotDisturb(object : NotificareCallback<Unit> {
            override fun onSuccess(result: Unit) {
                Snackbar.make(binding.root, "Done.", Snackbar.LENGTH_SHORT).show()
            }

            override fun onFailure(e: Exception) {
                Snackbar.make(binding.root, "Error: ${e.message}", Snackbar.LENGTH_SHORT).show()
            }
        })
    }

    fun onFetchUserDataClicked(@Suppress("UNUSED_PARAMETER") view: View) {
        Notificare.deviceManager.fetchUserData(object : NotificareCallback<NotificareUserData> {
            override fun onSuccess(result: NotificareUserData) {
                Log.i(TAG, "User data: $result")
                Snackbar.make(binding.root, "$result", Snackbar.LENGTH_SHORT).show()
            }

            override fun onFailure(e: Exception) {
                Snackbar.make(binding.root, "Error: ${e.message}", Snackbar.LENGTH_SHORT).show()
            }
        })
    }

    fun onUpdateUserDataClicked(@Suppress("UNUSED_PARAMETER") view: View) {
        val userData = mapOf(
            Pair("firstName", "Helder"),
            Pair("lastName", "Pinhal"),
        )

        Notificare.deviceManager.updateUserData(userData, object : NotificareCallback<Unit> {
            override fun onSuccess(result: Unit) {
                Snackbar.make(binding.root, "Done.", Snackbar.LENGTH_SHORT).show()
            }

            override fun onFailure(e: Exception) {
                Snackbar.make(binding.root, "Error: ${e.message}", Snackbar.LENGTH_SHORT).show()
            }
        })
    }

    fun onGetPreferredLanguageClicked(@Suppress("UNUSED_PARAMETER") view: View) {
        Snackbar.make(
            binding.root,
            "${Notificare.deviceManager.preferredLanguage}",
            Snackbar.LENGTH_SHORT
        ).show()
    }

    fun onUpdatePreferredLanguageClicked(@Suppress("UNUSED_PARAMETER") view: View) {
        Notificare.deviceManager.updatePreferredLanguage(
            "en-NL",
            object : NotificareCallback<Unit> {
                override fun onSuccess(result: Unit) {
                    Snackbar.make(binding.root, "Done.", Snackbar.LENGTH_SHORT).show()
                }

                override fun onFailure(e: Exception) {
                    Snackbar.make(binding.root, "Error: ${e.message}", Snackbar.LENGTH_SHORT).show()
                }
            })
    }

    fun onClearPreferredLanguageClicked(@Suppress("UNUSED_PARAMETER") view: View) {
        Notificare.deviceManager.updatePreferredLanguage(null, object : NotificareCallback<Unit> {
            override fun onSuccess(result: Unit) {
                Snackbar.make(binding.root, "Done.", Snackbar.LENGTH_SHORT).show()
            }

            override fun onFailure(e: Exception) {
                Snackbar.make(binding.root, "Error: ${e.message}", Snackbar.LENGTH_SHORT).show()
            }
        })
    }

    // region Notificare.OnReadyListener

    override fun onReady(application: NotificareApplication) {
        if (NotificarePush.isRemoteNotificationsEnabled) {
            NotificarePush.enableRemoteNotifications()
        }
    }

    // endregion

    // region NotificarePushUI.LifecycleListener

    override fun onNotificationWillPresent(notification: NotificareNotification) {
        NotificareLogger.info("---> notification will present '${notification.id}'")

        Handler(Looper.getMainLooper()).post {
            Toast.makeText(this, "Notification will present", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onNotificationPresented(notification: NotificareNotification) {
        NotificareLogger.info("---> notification presented '${notification.id}'")

        Handler(Looper.getMainLooper()).post {
            Toast.makeText(this, "Notification presented", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onNotificationFinishedPresenting(notification: NotificareNotification) {
        NotificareLogger.info("---> notification finished presenting '${notification.id}'")

        Handler(Looper.getMainLooper()).post {
            Toast.makeText(this, "Notification finished presenting", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onNotificationFailedToPresent(notification: NotificareNotification) {
        NotificareLogger.info("---> notification failed to present '${notification.id}'")

        Handler(Looper.getMainLooper()).post {
            Toast.makeText(this, "Notification failed to present", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onNotificationUrlClicked(notification: NotificareNotification, uri: Uri) {
        NotificareLogger.info("---> notification url clicked '${notification.id}'")

        Handler(Looper.getMainLooper()).post {
            Toast.makeText(this, "Notification URL clicked", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onActionWillExecute(notification: NotificareNotification, action: NotificareNotification.Action) {
        NotificareLogger.info("---> action will execute '${action.label}'")

        Handler(Looper.getMainLooper()).post {
            Toast.makeText(this, "Action will execute", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onActionExecuted(notification: NotificareNotification, action: NotificareNotification.Action) {
        NotificareLogger.info("---> action executed '${action.label}'")

        Handler(Looper.getMainLooper()).post {
            Toast.makeText(this, "Action executed", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onActionFailedToExecute(
        notification: NotificareNotification,
        action: NotificareNotification.Action,
        error: Exception?
    ) {
        NotificareLogger.info("---> action failed to execute '${action.label}'")

        Handler(Looper.getMainLooper()).post {
            Toast.makeText(this, "Action failed to execute", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCustomActionReceived(uri: Uri) {
        NotificareLogger.info("---> custom action received '$uri'")

        Handler(Looper.getMainLooper()).post {
            Toast.makeText(this, "Custom action received", Toast.LENGTH_SHORT).show()
        }
    }

    // endregion

    companion object {
        private val TAG = MainActivity::class.java.simpleName
    }
}
