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
import re.notifica.assets.NotificareAssets
import re.notifica.assets.models.NotificareAsset
import re.notifica.authentication.NotificareAuthentication
import re.notifica.authentication.models.NotificareUser
import re.notifica.authentication.models.NotificareUserPreference
import re.notifica.authentication.models.NotificareUserSegment
import re.notifica.geo.NotificareGeo
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

        val validateUserToken = NotificareAuthentication.parseValidateUserToken(intent)
        if (validateUserToken != null) {
            NotificareAuthentication.validateUser(validateUserToken, object : NotificareCallback<Unit> {
                override fun onSuccess(result: Unit) {
                    Log.i(TAG, "User validated.")
                }

                override fun onFailure(e: Exception) {
                    Log.e(TAG, "Failed to validate user.", e)
                }
            })

            return
        }

        val passwordResetToken = NotificareAuthentication.parsePasswordResetToken(intent)
        if (passwordResetToken != null) {
            NotificareAuthentication.resetPassword("123456", passwordResetToken, object : NotificareCallback<Unit> {
                override fun onSuccess(result: Unit) {
                    Log.i(TAG, "User password reset.")
                }

                override fun onFailure(e: Exception) {
                    Log.e(TAG, "Failed to reset user password.", e)
                }
            })

            return
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

    fun onCreateUserAccountClicked(@Suppress("UNUSED_PARAMETER") view: View) {
        NotificareAuthentication.createAccount(
            email = "helder+1@notifica.re",
            password = "123456",
            name = "Helder Pinhal",
            callback = object : NotificareCallback<Unit> {
                override fun onSuccess(result: Unit) {
                    Snackbar.make(binding.root, "Done.", Snackbar.LENGTH_SHORT).show()
                }

                override fun onFailure(e: Exception) {
                    Snackbar.make(binding.root, "Error: ${e.message}", Snackbar.LENGTH_SHORT).show()
                }
            })
    }

    fun onLoginClicked(@Suppress("UNUSED_PARAMETER") view: View) {
        NotificareAuthentication.login("helder@notifica.re", "123456", object : NotificareCallback<Unit> {
            override fun onSuccess(result: Unit) {
                Snackbar.make(binding.root, "Done.", Snackbar.LENGTH_SHORT).show()
            }

            override fun onFailure(e: Exception) {
                Snackbar.make(binding.root, "Error: ${e.message}", Snackbar.LENGTH_SHORT).show()
            }
        })
    }

    fun onLogoutClicked(@Suppress("UNUSED_PARAMETER") view: View) {
        NotificareAuthentication.logout(object : NotificareCallback<Unit> {
            override fun onSuccess(result: Unit) {
                Snackbar.make(binding.root, "Done.", Snackbar.LENGTH_SHORT).show()
            }

            override fun onFailure(e: Exception) {
                Snackbar.make(binding.root, "Error: ${e.message}", Snackbar.LENGTH_SHORT).show()
            }
        })
    }

    fun onFetchUserDetailsClicked(@Suppress("UNUSED_PARAMETER") view: View) {
        NotificareAuthentication.fetchUserDetails(object : NotificareCallback<NotificareUser> {
            override fun onSuccess(result: NotificareUser) {
                Log.i(TAG, "User: $result")
                Snackbar.make(binding.root, "$result", Snackbar.LENGTH_SHORT).show()
            }

            override fun onFailure(e: Exception) {
                Snackbar.make(binding.root, "Error: ${e.message}", Snackbar.LENGTH_SHORT).show()
            }
        })
    }

    fun onFetchUserPreferencesClicked(@Suppress("UNUSED_PARAMETER") view: View) {
        NotificareAuthentication.fetchUserPreferences(object : NotificareCallback<List<NotificareUserPreference>> {
            override fun onSuccess(result: List<NotificareUserPreference>) {
                Log.i(TAG, "User preferences: $result")
                Snackbar.make(binding.root, "$result", Snackbar.LENGTH_SHORT).show()
            }

            override fun onFailure(e: Exception) {
                Snackbar.make(binding.root, "Error: ${e.message}", Snackbar.LENGTH_SHORT).show()
            }
        })
    }

    fun onFetchUserSegmentsClicked(@Suppress("UNUSED_PARAMETER") view: View) {
        NotificareAuthentication.fetchUserSegments(object : NotificareCallback<List<NotificareUserSegment>> {
            override fun onSuccess(result: List<NotificareUserSegment>) {
                Log.i(TAG, "User segments: $result")
                Snackbar.make(binding.root, "$result", Snackbar.LENGTH_SHORT).show()
            }

            override fun onFailure(e: Exception) {
                Snackbar.make(binding.root, "Error: ${e.message}", Snackbar.LENGTH_SHORT).show()
            }
        })
    }

    fun onSendPasswordResetClicked(@Suppress("UNUSED_PARAMETER") view: View) {
        NotificareAuthentication.sendPasswordReset("helder@notifica.re", object : NotificareCallback<Unit> {
            override fun onSuccess(result: Unit) {
                Snackbar.make(binding.root, "Done.", Snackbar.LENGTH_SHORT).show()
            }

            override fun onFailure(e: Exception) {
                Snackbar.make(binding.root, "Error: ${e.message}", Snackbar.LENGTH_SHORT).show()
            }
        })
    }

    fun onResetPasswordClicked(@Suppress("UNUSED_PARAMETER") view: View) {
        NotificareAuthentication.resetPassword(
            password = "123456",
            token = "2825735c68d7b649c237becb6a245bbc6ab7c4684ce711aa03bc14e0cf9b99c7",
            callback = object : NotificareCallback<Unit> {
                override fun onSuccess(result: Unit) {
                    Snackbar.make(binding.root, "Done.", Snackbar.LENGTH_SHORT).show()
                }

                override fun onFailure(e: Exception) {
                    Snackbar.make(binding.root, "Error: ${e.message}", Snackbar.LENGTH_SHORT).show()
                }
            }
        )
    }

    fun onChangePasswordClicked(@Suppress("UNUSED_PARAMETER") view: View) {
        NotificareAuthentication.changePassword("123456", object : NotificareCallback<Unit> {
            override fun onSuccess(result: Unit) {
                Snackbar.make(binding.root, "Done.", Snackbar.LENGTH_SHORT).show()
            }

            override fun onFailure(e: Exception) {
                Snackbar.make(binding.root, "Error: ${e.message}", Snackbar.LENGTH_SHORT).show()
            }
        })
    }

    fun onValidateUserClicked(@Suppress("UNUSED_PARAMETER") view: View) {
        NotificareAuthentication.validateUser(
            token = "46f9a90a64652bb986907fd4784457ab0b738b269abd1ac80359589398dd7801",
            callback = object : NotificareCallback<Unit> {
                override fun onSuccess(result: Unit) {
                    Snackbar.make(binding.root, "Done.", Snackbar.LENGTH_SHORT).show()
                }

                override fun onFailure(e: Exception) {
                    Snackbar.make(binding.root, "Error: ${e.message}", Snackbar.LENGTH_SHORT).show()
                }
            }
        )
    }

    fun onGeneratePushEmailClicked(@Suppress("UNUSED_PARAMETER") view: View) {
        NotificareAuthentication.generatePushEmailAddress(object : NotificareCallback<NotificareUser> {
            override fun onSuccess(result: NotificareUser) {
                Log.i(TAG, "$result")
                Snackbar.make(binding.root, "$result", Snackbar.LENGTH_SHORT).show()
            }

            override fun onFailure(e: Exception) {
                Snackbar.make(binding.root, "Error: ${e.message}", Snackbar.LENGTH_SHORT).show()
            }
        })
    }

    fun onAddUserSegmentClicked(@Suppress("UNUSED_PARAMETER") view: View) {
        NotificareAuthentication.fetchUserSegments(object : NotificareCallback<List<NotificareUserSegment>> {
            override fun onSuccess(result: List<NotificareUserSegment>) {
                val segment = result.first()

                NotificareAuthentication.addUserSegment(segment, object : NotificareCallback<Unit> {
                    override fun onSuccess(result: Unit) {
                        Snackbar.make(binding.root, "Done.", Snackbar.LENGTH_SHORT).show()
                    }

                    override fun onFailure(e: Exception) {
                        Snackbar.make(binding.root, "Error: ${e.message}", Snackbar.LENGTH_SHORT).show()
                    }
                })
            }

            override fun onFailure(e: Exception) {
                Snackbar.make(binding.root, "Error: ${e.message}", Snackbar.LENGTH_SHORT).show()
            }
        })
    }

    fun onRemoveUserSegmentClicked(@Suppress("UNUSED_PARAMETER") view: View) {
        NotificareAuthentication.fetchUserSegments(object : NotificareCallback<List<NotificareUserSegment>> {
            override fun onSuccess(result: List<NotificareUserSegment>) {
                val segment = result.first()

                NotificareAuthentication.removeUserSegment(segment, object : NotificareCallback<Unit> {
                    override fun onSuccess(result: Unit) {
                        Snackbar.make(binding.root, "Done.", Snackbar.LENGTH_SHORT).show()
                    }

                    override fun onFailure(e: Exception) {
                        Snackbar.make(binding.root, "Error: ${e.message}", Snackbar.LENGTH_SHORT).show()
                    }
                })
            }

            override fun onFailure(e: Exception) {
                Snackbar.make(binding.root, "Error: ${e.message}", Snackbar.LENGTH_SHORT).show()
            }
        })
    }

    fun onAddUserSegmentToPreferenceClicked(@Suppress("UNUSED_PARAMETER") view: View) {
        NotificareAuthentication.fetchUserPreferences(object : NotificareCallback<List<NotificareUserPreference>> {
            override fun onSuccess(result: List<NotificareUserPreference>) {
                val preference = result.first()
                val option = preference.options.first()

                NotificareAuthentication.addUserSegmentToPreference(
                    option,
                    preference,
                    object : NotificareCallback<Unit> {
                        override fun onSuccess(result: Unit) {
                            Snackbar.make(binding.root, "Done.", Snackbar.LENGTH_SHORT).show()
                        }

                        override fun onFailure(e: Exception) {
                            Snackbar.make(binding.root, "Error: ${e.message}", Snackbar.LENGTH_SHORT).show()
                        }
                    }
                )
            }

            override fun onFailure(e: Exception) {
                Snackbar.make(binding.root, "Error: ${e.message}", Snackbar.LENGTH_SHORT).show()
            }
        })
    }

    fun onRemoveUserSegmentFromPreferenceClicked(@Suppress("UNUSED_PARAMETER") view: View) {
        NotificareAuthentication.fetchUserPreferences(object : NotificareCallback<List<NotificareUserPreference>> {
            override fun onSuccess(result: List<NotificareUserPreference>) {
                val preference = result.first()
                val option = preference.options.first()

                NotificareAuthentication.removeUserSegmentFromPreference(
                    option,
                    preference,
                    object : NotificareCallback<Unit> {
                        override fun onSuccess(result: Unit) {
                            Snackbar.make(binding.root, "Done.", Snackbar.LENGTH_SHORT).show()
                        }

                        override fun onFailure(e: Exception) {
                            Snackbar.make(binding.root, "Error: ${e.message}", Snackbar.LENGTH_SHORT).show()
                        }
                    }
                )
            }

            override fun onFailure(e: Exception) {
                Snackbar.make(binding.root, "Error: ${e.message}", Snackbar.LENGTH_SHORT).show()
            }
        })
    }

    fun onEnableLocationUpdatesClicked(@Suppress("UNUSED_PARAMETER") view: View) {
        val foregroundLocationPermission =
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)

        if (foregroundLocationPermission != PackageManager.PERMISSION_GRANTED) {
            if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)) {
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

                return
            }

            foregroundLocationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            )

            return
        }

        val backgroundLocationPermissionStr =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) Manifest.permission.ACCESS_BACKGROUND_LOCATION
            else Manifest.permission.ACCESS_FINE_LOCATION

        val backgroundLocationPermission = ContextCompat.checkSelfPermission(this, backgroundLocationPermissionStr)

        if (backgroundLocationPermission != PackageManager.PERMISSION_GRANTED) {
            if (shouldShowRequestPermissionRationale(backgroundLocationPermissionStr)) {
                AlertDialog.Builder(this)
                    .setTitle(R.string.app_name)
                    .setMessage(R.string.main_background_permission_rationale)
                    .setPositiveButton(android.R.string.ok) { _, _ ->
                        backgroundLocationPermissionLauncher.launch(backgroundLocationPermissionStr)
                    }
                    .show()

                return
            }

            backgroundLocationPermissionLauncher.launch(backgroundLocationPermissionStr)
            return
        }

        NotificareGeo.enableLocationUpdates()
    }

    fun onDisableLocationUpdatesClicked(@Suppress("UNUSED_PARAMETER") view: View) {
        NotificareGeo.disableLocationUpdates()
    }

    // region Notificare.OnReadyListener

    override fun onReady(application: NotificareApplication) {
        if (NotificarePush.isRemoteNotificationsEnabled) {
            NotificarePush.enableRemoteNotifications()
        }

        if (NotificareGeo.locationServicesEnabled) {
            NotificareGeo.enableLocationUpdates()
        }
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
