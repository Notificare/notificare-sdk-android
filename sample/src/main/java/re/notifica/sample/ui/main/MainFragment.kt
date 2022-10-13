package re.notifica.sample.ui.main

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import re.notifica.Notificare
import re.notifica.inbox.ktx.inbox
import re.notifica.models.NotificareApplication
import re.notifica.models.NotificareDoNotDisturb
import re.notifica.models.NotificareTime
import re.notifica.sample.R
import re.notifica.sample.databinding.FragmentMainBinding
import re.notifica.sample.ktx.LocationPermission
import re.notifica.sample.ktx.showBasicAlert
import re.notifica.sample.models.BaseFragment
import timber.log.Timber

class MainFragment : BaseFragment(), Notificare.Listener {
    private val viewModel: MainViewModel by viewModels()
    private lateinit var binding: FragmentMainBinding

    override val baseViewModel: MainViewModel by viewModels()

    private val notificationsPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            binding.notificationsCard.notificationsSwitch.isChecked = false
            return@registerForActivityResult
        }

        viewModel.changeRemoteNotifications(enabled = true)
    }

    private val foregroundLocationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.all { it.value }

        if (granted) {
            return@registerForActivityResult enableLocationUpdates()
        }

        // Enables location updates with whatever capabilities have been granted so far.
        viewModel.changeLocationUpdates(enabled = true)
    }

    private val backgroundLocationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            return@registerForActivityResult enableLocationUpdates()
        }

        // Enables location updates with whatever capabilities have been granted so far.
        viewModel.changeLocationUpdates(enabled = true)
    }

    private val bluetoothScanLocationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            return@registerForActivityResult enableLocationUpdates()
        }

        // Enables location updates with whatever capabilities have been granted so far.
        viewModel.changeLocationUpdates(enabled = true)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycle.addObserver(viewModel)
        Notificare.addListener(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        lifecycle.removeObserver(viewModel)
        Notificare.removeListener(this)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentMainBinding.inflate(inflater, container, false)

        setupListeners()
        setupObservers()

        return binding.root
    }

    override fun onReady(application: NotificareApplication) {
        viewModel.updateNotificareReady()
    }

    override fun onUnlaunched() {
        viewModel.updateNotificareReady()
    }

    private fun setupListeners() {
        binding.launchCard.launchButton.setOnClickListener {
            Notificare.launch()
        }
        binding.launchCard.unlaunchButton.setOnClickListener {
            Notificare.unlaunch()
        }

        binding.notificationsCard.notificationsSwitch.setOnCheckedChangeListener { _, checked ->
            if (checked == viewModel.notificationsEnabled.value) return@setOnCheckedChangeListener

            if (checked) {
                enableRemoteNotifications()
            } else {
                viewModel.changeRemoteNotifications(enabled = false)
            }
        }
        binding.notificationsCard.tagsCard.setOnClickListener {
            findNavController().navigate(R.id.action_mainFragment_to_tagsFragment)
        }
        binding.notificationsCard.inboxCard.setOnClickListener {
            findNavController().navigate(R.id.action_mainFragment_to_inboxFragment)
        }

        binding.dndCard.dndSwitch.setOnCheckedChangeListener { _, checked ->
            if (checked == viewModel.dndEnabled.value) return@setOnCheckedChangeListener
            viewModel.changeDoNotDisturbEnabled(enabled = checked)
        }

        binding.dndCard.dndStartContainer.setOnClickListener {
            val dnd = viewModel.dnd.value ?: return@setOnClickListener

            val picker = MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_24H)
                .setHour(dnd.start.hours)
                .setMinute(dnd.start.minutes)
                .build()

            picker.addOnPositiveButtonClickListener {
                viewModel.changeDoNotDisturb(
                    NotificareDoNotDisturb(
                        start = NotificareTime(picker.hour, picker.minute),
                        end = dnd.end,
                    )
                )
            }

            picker.show(childFragmentManager, "time-picker")
        }

        binding.dndCard.dndEndContainer.setOnClickListener {
            val dnd = viewModel.dnd.value ?: return@setOnClickListener

            val picker = MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_24H)
                .setHour(dnd.end.hours)
                .setMinute(dnd.end.minutes)
                .build()

            picker.addOnPositiveButtonClickListener {
                viewModel.changeDoNotDisturb(
                    NotificareDoNotDisturb(
                        start = dnd.start,
                        end = NotificareTime(picker.hour, picker.minute),
                    )
                )
            }

            picker.show(childFragmentManager, "time-picker")
        }

        binding.locationCard.locationSwitch.setOnCheckedChangeListener { _, checked ->
            if (checked == viewModel.hasLocationUpdatesEnabled.value) return@setOnCheckedChangeListener

            if (checked) {
                enableLocationUpdates()
            } else {
                viewModel.changeLocationUpdates(enabled = false)
            }
        }
        binding.locationCard.beaconsCard.setOnClickListener {
            if (viewModel.locationPermission.value != LocationPermission.BACKGROUND) {
                showBasicAlert(requireContext(), "Background location permission is needed to search for beacons")

                return@setOnClickListener
            }

            if (viewModel.hasBluetoothPermission.value != true) {
                showBasicAlert(requireContext(), "Background location permission is needed to search for beacons")

                return@setOnClickListener
            }

            findNavController().navigate(R.id.action_mainFragment_to_beaconsFragment)
        }

        binding.iamCard.iamSwitch.setOnCheckedChangeListener { _, checked ->
            if (checked == viewModel.iamSuppressed.value) return@setOnCheckedChangeListener

            viewModel.changeIamSuppressed(checked)
        }

        binding.deviceRegistrationCard.registerUserButton.setOnClickListener {
            val id = binding.deviceRegistrationCard.userIdInput.editText?.text
            val name = binding.deviceRegistrationCard.userNameInput.editText?.text

            if (!id.isNullOrEmpty()) {
                viewModel.registerDevice(null, null)
                return@setOnClickListener
            }

            viewModel.registerDevice(id.toString(), name.toString())
        }

        binding.otherFeaturesCard.scannablesCard.setOnClickListener {
            findNavController().navigate(R.id.action_mainFragment_to_scannablesFragment)
        }

        binding.otherFeaturesCard.monetizeCard.setOnClickListener {
            findNavController().navigate(R.id.action_mainFragment_to_monetizeFragment)
        }

        binding.otherFeaturesCard.assetsCard.setOnClickListener {
            findNavController().navigate(R.id.action_mainFragment_to_assetsFragment)
        }

        binding.otherFeaturesCard.eventsCard.setOnClickListener {
            findNavController().navigate(R.id.action_mainFragment_to_eventsFragment)
        }

        binding.otherFeaturesCard.authenticationCard.setOnClickListener {
            findNavController().navigate(R.id.action_mainFragment_to_authenticationFragment)
        }
    }

    private fun setupObservers() {
        Notificare.inbox().observableBadge.observe(viewLifecycleOwner) { badge ->
            binding.notificationsCard.inboxBadgeLabel.isVisible = badge > 0
            binding.notificationsCard.inboxBadgeLabel.text = if (badge <= 99) badge.toString() else "99+"
        }

        viewModel.notificareReady.observe(viewLifecycleOwner) { isReady ->
            binding.launchCard.readyStatusLabel.text = isReady.toString()
            binding.launchCard.launchButton.isEnabled = isReady != true
            binding.launchCard.unlaunchButton.isEnabled = isReady == true
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

        viewModel.dndEnabled.observe(viewLifecycleOwner) { enabled ->
            binding.dndCard.dndSwitch.isChecked = enabled
            binding.dndCard.dndStartContainer.isVisible = enabled
            binding.dndCard.dndEndContainer.isVisible = enabled
        }
        viewModel.dnd.observe(viewLifecycleOwner) { dnd ->
            binding.dndCard.dndStartLabel.text = dnd.start.format()
            binding.dndCard.dndEndLabel.text = dnd.end.format()
        }

        viewModel.hasLocationUpdatesEnabled.observe(viewLifecycleOwner) { enabled ->
            binding.locationCard.locationSwitch.isChecked = enabled
            binding.locationCard.locationEnabledStatusLabel.text = enabled.toString()
        }
        viewModel.locationPermission.observe(viewLifecycleOwner) { permission ->
            binding.locationCard.locationPermissionTypeStatusLabel.text = permission.name
        }
        viewModel.hasBluetoothEnabled.observe(viewLifecycleOwner) { enabled ->
            binding.locationCard.locationBluetoothStatusLabel.text = enabled.toString()
        }
        viewModel.hasBluetoothPermission.observe(viewLifecycleOwner) { granted ->
            binding.locationCard.locationBluetoothPermissionStatus.text = granted.toString()
        }

        viewModel.iamSuppressed.observe(viewLifecycleOwner) { suppressed ->
            binding.iamCard.iamSwitch.isChecked = suppressed
        }

        viewModel.deviceRegistrationData.observe(viewLifecycleOwner) { deviceData ->
            if (deviceData != null) {
                if (deviceData.userId != null) {
                    binding.deviceRegistrationCard.userIdInput.editText?.setText(deviceData.userId)
                    binding.deviceRegistrationCard.userNameInput.editText?.setText(deviceData.userName)
                    return@observe
                }
            }
        }
    }

    private fun enableRemoteNotifications() {
        if (!ensureNotificationsPermission()) return
        viewModel.changeRemoteNotifications(enabled = true)
    }

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

    private fun enableLocationUpdates() {
        if (!ensureForegroundLocationPermission()) return
        if (!ensureBackgroundLocationPermission()) return
        if (!ensureBluetoothScanPermission()) return

        viewModel.changeLocationUpdates(enabled = true)
    }

    private fun ensureForegroundLocationPermission(): Boolean {
        val permission = Manifest.permission.ACCESS_FINE_LOCATION
        val granted = ContextCompat.checkSelfPermission(
            requireContext(),
            permission
        ) == PackageManager.PERMISSION_GRANTED

        // We already have been granted the requested permission. Move forward...
        if (granted) return true

        if (shouldShowRequestPermissionRationale(permission)) {
            AlertDialog.Builder(requireContext())
                .setTitle(R.string.app_name)
                .setMessage(R.string.permission_foreground_location_rationale)
                .setCancelable(false)
                .setPositiveButton(R.string.dialog_ok_button) { _, _ ->
                    Timber.d("Requesting foreground location permission.")
                    foregroundLocationPermissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        )
                    )
                }
                .setNegativeButton(R.string.dialog_cancel_button) { _, _ ->
                    Timber.d("Foreground location permission rationale cancelled.")

                    // Enables location updates with whatever capabilities have been granted so far.
                    viewModel.changeLocationUpdates(enabled = true)
                }
                .show()

            return false
        }

        Timber.d("Requesting foreground location permission.")
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

        val granted = ContextCompat.checkSelfPermission(
            requireContext(),
            permission
        ) == PackageManager.PERMISSION_GRANTED

        if (granted) return true

        if (shouldShowRequestPermissionRationale(permission)) {
            AlertDialog.Builder(requireContext())
                .setTitle(R.string.app_name)
                .setMessage(R.string.permission_background_location_rationale)
                .setCancelable(false)
                .setPositiveButton(R.string.dialog_ok_button) { _, _ ->
                    Timber.d("Requesting background location permission.")
                    backgroundLocationPermissionLauncher.launch(permission)
                }
                .setNegativeButton(R.string.dialog_cancel_button) { _, _ ->
                    Timber.d("Background location permission rationale cancelled.")

                    // Enables location updates with whatever capabilities have been granted so far.
                    viewModel.changeLocationUpdates(enabled = true)
                }
                .show()

            return false
        }

        Timber.d("Requesting background location permission.")
        backgroundLocationPermissionLauncher.launch(permission)

        return false
    }

    private fun ensureBluetoothScanPermission(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true

        val permission = Manifest.permission.BLUETOOTH_SCAN
        val granted = ContextCompat.checkSelfPermission(
            requireContext(),
            permission
        ) == PackageManager.PERMISSION_GRANTED

        // We already have been granted the requested permission. Move forward...
        if (granted) return true

        if (shouldShowRequestPermissionRationale(permission)) {
            AlertDialog.Builder(requireContext())
                .setTitle(R.string.app_name)
                .setMessage(R.string.permission_bluetooth_scan_rationale)
                .setCancelable(false)
                .setPositiveButton(R.string.dialog_ok_button) { _, _ ->
                    Timber.d("Requesting bluetooth scan permission.")
                    bluetoothScanLocationPermissionLauncher.launch(permission)
                }
                .setNegativeButton(R.string.dialog_cancel_button) { _, _ ->
                    Timber.d("Bluetooth scan permission rationale cancelled.")

                    // Enables location updates with whatever capabilities have been granted so far.
                    viewModel.changeLocationUpdates(enabled = true)
                }
                .show()

            return false
        }

        Timber.d("Requesting bluetooth scan permission.")
        bluetoothScanLocationPermissionLauncher.launch(permission)

        return false
    }
}
