package re.notifica.sample

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar
import re.notifica.Notificare
import re.notifica.NotificareCallback
import re.notifica.assets.ktx.assets
import re.notifica.assets.models.NotificareAsset
import re.notifica.geo.NotificareGeo
import re.notifica.geo.ktx.INTENT_ACTION_BEACON_NOTIFICATION_OPENED
import re.notifica.geo.ktx.geo
import re.notifica.geo.models.NotificareBeacon
import re.notifica.geo.models.NotificareLocation
import re.notifica.geo.models.NotificareRegion
import re.notifica.iam.NotificareInAppMessaging
import re.notifica.iam.ktx.inAppMessaging
import re.notifica.iam.models.NotificareInAppMessage
import re.notifica.ktx.device
import re.notifica.models.*
import re.notifica.monetize.NotificareMonetize
import re.notifica.monetize.ktx.monetize
import re.notifica.monetize.models.NotificarePurchase
import re.notifica.push.ktx.INTENT_ACTION_ACTION_OPENED
import re.notifica.push.ktx.INTENT_ACTION_NOTIFICATION_OPENED
import re.notifica.push.ktx.push
import re.notifica.push.ui.NotificarePushUI
import re.notifica.push.ui.ktx.pushUI
import re.notifica.sample.databinding.ActivityMainBinding
import re.notifica.sample.ui.beacons.BeaconsActivity
import re.notifica.sample.ui.iap.InAppPurchasesActivity
import re.notifica.sample.ui.inbox.InboxActivity
import re.notifica.sample.ui.wallet.WalletActivity
import re.notifica.scannables.NotificareScannables
import re.notifica.scannables.NotificareUserCancelledScannableSessionException
import re.notifica.scannables.ktx.scannables
import re.notifica.scannables.models.NotificareScannable
import java.util.*

class MainActivity : AppCompatActivity(), Notificare.Listener, NotificarePushUI.NotificationLifecycleListener,
    NotificareScannables.ScannableSessionListener, NotificareGeo.Listener, NotificareMonetize.Listener {

    private lateinit var binding: ActivityMainBinding

    private val notificationsPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            Log.i(TAG, "User denied notifications permissions.")
            return@registerForActivityResult
        }

        onEnableRemoteNotificationsClicked(binding.root)
    }

    private val foregroundLocationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.any { !it.value }) {
            Log.i(TAG, "User denied foreground location permissions.")
            return@registerForActivityResult
        }

        onEnableLocationUpdatesClicked(binding.root)
    }

    private val backgroundLocationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            Log.i(TAG, "User denied background location permissions.")
            return@registerForActivityResult
        }

        onEnableLocationUpdatesClicked(binding.root)
    }

    private val bluetoothScanPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            Log.i(TAG, "User denied bluetooth scan permissions.")
            return@registerForActivityResult
        }

        onEnableLocationUpdatesClicked(binding.root)
    }

    private val messageLifecycleListener = object : NotificareInAppMessaging.MessageLifecycleListener {
        override fun onMessagePresented(message: NotificareInAppMessage) {
            Log.i(TAG, "---> message presented '${message.name}'")
            Toast.makeText(this@MainActivity, "Message presented", Toast.LENGTH_SHORT).show()
        }

        override fun onMessageFinishedPresenting(message: NotificareInAppMessage) {
            Log.i(TAG, "---> message finished presenting '${message.name}'")
            Toast.makeText(this@MainActivity, "Message finished presenting", Toast.LENGTH_SHORT).show()
        }

        override fun onMessageFailedToPresent(message: NotificareInAppMessage) {
            Log.i(TAG, "---> message failed to present '${message.name}'")
            Toast.makeText(this@MainActivity, "Message failed to present", Toast.LENGTH_SHORT).show()
        }

        override fun onActionExecuted(message: NotificareInAppMessage, action: NotificareInAppMessage.Action) {
            Log.i(TAG, "---> action executed '${message.name}'")
            Toast.makeText(this@MainActivity, "Action executed", Toast.LENGTH_SHORT).show()
        }

        override fun onActionFailedToExecute(
            message: NotificareInAppMessage,
            action: NotificareInAppMessage.Action,
            error: Exception?
        ) {
            Log.i(TAG, "---> action failed to execute '${message.name}'", error)
            Toast.makeText(this@MainActivity, "Action failed to execute", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater).also {
            setContentView(it.root)
        }

        if (intent != null) handleIntent(intent)

        Notificare.addListener(this)
        Notificare.pushUI().addLifecycleListener(this)
        Notificare.scannables().addListener(this)
        Notificare.geo().addListener(this)
        Notificare.monetize().addListener(this)
        Notificare.inAppMessaging().addLifecycleListener(messageLifecycleListener)
    }

    override fun onDestroy() {
        super.onDestroy()

        Notificare.removeListener(this)
        Notificare.pushUI().removeLifecycleListener(this)
        Notificare.scannables().removeListener(this)
        Notificare.geo().removeListener(this)
        Notificare.monetize().removeListener(this)
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
                val notification: NotificareNotification = requireNotNull(
                    intent.getParcelableExtra(Notificare.INTENT_EXTRA_NOTIFICATION)
                )

                val type = NotificareNotification.NotificationType.from(notification.type)
                if (type == NotificareNotification.NotificationType.ALERT && notification.actions.isEmpty()) {
                    Snackbar.make(binding.root, notification.message, Snackbar.LENGTH_LONG).show()
                } else {
                    Notificare.pushUI().presentNotification(this, notification)
                }

                return
            }
            Notificare.INTENT_ACTION_ACTION_OPENED -> {
                val notification: NotificareNotification = requireNotNull(
                    intent.getParcelableExtra(Notificare.INTENT_EXTRA_NOTIFICATION)
                )

                val action: NotificareNotification.Action = requireNotNull(
                    intent.getParcelableExtra(Notificare.INTENT_EXTRA_ACTION)
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
        Log.i(TAG, "Received deep link with uri = $uri")
        Toast.makeText(this, "Deep link = $uri", Toast.LENGTH_SHORT).show()
    }

    fun onLaunchClicked(@Suppress("UNUSED_PARAMETER") view: View) {
        Notificare.launch()
    }

    fun onUnlaunchClicked(@Suppress("UNUSED_PARAMETER") view: View) {
        Notificare.unlaunch()
    }

    fun onEnableRemoteNotificationsClicked(@Suppress("UNUSED_PARAMETER") view: View) {
        if (!ensureNotificationsPermission()) return

        Notificare.push().enableRemoteNotifications()
    }

    fun onDisableRemoteNotificationsClicked(@Suppress("UNUSED_PARAMETER") view: View) {
        Notificare.push().disableRemoteNotifications()
    }

    fun onOpenInboxClicked(@Suppress("UNUSED_PARAMETER") view: View) {
        startActivity(Intent(this, InboxActivity::class.java))
    }

    fun onFetchTagsClicked(@Suppress("UNUSED_PARAMETER") view: View) {
        Notificare.device().fetchTags(object : NotificareCallback<List<String>> {
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

        Notificare.device().addTags(tags, object : NotificareCallback<Unit> {
            override fun onSuccess(result: Unit) {
                Snackbar.make(binding.root, "Done.", Snackbar.LENGTH_SHORT).show()
            }

            override fun onFailure(e: Exception) {
                Snackbar.make(binding.root, "Error: ${e.message}", Snackbar.LENGTH_SHORT).show()
            }
        })
    }

    fun onRemoveTagsClicked(@Suppress("UNUSED_PARAMETER") view: View) {
        Notificare.device().removeTag("remove-me", object : NotificareCallback<Unit> {
            override fun onSuccess(result: Unit) {
                Snackbar.make(binding.root, "Done.", Snackbar.LENGTH_SHORT).show()
            }

            override fun onFailure(e: Exception) {
                Snackbar.make(binding.root, "Error: ${e.message}", Snackbar.LENGTH_SHORT).show()
            }
        })
    }

    fun onClearTagsClicked(@Suppress("UNUSED_PARAMETER") view: View) {
        Notificare.device().clearTags(object : NotificareCallback<Unit> {
            override fun onSuccess(result: Unit) {
                Snackbar.make(binding.root, "Done.", Snackbar.LENGTH_SHORT).show()
            }

            override fun onFailure(e: Exception) {
                Snackbar.make(binding.root, "Error: ${e.message}", Snackbar.LENGTH_SHORT).show()
            }
        })
    }

    fun onFetchDndClicked(@Suppress("UNUSED_PARAMETER") view: View) {
        Notificare.device().fetchDoNotDisturb(object :
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

        Notificare.device().updateDoNotDisturb(dnd, object : NotificareCallback<Unit> {
            override fun onSuccess(result: Unit) {
                Snackbar.make(binding.root, "Done.", Snackbar.LENGTH_SHORT).show()
            }

            override fun onFailure(e: Exception) {
                Snackbar.make(binding.root, "Error: ${e.message}", Snackbar.LENGTH_SHORT).show()
            }
        })
    }

    fun onClearDndClicked(@Suppress("UNUSED_PARAMETER") view: View) {
        Notificare.device().clearDoNotDisturb(object : NotificareCallback<Unit> {
            override fun onSuccess(result: Unit) {
                Snackbar.make(binding.root, "Done.", Snackbar.LENGTH_SHORT).show()
            }

            override fun onFailure(e: Exception) {
                Snackbar.make(binding.root, "Error: ${e.message}", Snackbar.LENGTH_SHORT).show()
            }
        })
    }

    fun onFetchUserDataClicked(@Suppress("UNUSED_PARAMETER") view: View) {
        Notificare.device().fetchUserData(object : NotificareCallback<NotificareUserData> {
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

        Notificare.device().updateUserData(userData, object : NotificareCallback<Unit> {
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
            "${Notificare.device().preferredLanguage}",
            Snackbar.LENGTH_SHORT
        ).show()
    }

    fun onUpdatePreferredLanguageClicked(@Suppress("UNUSED_PARAMETER") view: View) {
        Notificare.device().updatePreferredLanguage(
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
        Notificare.device().updatePreferredLanguage(null, object : NotificareCallback<Unit> {
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
        Notificare.assets().fetch("test_helder", object : NotificareCallback<List<NotificareAsset>> {
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
        // Notificare.scannables().startScannableSession(this)
        if (Notificare.scannables().canStartNfcScannableSession) {
            Notificare.scannables().startNfcScannableSession(this)
        } else {
            Notificare.scannables().startQrCodeScannableSession(this)
        }
    }

    fun onEnableLocationUpdatesClicked(@Suppress("UNUSED_PARAMETER") view: View) {
        if (!ensureForegroundLocationPermission()) return
        if (!ensureBackgroundLocationPermission()) return
        if (!ensureBluetoothScanPermission()) return

        Notificare.geo().enableLocationUpdates()
    }

    fun onDisableLocationUpdatesClicked(@Suppress("UNUSED_PARAMETER") view: View) {
        Notificare.geo().disableLocationUpdates()
    }

    fun onRangingBeaconsClicked(@Suppress("UNUSED_PARAMETER") view: View) {
        startActivity(Intent(this, BeaconsActivity::class.java))
    }

    fun onOpenWalletClicked(@Suppress("UNUSED_PARAMETER") view: View) {
        startActivity(Intent(this, WalletActivity::class.java))
    }

    fun onRefreshClicked(@Suppress("UNUSED_PARAMETER") view: View) {
        Notificare.monetize().refresh(object : NotificareCallback<Unit> {
            override fun onSuccess(result: Unit) {
                Snackbar.make(binding.root, "Done.", Snackbar.LENGTH_SHORT).show()
            }

            override fun onFailure(e: Exception) {
                Snackbar.make(binding.root, "Error: ${e.message}", Snackbar.LENGTH_SHORT).show()
            }
        })
    }

    fun onOpenPurchasesClicked(@Suppress("UNUSED_PARAMETER") view: View) {
        startActivity(Intent(this, InAppPurchasesActivity::class.java))
    }

    fun onToggleSuppressedMessagesClicked(@Suppress("UNUSED_PARAMETER") view: View) {
        Notificare.inAppMessaging().hasMessagesSuppressed = !Notificare.inAppMessaging().hasMessagesSuppressed
    }

    // region Notificare.Listener

    override fun onReady(application: NotificareApplication) {
        if (Notificare.push().hasRemoteNotificationsEnabled) {
            Notificare.push().enableRemoteNotifications()
        }

        if (Notificare.geo().hasLocationServicesEnabled) {
            Notificare.geo().enableLocationUpdates()
        }

        Snackbar.make(binding.root, "Notificare finished launching.", Snackbar.LENGTH_SHORT).show()
    }

    override fun onUnlaunched() {
        Snackbar.make(binding.root, "Notificare finished un-launching.", Snackbar.LENGTH_SHORT).show()
    }

    // endregion

    // region NotificarePushUI.LifecycleListener

    override fun onNotificationWillPresent(notification: NotificareNotification) {
        Log.i(TAG, "---> notification will present '${notification.id}'")
        Toast.makeText(this, "Notification will present", Toast.LENGTH_SHORT).show()
    }

    override fun onNotificationPresented(notification: NotificareNotification) {
        Log.i(TAG, "---> notification presented '${notification.id}'")
        Toast.makeText(this, "Notification presented", Toast.LENGTH_SHORT).show()
    }

    override fun onNotificationFinishedPresenting(notification: NotificareNotification) {
        Log.i(TAG, "---> notification finished presenting '${notification.id}'")
        Toast.makeText(this, "Notification finished presenting", Toast.LENGTH_SHORT).show()
    }

    override fun onNotificationFailedToPresent(notification: NotificareNotification) {
        Log.i(TAG, "---> notification failed to present '${notification.id}'")
        Toast.makeText(this, "Notification failed to present", Toast.LENGTH_SHORT).show()
    }

    override fun onNotificationUrlClicked(notification: NotificareNotification, uri: Uri) {
        Log.i(TAG, "---> notification url clicked '${notification.id}'")
        Toast.makeText(this, "Notification URL clicked", Toast.LENGTH_SHORT).show()
    }

    override fun onActionWillExecute(notification: NotificareNotification, action: NotificareNotification.Action) {
        Log.i(TAG, "---> action will execute '${action.label}'")
        Toast.makeText(this, "Action will execute", Toast.LENGTH_SHORT).show()
    }

    override fun onActionExecuted(notification: NotificareNotification, action: NotificareNotification.Action) {
        Log.i(TAG, "---> action executed '${action.label}'")
        Toast.makeText(this, "Action executed", Toast.LENGTH_SHORT).show()
    }

    override fun onActionFailedToExecute(
        notification: NotificareNotification,
        action: NotificareNotification.Action,
        error: Exception?
    ) {
        Log.i(TAG, "---> action failed to execute '${action.label}'")
        Toast.makeText(this, "Action failed to execute", Toast.LENGTH_SHORT).show()
    }

    override fun onCustomActionReceived(
        notification: NotificareNotification,
        action: NotificareNotification.Action,
        uri: Uri
    ) {
        Log.i(TAG, "---> custom action received '$uri'")
        Toast.makeText(this, "Custom action received", Toast.LENGTH_SHORT).show()
    }

    // endregion

    // region NotificareScannables.ScannableSessionListener

    override fun onScannableDetected(scannable: NotificareScannable) {
        Snackbar.make(binding.root, "$scannable", Snackbar.LENGTH_SHORT).apply {
            addCallback(object : Snackbar.Callback() {
                override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                    val notification = scannable.notification ?: return
                    Notificare.pushUI().presentNotification(this@MainActivity, notification)
                }
            })
        }.show()
    }

    override fun onScannableSessionError(error: Exception) {
        if (error is NotificareUserCancelledScannableSessionException) {
            return
        }

        Snackbar.make(binding.root, "Failed to detect scannable: ${error.message}", Snackbar.LENGTH_SHORT).apply {
            setBackgroundTint(ContextCompat.getColor(this@MainActivity, R.color.notificare_error))
            setTextColor(ContextCompat.getColor(this@MainActivity, R.color.white))
        }.show()
    }

    // endregion

    // region NotificareGeo.Listener

    override fun onLocationUpdated(location: NotificareLocation) {
        Log.w(TAG, "---> onLocationUpdated = $location")
    }

    override fun onRegionEntered(region: NotificareRegion) {
        Log.w(TAG, "---> onEnterRegion = $region")
    }

    override fun onRegionExited(region: NotificareRegion) {
        Log.w(TAG, "---> onExitRegion = $region")
    }

    override fun onBeaconEntered(beacon: NotificareBeacon) {
        Log.w(TAG, "---> onEnterBeacon = $beacon")
    }

    override fun onBeaconExited(beacon: NotificareBeacon) {
        Log.w(TAG, "---> onExitBeacon = $beacon")
    }

    override fun onBeaconsRanged(region: NotificareRegion, beacons: List<NotificareBeacon>) {
//        Log.w(TAG, "---> onBeaconsRanged")
//        Log.w(TAG, "---> region = $region")
//        Log.w(TAG, "---> beacons = $beacons")
    }

    // endregion

    // region NotificareMonetize.Listener

    override fun onBillingSetupFinished() {
        Log.i(TAG, "---> billing setup finished")
    }

    override fun onBillingSetupFailed(code: Int, message: String) {
        Log.i(TAG, "---> billing setup failed with code '$code': $message")
    }

    override fun onPurchaseFinished(purchase: NotificarePurchase) {
        Log.i(TAG, "---> purchase finished: $purchase")
    }

    override fun onPurchaseRestored(purchase: NotificarePurchase) {
        Log.i(TAG, "---> purchase restored: $purchase")
    }

    override fun onPurchaseCanceled() {
        Log.i(TAG, "---> purchase canceled")
    }

    override fun onPurchaseFailed(code: Int, message: String) {
        Log.w(TAG, "---> purchase failed with code '$code': $message")
    }

    // endregion

    private fun ensureNotificationsPermission(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true

        val permission = Manifest.permission.POST_NOTIFICATIONS
        val granted = ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
        if (granted) return true

        if (shouldShowRequestPermissionRationale(permission)) {
            AlertDialog.Builder(this)
                .setTitle(R.string.app_name)
                .setMessage(R.string.main_notifications_permission_rationale)
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    notificationsPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
                .show()

            return false
        }

        notificationsPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        return false
    }

    private fun ensureForegroundLocationPermission(): Boolean {
        val permission = Manifest.permission.ACCESS_FINE_LOCATION
        val granted = ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
        if (granted) return true

        if (shouldShowRequestPermissionRationale(permission)) {
            AlertDialog.Builder(this)
                .setTitle(R.string.app_name)
                .setMessage(R.string.main_foreground_permission_rationale)
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    foregroundLocationPermissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        )
                    )
                }
                .show()

            return false
        }

        foregroundLocationPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        )

        return false
    }

    private fun ensureBackgroundLocationPermission(): Boolean {
        val permission = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> Manifest.permission.ACCESS_BACKGROUND_LOCATION
            else -> Manifest.permission.ACCESS_FINE_LOCATION
        }

        val granted = ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
        if (granted) return true

        if (shouldShowRequestPermissionRationale(permission)) {
            AlertDialog.Builder(this)
                .setTitle(R.string.app_name)
                .setMessage(R.string.main_background_permission_rationale)
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    backgroundLocationPermissionLauncher.launch(permission)
                }
                .show()

            return false
        }

        backgroundLocationPermissionLauncher.launch(permission)
        return false
    }

    private fun ensureBluetoothScanPermission(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true

        val permission = Manifest.permission.BLUETOOTH_SCAN
        val granted = ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
        if (granted) return true

        if (shouldShowRequestPermissionRationale(permission)) {
            AlertDialog.Builder(this)
                .setTitle(R.string.app_name)
                .setMessage(R.string.main_background_permission_rationale)
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    bluetoothScanPermissionLauncher.launch(permission)
                }
                .show()

            return false
        }

        bluetoothScanPermissionLauncher.launch(permission)
        return false
    }

    companion object {
        private val TAG = MainActivity::class.java.simpleName
    }
}
