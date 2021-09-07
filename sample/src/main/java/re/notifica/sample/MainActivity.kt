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
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar
import re.notifica.Notificare
import re.notifica.NotificareCallback
import re.notifica.internal.NotificareLogger
import re.notifica.assets.NotificareAssets
import re.notifica.assets.models.NotificareAsset
import re.notifica.models.*
import re.notifica.push.NotificarePush
import re.notifica.push.ui.NotificarePushUI
import re.notifica.sample.databinding.ActivityMainBinding
import re.notifica.sample.ui.inbox.InboxActivity
import re.notifica.scannables.NotificareScannables
import re.notifica.scannables.NotificareScannablesException
import re.notifica.scannables.models.NotificareScannable
import java.util.*

class MainActivity : AppCompatActivity(), Notificare.OnReadyListener, NotificarePushUI.NotificationLifecycleListener,
    NotificareScannables.ScannableSessionListener {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater).also {
            setContentView(it.root)
        }

        if (intent != null) handleIntent(intent)

        Notificare.addOnReadyListener(this)
        NotificarePushUI.addLifecycleListener(this)
        NotificareScannables.addListener(this)
    }

    override fun onDestroy() {
        super.onDestroy()

        Notificare.addOnReadyListener(this)
        NotificarePushUI.removeLifecycleListener(this)
        NotificareScannables.removeListener(this)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)

        if (intent != null) handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        if (NotificarePush.handleTrampolineIntent(intent)) return
        if (Notificare.handleTestDeviceIntent(intent)) return
        if (Notificare.handleDynamicLinkIntent(this, intent)) return

        when (intent.action) {
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

                return
            }
            NotificarePush.INTENT_ACTION_ACTION_OPENED -> {
                val notification: NotificareNotification = requireNotNull(
                    intent.getParcelableExtra(Notificare.INTENT_EXTRA_NOTIFICATION)
                )

                val action: NotificareNotification.Action = requireNotNull(
                    intent.getParcelableExtra(Notificare.INTENT_EXTRA_ACTION)
                )

                NotificarePushUI.presentAction(this, notification, action)
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

    fun onFetchNotificationClicked(@Suppress("UNUSED_PARAMETER") view: View) {
        Notificare.fetchNotification("60f01c9ef8ca33707ec4e8e1", object : NotificareCallback<NotificareNotification> {
            override fun onSuccess(result: NotificareNotification) {
                Log.i(TAG, "Notification: $result")
                Snackbar.make(binding.root, "$result", Snackbar.LENGTH_SHORT).show()
            }

            override fun onFailure(e: Exception) {
                Snackbar.make(binding.root, "Error: ${e.message}", Snackbar.LENGTH_SHORT).show()
            }
        })
    }

    fun onFetchAssetsClicked(@Suppress("UNUSED_PARAMETER") view: View) {
        NotificareAssets.fetchAssets("test_helder", object : NotificareCallback<List<NotificareAsset>> {
            override fun onSuccess(result: List<NotificareAsset>) {
                Log.i(TAG, "Assets: $result")
                Snackbar.make(binding.root, "$result", Snackbar.LENGTH_SHORT).show()
            }

            override fun onFailure(e: Exception) {
                Snackbar.make(binding.root, "Error: ${e.message}", Snackbar.LENGTH_SHORT).show()
            }
        })
    }

    fun onStartScannableSessionClicked(@Suppress("UNUSED_PARAMETER") view: View) {
        // NotificareScannables.startNfcScannableSession(this)
        if (NotificareScannables.canStartNfcScannableSession) {
            NotificareScannables.startNfcScannableSession(this)
        } else {
            NotificareScannables.startQrCodeScannableSession(this)
        }
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

    override fun onCustomActionReceived(
        notification: NotificareNotification,
        action: NotificareNotification.Action,
        uri: Uri
    ) {
        NotificareLogger.info("---> custom action received '$uri'")

        Handler(Looper.getMainLooper()).post {
            Toast.makeText(this, "Custom action received", Toast.LENGTH_SHORT).show()
        }
    }

    // endregion

    // region NotificareScannables.ScannableSessionListener

    override fun onScannableDetected(scannable: NotificareScannable) {
        Snackbar.make(binding.root, "$scannable", Snackbar.LENGTH_SHORT).apply {
            addCallback(object : Snackbar.Callback() {
                override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                    val notification = scannable.notification ?: return
                    NotificarePushUI.presentNotification(this@MainActivity, notification)
                }
            })
        }.show()
    }

    override fun onScannerSessionError(error: Exception) {
        if (error is NotificareScannablesException.UserCancelledScannableSession) {
            return
        }

        Snackbar.make(binding.root, "Failed to detect scannable: ${error.message}", Snackbar.LENGTH_SHORT).apply {
            setBackgroundTint(ContextCompat.getColor(this@MainActivity, R.color.notificare_error))
            setTextColor(ContextCompat.getColor(this@MainActivity, R.color.white))
        }.show()
    }

    // endregion

    companion object {
        private val TAG = MainActivity::class.java.simpleName
    }
}
