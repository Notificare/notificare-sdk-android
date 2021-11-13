package re.notifica.push.ui.notifications

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.MediaStore
import android.view.*
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import re.notifica.Notificare
import re.notifica.internal.NotificareLogger
import re.notifica.models.NotificareNotification
import re.notifica.push.ui.R
import re.notifica.push.ui.databinding.NotificareNotificationContainerFragmentBinding
import re.notifica.push.ui.ktx.pushUIImplementation
import re.notifica.push.ui.ktx.pushUIInternal
import re.notifica.push.ui.models.NotificarePendingResult
import re.notifica.push.ui.notifications.fragments.NotificareCallbackActionFragment
import re.notifica.push.ui.notifications.fragments.base.NotificationFragment

public class NotificationContainerFragment
    : Fragment(), NotificationFragment.Callback, NotificationDialog.Callback, NotificationActionsDialog.Callback {

    private lateinit var binding: NotificareNotificationContainerFragmentBinding
    private lateinit var notification: NotificareNotification
    private var action: NotificareNotification.Action? = null
    private lateinit var callback: Callback

    private var pendingAction: NotificareNotification.Action? = null
    private var pendingResult: NotificarePendingResult? = null

    private var notificationDialog: NotificationDialog? = null
    private var actionsDialog: NotificationActionsDialog? = null

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

        if (notification.type != NotificareNotification.TYPE_ALERT && notification.actions.isNotEmpty()) {
            setHasOptionsMenu(true)
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
        val fragmentClassName = Notificare.pushUIImplementation().getFragmentCanonicalClassName(notification)

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

        if (action == null && type == NotificareNotification.NotificationType.ALERT) {
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

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.notificare_menu_notification_fragment, menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        menu.findItem(R.id.notificare_action_show_actions).isVisible = showActionsMenuItem
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.notificare_action_show_actions -> {
                showActionsDialog()
                return true
            }
        }

        return super.onOptionsItemSelected(item)
    }

    private fun showActionsDialog() {
        NotificationActionsDialog.newInstance(notification)
            .also { actionsDialog = it }
            .show(childFragmentManager, "actionDialog")
    }

    private fun handleAction(action: NotificareNotification.Action) {
        if (action.camera && isCameraPermissionNeeded && !isCameraPermissionGranted) {
            pendingAction = action
            requestPermissions(arrayOf(Manifest.permission.CAMERA), CAMERA_REQUEST_CODE)

            return
        }

        val actionHandler = Notificare.pushUIImplementation().createActionHandler(
            activity = requireActivity(),
            notification = notification,
            action = action,
        ) ?: run {
            NotificareLogger.debug("Unable to create an action handler for '${action.type}'.")
            return
        }

        lifecycleScope.launch {
            try {
                callback.onNotificationFragmentStartProgress(notification)

                Notificare.pushUIInternal().lifecycleListeners.forEach { it.onActionWillExecute(notification, action) }

                val result = actionHandler.execute()
                pendingResult = result
                callback.onNotificationFragmentEndProgress(notification)

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
                            requireContext().getString(R.string.notificare_action_camera_failed)
                        )

                        val error = Exception(requireContext().getString(R.string.notificare_action_camera_failed))
                        Notificare.pushUIInternal().lifecycleListeners.forEach {
                            it.onActionFailedToExecute(
                                notification,
                                action,
                                error
                            )
                        }
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
                callback.onNotificationFragmentEndProgress(notification)
                callback.onNotificationFragmentActionFailed(notification, e.localizedMessage)

                Notificare.pushUIInternal().lifecycleListeners.forEach {
                    it.onActionFailedToExecute(notification, action, e)
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
        activity?.invalidateOptionsMenu()
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
        showActionsDialog()
    }

    override fun onNotificationFragmentHandleAction(action: NotificareNotification.Action) {
        handleAction(action)
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

    // endregion

    // region NotificationActionsDialog.Callback

    override fun onActionDialogActionClick(which: Int) {
        handleAction(notification.actions[which])
    }

    override fun onActionDialogCancelClick() {
        NotificareLogger.debug("Action dialog canceled.")
    }

    override fun onActionDialogCloseClick() {
        callback.onNotificationFragmentFinished()
    }

    // endregion

    public companion object {
        private const val CAMERA_REQUEST_CODE = 1

        public fun newInstance(
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

    public interface Callback {
        public fun onNotificationFragmentFinished()

        public fun onNotificationFragmentShouldShowActionBar(notification: NotificareNotification)

        public fun onNotificationFragmentCanHideActionBar(notification: NotificareNotification)

        public fun onNotificationFragmentStartProgress(notification: NotificareNotification)

        public fun onNotificationFragmentEndProgress(notification: NotificareNotification)

        public fun onNotificationFragmentActionCanceled(notification: NotificareNotification)

        public fun onNotificationFragmentActionFailed(
            notification: NotificareNotification,
            reason: String?
        )

        public fun onNotificationFragmentActionSucceeded(notification: NotificareNotification)
    }
}
