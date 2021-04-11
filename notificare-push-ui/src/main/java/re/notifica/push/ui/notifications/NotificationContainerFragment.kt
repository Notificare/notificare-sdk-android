package re.notifica.push.ui.notifications

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import re.notifica.Notificare
import re.notifica.NotificareLogger
import re.notifica.models.NotificareNotification
import re.notifica.push.ui.NotificarePushUI
import re.notifica.push.ui.R
import re.notifica.push.ui.databinding.NotificareNotificationContainerFragmentBinding
import re.notifica.push.ui.models.NotificarePendingResult
import re.notifica.push.ui.notifications.fragments.NotificareCallbackActionFragment
import re.notifica.push.ui.notifications.fragments.base.NotificationFragment

class NotificationContainerFragment
    : Fragment(), NotificationFragment.Callback, NotificationDialog.Callback {

    private lateinit var binding: NotificareNotificationContainerFragmentBinding
    private lateinit var notification: NotificareNotification
    private var action: NotificareNotification.Action? = null
    private lateinit var callback: Callback

    private var pendingAction: NotificareNotification.Action? = null
    private var pendingResult: NotificarePendingResult? = null

    private var notificationDialog: NotificationDialog? = null
    private var actionsDialog: NotificationDialog? = null

    private var showActionsMenuItem = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        notification = savedInstanceState?.getParcelable(Notificare.INTENT_EXTRA_NOTIFICATION)
            ?: arguments?.getParcelable(Notificare.INTENT_EXTRA_NOTIFICATION)
                ?: throw IllegalArgumentException("Missing required notification parameter.")

        action = savedInstanceState?.getParcelable(Notificare.INTENT_EXTRA_ACTION)
            ?: arguments?.getParcelable(Notificare.INTENT_EXTRA_ACTION)

        try {
            callback = activity as Callback
        } catch (e: ClassCastException) {
            throw ClassCastException("Parent activity must implement NotificationContainerFragment.Callback.")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = NotificareNotificationContainerFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Inform user that this type has actions attached
        if (action == null && notification.type != NotificareNotification.TYPE_ALERT && notification.type != NotificareNotification.TYPE_PASSBOOK && notification.actions.isNotEmpty()) {
            setHasOptionsMenu(true)
        }

        if (savedInstanceState != null) return

        val type = NotificareNotification.NotificationType.from(notification.type)
        val fragmentClassName = NotificarePushUI.getFragmentCanonicalClassName(notification)

        val fragment = fragmentClassName?.let {
            try {
                val klass = Class.forName(it)
                klass.getConstructor().newInstance() as Fragment
            } catch (e: Exception) {
                NotificareLogger.error(
                    "Failed to dynamically create the concrete notification fragment.",
                    e
                )

                null
            }
        }

        if (fragment != null) {
            fragment.arguments = Bundle().apply {
                putParcelable(Notificare.INTENT_EXTRA_NOTIFICATION, notification)
            }

            childFragmentManager
                .beginTransaction()
                .add(binding.notificareNotificationFragmentContainer.id, fragment)
                .commit()
        }

        if (action == null && (type == NotificareNotification.NotificationType.ALERT || type == NotificareNotification.NotificationType.PASSBOOK)) {
            callback.onNotificationFragmentCanHideActionBar(notification)
            NotificationDialog.newInstance(notification)
                .also { notificationDialog = it }
                .show(childFragmentManager, "dialog")
        } else {
            callback.onNotificationFragmentShouldShowActionBar(notification)
        }

        // Handle the action is one was provided.
        action?.run { handleAction(this) }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable(Notificare.INTENT_EXTRA_NOTIFICATION, notification)
        outState.putParcelable(Notificare.INTENT_EXTRA_ACTION, action)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when (requestCode) {
            CAMERA_REQUEST_CODE -> {
                val pendingAction = pendingAction

                if (
                    permissions.isNotEmpty() &&
                    grantResults.isNotEmpty() &&
                    permissions.first() == Manifest.permission.CAMERA &&
                    grantResults.first() == PackageManager.PERMISSION_GRANTED &&
                    pendingAction != null
                ) {
                    handleAction(pendingAction)
                } else {
                    callback.onNotificationFragmentActionFailed(
                        notification,
                        resources.getString(R.string.notificare_action_camera_failed)
                    )

                    callback.onNotificationFragmentFinished()
                }
            }
            else -> super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == NotificarePendingResult.CAPTURE_IMAGE_REQUEST_CODE || requestCode == NotificarePendingResult.CAPTURE_IMAGE_AND_KEYBOARD_REQUEST_CODE) {
            notificationDialog?.dismissAllowingStateLoss()
            actionsDialog?.dismissAllowingStateLoss()

            if (resultCode == Activity.RESULT_OK && pendingResult?.imageUri != null) {
                handlePendingResult()
            } else if (resultCode == Activity.RESULT_CANCELED) {
                // User cancelled the image capture
                callback.onNotificationFragmentActionCanceled(notification)
                callback.onNotificationFragmentFinished()
            } else {
                // Image capture failed, advise user
                callback.onNotificationFragmentActionFailed(
                    notification,
                    resources.getString(R.string.notificare_action_failed)
                )
                callback.onNotificationFragmentFinished()
            }
        }
    }

    private fun handleAction(action: NotificareNotification.Action) {
        if (action.camera && isCameraPermissionNeeded && !isCameraPermissionGranted) {
            pendingAction = action
            requestPermissions(arrayOf(Manifest.permission.CAMERA), CAMERA_REQUEST_CODE)

            return
        }

        val actionHandler = NotificarePushUI.createActionHandler(requireActivity(), notification, action) ?: run {
            NotificareLogger.debug("Unable to create an action handler for '${action.type}'.")
            return
        }

        GlobalScope.launch(Dispatchers.Main) {
            try {
                callback.onNotificationFragmentStartProgress(notification)

                val result = actionHandler.execute()
                pendingResult = result
                callback.onNotificationFragmentEndProgress(notification)

                val context = context ?: return@launch

                if (result?.requestCode == NotificarePendingResult.CAPTURE_IMAGE_REQUEST_CODE || result?.requestCode == NotificarePendingResult.CAPTURE_IMAGE_AND_KEYBOARD_REQUEST_CODE) {
                    if (result.imageUri != null) {
                        // We need to wait for the image coming back from the camera activity.
                        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                            .addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                            .putExtra(MediaStore.EXTRA_OUTPUT, result.imageUri)

                        startActivityForResult(intent, result.requestCode)
                    } else {
                        callback.onNotificationFragmentActionFailed(
                            notification,
                            context.getString(R.string.notificare_action_camera_failed)
                        )
                    }
                } else if (result?.requestCode == NotificarePendingResult.KEYBOARD_REQUEST_CODE) {
                    // We can show the keyboard right away.
                    notificationDialog?.dismiss()
                    actionsDialog?.dismissAllowingStateLoss()

                    handlePendingResult()
                } else {
                    // No need to wait for results coming from camera activity, just dismiss progress and finish.
                    notificationDialog?.dismiss()
                    actionsDialog?.dismissAllowingStateLoss()

                    callback.onNotificationFragmentActionSucceeded(notification)
                    callback.onNotificationFragmentFinished()
                }
            } catch (e: Exception) {
                if (context != null) {
                    callback.onNotificationFragmentEndProgress(notification)
                    callback.onNotificationFragmentActionFailed(notification, e.localizedMessage)
                }
            }
        }
    }

    private val isCameraPermissionNeeded: Boolean
        get() {
            val packageManager = Notificare.requireContext().packageManager
            val packageName = Notificare.requireContext().packageName

            try {
                val info = packageManager.getPackageInfo(packageName, PackageManager.GET_PERMISSIONS)
                val requestedPermissions = info.requestedPermissions

                if (requestedPermissions != null) {
                    return requestedPermissions.any { it == Manifest.permission.CAMERA }
                }
            } catch (e: PackageManager.NameNotFoundException) {
                NotificareLogger.warning("Failed to read the manifest.", e)
            }

            return false
        }

    private val isCameraPermissionGranted: Boolean
        get() {
            val context = context ?: return false

            return ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        }

    private fun handlePendingResult() {
        val pendingResult = pendingResult ?: run {
            NotificareLogger.debug("No pending result to process.")
            return
        }

        val fragment = NotificareCallbackActionFragment.newInstance(pendingResult)
        childFragmentManager.beginTransaction()
            .replace(binding.notificareNotificationFragmentContainer.id, fragment)
            .commit()
    }

    // region NotificationFragment.Callback

    override fun onNotificationFragmentFinished() {
        callback.onNotificationFragmentFinished()
    }

    override fun onNotificationFragmentShouldShowActionBar() {
        callback.onNotificationFragmentShouldShowActionBar(notification)
    }

    override fun onNotificationFragmentCanHideActionBar() {
        callback.onNotificationFragmentCanHideActionBar(notification)
    }

    override fun onNotificationFragmentCanHideActionsMenu() {
        showActionsMenuItem = false
    }

    override fun onNotificationFragmentStartProgress() {
        callback.onNotificationFragmentStartProgress(notification)
    }

    override fun onNotificationFragmentEndProgress() {
        callback.onNotificationFragmentEndProgress(notification)
    }

    override fun onNotificationFragmentActionCanceled() {
        callback.onNotificationFragmentActionCanceled(notification)
    }

    override fun onNotificationFragmentActionFailed(reason: String) {
        callback.onNotificationFragmentActionFailed(notification, reason)
    }

    override fun onNotificationFragmentActionSucceeded() {
        callback.onNotificationFragmentActionSucceeded(notification)
    }

    override fun onNotificationFragmentShowActions() {
        // TODO show alert dialog
    }

    override fun onNotificationFragmentHandleAction(action: NotificareNotification.Action) {
        // TODO handle action
    }

    override fun onNotificationFragmentStartActivity(intent: Intent) {
        try {
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            NotificareLogger.warning("No activity found to handle intent.", e)
        }
    }

    // endregion

    // region NotificationDialog.Callback

    override fun onNotificationDialogOkClick() {
        NotificareLogger.debug("User clicked the OK button.")
    }

    override fun onNotificationDialogCancelClick() {
        NotificareLogger.debug("User clicked the cancel button.")
    }

    override fun onNotificationDialogDismiss() {
        NotificareLogger.debug("User dismissed the dialog.")
        // if (notification.getType().equals(NotificareNotification.TYPE_ALERT) || notification.getType().equals(NotificareNotification.TYPE_PASSBOOK)) {
        if (pendingResult == null) {
            callback.onNotificationFragmentFinished()
        }
    }

    override fun onNotificationDialogActionClick(position: Int) {
        NotificareLogger.debug("User clicked on action index $position.")

        if (position >= notification.actions.size) {
            // This is the cancel button
            callback.onNotificationFragmentFinished()
        } else {
            // Perform the action
            handleAction(notification.actions[position])
        }
    }

    override fun onNotificationDialogOpenPassbookClick() {
        // TODO
    }

    // endregion

    companion object {
        private const val CAMERA_REQUEST_CODE = 1

        fun newInstance(
            notification: NotificareNotification,
            action: NotificareNotification.Action?
        ): NotificationContainerFragment {
            return NotificationContainerFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(Notificare.INTENT_EXTRA_NOTIFICATION, notification)
                    putParcelable(Notificare.INTENT_EXTRA_ACTION, action)
                }
            }
        }
    }

    interface Callback {
        fun onNotificationFragmentFinished()

        fun onNotificationFragmentShouldShowActionBar(notification: NotificareNotification)

        fun onNotificationFragmentCanHideActionBar(notification: NotificareNotification)

        fun onNotificationFragmentStartProgress(notification: NotificareNotification)

        fun onNotificationFragmentEndProgress(notification: NotificareNotification)

        fun onNotificationFragmentActionCanceled(notification: NotificareNotification)

        fun onNotificationFragmentActionFailed(
            notification: NotificareNotification,
            reason: String?
        )

        fun onNotificationFragmentActionSucceeded(notification: NotificareNotification)
    }
}
