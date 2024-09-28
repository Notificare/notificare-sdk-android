package re.notifica.sample.user.inbox.ui.main

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import kotlinx.coroutines.launch
import re.notifica.Notificare
import re.notifica.models.NotificareApplication
import re.notifica.models.NotificareDoNotDisturb
import re.notifica.models.NotificareTime
import re.notifica.push.ktx.push
import re.notifica.sample.user.inbox.BuildConfig
import re.notifica.sample.user.inbox.R
import re.notifica.sample.user.inbox.core.BaseFragment
import re.notifica.sample.user.inbox.core.NotificationEvent
import re.notifica.sample.user.inbox.databinding.FragmentMainBinding
import timber.log.Timber

internal class MainFragment : BaseFragment(), Notificare.Listener {
    private val pendingRationales = mutableListOf<PermissionType>()
    private val viewModel: MainViewModel by viewModels()
    private lateinit var binding: FragmentMainBinding

    override val baseViewModel: MainViewModel by viewModels()
    // Permission Launcher

    private val notificationsPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.updateRemoteNotificationsStatus(true)
            return@registerForActivityResult
        }

        if (shouldOpenSettings(PermissionType.NOTIFICATIONS)) {
            showSettingsPrompt(PermissionType.NOTIFICATIONS)
            return@registerForActivityResult
        }

        binding.notificationsCard.notificationsSwitch.isChecked = false
    }

    private val openNotificationsSettingsLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (binding.notificationsCard.notificationsSwitch.isChecked) {
            if (NotificationManagerCompat.from(requireContext().applicationContext).areNotificationsEnabled()) {
                if (!Notificare.push().hasRemoteNotificationsEnabled) {
                    viewModel.updateRemoteNotificationsStatus(true)
                }
            } else {
                binding.notificationsCard.notificationsSwitch.isChecked = false
            }
        }
    }

    // End Permission Launcher section

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycle.addObserver(viewModel)

        viewModel.setUserInboxURLs(
            base = requireContext().getString(R.string.user_inbox_base_url),
            registerDevice = requireContext().getString(R.string.user_inbox_register_device_url),
            fetchInbox = requireContext().getString(R.string.user_inbox_fetch_inbox_url)
        )

        Notificare.addListener(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        lifecycle.removeObserver(viewModel)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentMainBinding.inflate(inflater, container, false)

        setupListeners()
        setupObservers()

        if (Notificare.isReady && viewModel.deviceRegistered.value != true) {
            viewModel.startAutoLoginFlow(requireContext(), account)
        }

        lifecycleScope.launch {
            NotificationEvent.inboxShouldUpdateFlow.collect {
                viewModel.refreshBadge(requireContext(), account)
            }
        }

        return binding.root
    }

    override fun onReady(application: NotificareApplication) {
        if (viewModel.deviceRegistered.value != true) {
            viewModel.startAutoLoginFlow(requireContext(), account)
        }
    }

    private fun setupListeners() {
        // Authentication flow

        binding.authenticationCard.loginButton.setOnClickListener {
            viewModel.startLoginFLow(requireContext(), account)
        }

        binding.authenticationCard.logoutButton.setOnClickListener {
            viewModel.startLogoutFlow(requireContext(), account)
        }

        // Launch flow

        binding.launchCard.launchButton.setOnClickListener {
            lifecycleScope.launch {
                Notificare.launch()
            }
        }

        binding.launchCard.unlaunchButton.setOnClickListener {
            lifecycleScope.launch {
                Notificare.unlaunch()
            }
        }

        // End region

        // Notifications card

        binding.notificationsCard.notificationsSwitch.setOnCheckedChangeListener { _, checked ->
            if (checked == viewModel.notificationsEnabled.value) return@setOnCheckedChangeListener

            if (checked) {
                enableRemoteNotifications()
            } else {
                viewModel.updateRemoteNotificationsStatus(false)
            }
        }

        binding.notificationsCard.inboxRow.setOnClickListener {
            findNavController().navigate(R.id.action_mainFragment_to_inboxFragment)
        }

        // End region

        // Do not disturb

        binding.dndCard.dndSwitch.setOnCheckedChangeListener { _, checked ->
            if (checked == viewModel.dndEnabled.value) return@setOnCheckedChangeListener
            viewModel.updateDndStatus(checked)
        }

        binding.dndCard.dndStartTimeContainer.setOnClickListener {
            val dnd = viewModel.dnd.value ?: return@setOnClickListener

            val timePicker = MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_24H)
                .setHour(dnd.start.hours)
                .setMinute(dnd.start.minutes)
                .build()

            timePicker.addOnPositiveButtonClickListener {
                viewModel.updateDndTime(
                    NotificareDoNotDisturb(
                        start = NotificareTime(timePicker.hour, timePicker.minute),
                        end = dnd.end,
                    )
                )
            }

            timePicker.show(childFragmentManager, "time-picker")
        }

        binding.dndCard.dndEndTimeContainer.setOnClickListener {
            val dnd = viewModel.dnd.value ?: return@setOnClickListener

            val timePicker = MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_24H)
                .setHour(dnd.end.hours)
                .setMinute(dnd.end.minutes)
                .build()

            timePicker.addOnPositiveButtonClickListener {
                viewModel.updateDndTime(
                    NotificareDoNotDisturb(
                        start = dnd.start,
                        end = NotificareTime(timePicker.hour, timePicker.minute),
                    )
                )
            }

            timePicker.show(childFragmentManager, "time-picker")
        }

        // End region
    }

    private fun setupObservers() {
        viewModel.isLoggedIn.observe(viewLifecycleOwner) { isLoggedIn ->
            binding.authenticationCard.loggedInStatusLabel.text = isLoggedIn.toString()
            binding.authenticationCard.loginButton.isEnabled = !isLoggedIn
            binding.authenticationCard.logoutButton.isEnabled = isLoggedIn
        }

        viewModel.deviceRegistered.observe(viewLifecycleOwner) { isRegistered ->
            binding.authenticationCard.deviceRegisteredStatusLabel.text = isRegistered.toString()
        }

        viewModel.notificareReady.observe(viewLifecycleOwner) { isReady ->
            binding.launchCard.readyStatusLabel.text = isReady.toString()
            binding.launchCard.launchButton.isEnabled = !isReady
            binding.launchCard.unlaunchButton.isEnabled = isReady
        }

        viewModel.notificareConfigured.observe(viewLifecycleOwner) { isConfigured ->
            binding.launchCard.configuredStatusLabel.text = isConfigured.toString()
        }

        viewModel.notificationsEnabled.observe(viewLifecycleOwner) { enabled ->
            binding.notificationsCard.notificationsSwitch.isChecked = enabled
        }

        viewModel.remoteNotificationsEnabled.observe(viewLifecycleOwner) { enabled ->
            binding.notificationsCard.notificationsEnabledStatusLabel.text = enabled.toString()
        }

        viewModel.notificationsAllowedUI.observe(viewLifecycleOwner) { enabled ->
            binding.notificationsCard.notificationsAllowedUiStatusLabel.text = enabled.toString()
        }

        viewModel.hasNotificationsPermissions.observe(viewLifecycleOwner) { granted ->
            binding.notificationsCard.notificationsPermissionStatusLabel.text = granted.toString()
        }

        viewModel.badge.observe(viewLifecycleOwner) { badge ->
            binding.notificationsCard.inboxBadgeLabel.isVisible = badge > 0
            binding.notificationsCard.inboxBadgeLabel.text = if (badge <= 99) badge.toString() else "99+"
        }

        viewModel.dndEnabled.observe(viewLifecycleOwner) { enabled ->
            binding.dndCard.dndSwitch.isChecked = enabled
            binding.dndCard.dndStartTimeContainer.isVisible = enabled
            binding.dndCard.dndEndTimeContainer.isVisible = enabled
        }
        viewModel.dnd.observe(viewLifecycleOwner) { dnd ->
            binding.dndCard.dndStartLabel.text = dnd.start.format()
            binding.dndCard.dndEndLabel.text = dnd.end.format()
        }

        viewModel.applicationInfo.observe(viewLifecycleOwner) { info ->
            binding.applicationInfoCard.root.isVisible = info != null

            if (info != null) {
                binding.applicationInfoCard.nameStatusLabel.text = "${info.name} (${BuildConfig.VERSION_CODE})"
                binding.applicationInfoCard.identifierStatusLabel.text = info.identifier
            }
        }
    }

    private fun enableRemoteNotifications() {
        if (!ensureNotificationsPermission()) return

        if (!NotificationManagerCompat.from(requireContext().applicationContext).areNotificationsEnabled()) {
            // Only runs on Android 12 or lower.
            if (shouldOpenSettings(PermissionType.NOTIFICATIONS)) {
                showSettingsPrompt(PermissionType.NOTIFICATIONS)
            }

            return
        }

        viewModel.updateRemoteNotificationsStatus(true)
    }

    // Permissions Request

    private fun ensureNotificationsPermission(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true

        val permission = Manifest.permission.POST_NOTIFICATIONS
        val granted = ContextCompat.checkSelfPermission(
            requireContext(),
            permission
        ) == PackageManager.PERMISSION_GRANTED

        if (granted) return true

        if (shouldShowRequestPermissionRationale(permission)) {
            AlertDialog.Builder(requireContext())
                .setTitle(R.string.app_name)
                .setMessage(R.string.permission_notifications_rationale)
                .setCancelable(false)
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    Timber.d("Requesting notifications permission.")
                    pendingRationales.add(PermissionType.NOTIFICATIONS)
                    notificationsPermissionLauncher.launch(permission)
                }
                .setNegativeButton(R.string.dialog_cancel_button) { _, _ ->
                    Timber.d("Notifications permission rationale cancelled.")
                    binding.notificationsCard.notificationsSwitch.isChecked = false
                }
                .show()

            return false
        }

        Timber.d("Requesting notifications permission.")
        notificationsPermissionLauncher.launch(permission)

        return false
    }

    // End region

    // Open settings region

    private fun shouldOpenSettings(permissionType: PermissionType): Boolean {
        val permission = when (permissionType) {
            PermissionType.NOTIFICATIONS -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    Manifest.permission.POST_NOTIFICATIONS
                } else {
                    null
                }
            }
        }

        if (permission != null) {
            if (!shouldShowRequestPermissionRationale(permission) && pendingRationales.contains(permissionType)) {
                pendingRationales.remove(permissionType)
                return false
            }

            if (shouldShowRequestPermissionRationale(permission) && pendingRationales.contains(permissionType)) {
                pendingRationales.remove(permissionType)
                return false
            }

            if (shouldShowRequestPermissionRationale(permission) && !pendingRationales.contains(permissionType)) {
                return false
            }
        }

        return true
    }

    private fun showSettingsPrompt(permissionType: PermissionType) {
        AlertDialog.Builder(requireContext()).setTitle(R.string.app_name)
            .setMessage(R.string.permission_open_os_settings_rationale)
            .setCancelable(false)
            .setPositiveButton(R.string.dialog_ok_button) { _, _ ->
                Timber.d("Opening OS Settings")
                when (permissionType) {
                    re.notifica.sample.user.inbox.ui.main.MainFragment.PermissionType.NOTIFICATIONS -> {
                        openNotificationsSettingsLauncher.launch(
                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.fromParts("package", requireContext().packageName, null)
                            }
                        )
                    }
                }
            }
            .setNegativeButton(R.string.dialog_cancel_button) { _, _ ->
                Timber.d("Redirect to OS Settings cancelled")
                when (permissionType) {
                    PermissionType.NOTIFICATIONS -> binding.notificationsCard.notificationsSwitch.isChecked = false
                }
            }
            .show()
    }

    enum class PermissionType {
        NOTIFICATIONS
    }
}
