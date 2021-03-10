package re.notifica.push.ui.notifications

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import re.notifica.Notificare
import re.notifica.NotificareLogger
import re.notifica.models.NotificareNotification
import re.notifica.push.ui.NotificarePushUI
import re.notifica.push.ui.R
import re.notifica.push.ui.databinding.NotificareNotificationContainerFragmentBinding
import re.notifica.push.ui.notifications.fragments.base.NotificationFragment

class NotificationContainerFragment
    : Fragment(), NotificationFragment.Callback, NotificationDialog.Callback {

    private lateinit var binding: NotificareNotificationContainerFragmentBinding
    private lateinit var notification: NotificareNotification
    private lateinit var callback: Callback

    private var pendingAction: NotificareNotification.Action? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        notification = savedInstanceState?.getParcelable(Notificare.INTENT_EXTRA_NOTIFICATION)
            ?: arguments?.getParcelable(Notificare.INTENT_EXTRA_NOTIFICATION)
                ?: throw IllegalArgumentException("Missing required notification parameter.")

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
        // TODO action == null && !notification.getType().equals(NotificareNotification.TYPE_ALERT) && !notification.getType().equals(NotificareNotification.TYPE_PASSBOOK) && notification.getActions().size() > 0
        // setHasOptionsMenu()

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

        // TODO action == null &&
        if (type == NotificareNotification.NotificationType.ALERT || type == NotificareNotification.NotificationType.PASSBOOK) {
            callback.onNotificationFragmentCanHideActionBar(notification)
            // TODO keep an instance variable
            NotificationDialog.newInstance(notification)
                .show(childFragmentManager, "dialog")
        } else {
            callback.onNotificationFragmentShouldShowActionBar(notification)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable(Notificare.INTENT_EXTRA_NOTIFICATION, notification)
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

    private fun handleAction(action: NotificareNotification.Action) {
        if (action.camera && isCameraPermissionNeeded && !isCameraPermissionGranted) {
            pendingAction = action
            requestPermissions(arrayOf(Manifest.permission.CAMERA), CAMERA_REQUEST_CODE)

            return
        }

        callback.onNotificationFragmentStartProgress(notification)
        // TODO
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
        // TODO
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
        //        if (notification.getType().equals(NotificareNotification.TYPE_ALERT) || notification.getType().equals(NotificareNotification.TYPE_PASSBOOK)) {
//            if (callback != null && pendingResult == null) {

        callback.onNotificationFragmentFinished()
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

        fun newInstance(notification: NotificareNotification): NotificationContainerFragment {
            return NotificationContainerFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(Notificare.INTENT_EXTRA_NOTIFICATION, notification)
                    //putParcelable(NotificarePush.INTENT_EXTRA_ACTION, action)
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
