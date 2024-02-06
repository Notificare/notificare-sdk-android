package re.notifica.sample.ui.main

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
import androidx.navigation.fragment.findNavController
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import re.notifica.Notificare
import re.notifica.geo.ktx.geo
import re.notifica.inbox.ktx.inbox
import re.notifica.models.NotificareDoNotDisturb
import re.notifica.models.NotificareTime
import re.notifica.push.ktx.push
import re.notifica.sample.R
import re.notifica.sample.databinding.FragmentMainBinding
import re.notifica.sample.ktx.LocationPermission
import re.notifica.sample.ktx.showBasicAlert
import re.notifica.sample.live_activities.models.CoffeeBrewingState
import re.notifica.sample.core.BaseFragment
import timber.log.Timber

class MainFragment : BaseFragment() {
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

    private val foregroundLocationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.all { it.value }

        if (granted) {
            enableLocationUpdates()
            return@registerForActivityResult
        }

        if (shouldOpenSettings(PermissionType.LOCATION)) {
            showSettingsPrompt(PermissionType.LOCATION)
            return@registerForActivityResult
        }

        binding.locationCard.locationSwitch.isChecked = false
    }

    private val backgroundLocationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            enableLocationUpdates()
            return@registerForActivityResult
        }

        // Enables location updates with whatever capabilities have been granted so far.
        viewModel.updateLocationUpdatesStatus(true)
    }

    private val bluetoothScanPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            enableLocationUpdates()
            return@registerForActivityResult
        }

        // Enables location updates with whatever capabilities have been granted so far.
        viewModel.updateLocationUpdatesStatus(true)
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

    private val openLocationSettingsLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (binding.locationCard.locationSwitch.isChecked) {
            val granted = ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

            if (granted) {
                if (!Notificare.geo().hasLocationServicesEnabled) {
                    viewModel.updateLocationUpdatesStatus(true)
                }
            } else {
                binding.locationCard.locationSwitch.isChecked = false
            }
        }
    }

    // End Permission Launcher section

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycle.addObserver(viewModel)
    }

    override fun onDestroy() {
        super.onDestroy()
        lifecycle.removeObserver(viewModel)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentMainBinding.inflate(inflater, container, false)

        setupListeners()
        setupObservers()

        return binding.root
    }

    private fun setupListeners() {

        // Launch flow

        binding.launchCard.launchButton.setOnClickListener {
            Notificare.launch()
        }

        binding.launchCard.unlaunchButton.setOnClickListener {
            Notificare.unlaunch()
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

        binding.notificationsCard.tagsRow.setOnClickListener {
            findNavController().navigate(R.id.action_mainFragment_to_tagsFragment)
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

        // Live Activities

        binding.coffeeBrewerCard.coffeeBrewerCancelButton.setOnClickListener {
            viewModel.cancelCoffeeSession()
        }

        // End region

        // Location

        binding.locationCard.locationSwitch.setOnCheckedChangeListener { _, checked ->
            if (checked == viewModel.hasLocationUpdatesEnabled.value) return@setOnCheckedChangeListener

            if (checked) {
                enableLocationUpdates()
            } else {
                viewModel.updateLocationUpdatesStatus(false)
            }
        }

        binding.locationCard.beaconsRow.setOnClickListener {
            if (viewModel.locationPermission.value != LocationPermission.BACKGROUND) {
                showBasicAlert(requireContext(), "Background location permission is needed to search for beacons")
                return@setOnClickListener
            }

            if (viewModel.hasBluetoothPermission.value != true) {
                showBasicAlert(requireContext(), "Bluetooth location permission is needed to search for beacons")
                return@setOnClickListener
            }

            findNavController().navigate(R.id.action_mainFragment_to_beaconsFragment)
        }

        // End region

        // In app messaging

        binding.iamCard.evaluateContextSwitch.setOnCheckedChangeListener { _, checked ->
            viewModel.updateIamEvaluateContextStatus(checked)
        }

        binding.iamCard.iamSwitch.setOnCheckedChangeListener { _, checked ->
            if (checked == viewModel.iamSuppressed.value) return@setOnCheckedChangeListener

            viewModel.updateIamSuppressedStatus(checked)
        }

        // End region

        // Device Registration

        binding.deviceRegistrationCard.registerUserButton.setOnClickListener {
            val id = binding.deviceRegistrationCard.userIdInput.editText?.text
            val name = binding.deviceRegistrationCard.userNameInput.editText?.text

            if (id.isNullOrEmpty() || name.isNullOrEmpty()) {
                showBasicAlert(requireContext(), "Please fill the data above.")
                return@setOnClickListener
            }

            viewModel.registerDevice(id.toString(), name.toString())
        }

        binding.deviceRegistrationCard.registerAnonymousButton.setOnClickListener {
            viewModel.registerDevice(null, null)
        }

        // End region

        // Other features

        binding.otherFeaturesCard.scannablesRow.setOnClickListener {
            findNavController().navigate(R.id.action_mainFragment_to_scannablesFragment)
        }

        binding.otherFeaturesCard.monetizeRow.setOnClickListener {
            findNavController().navigate(R.id.action_mainFragment_to_monetizeFragment)
        }

        binding.otherFeaturesCard.assetsRow.setOnClickListener {
            findNavController().navigate(R.id.action_mainFragment_to_assetsFragment)
        }

        binding.otherFeaturesCard.eventsRow.setOnClickListener {
            findNavController().navigate(R.id.action_mainFragment_to_eventsFragment)
        }

        // End region
    }

    private fun setupObservers() {
        Notificare.inbox().observableBadge.observe(viewLifecycleOwner) { badge ->
            binding.notificationsCard.inboxBadgeLabel.isVisible = badge > 0
            binding.notificationsCard.inboxBadgeLabel.text = if (badge <= 99) badge.toString() else "99+"
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

        viewModel.dndEnabled.observe(viewLifecycleOwner) { enabled ->
            binding.dndCard.dndSwitch.isChecked = enabled
            binding.dndCard.dndStartTimeContainer.isVisible = enabled
            binding.dndCard.dndEndTimeContainer.isVisible = enabled
        }
        viewModel.dnd.observe(viewLifecycleOwner) { dnd ->
            binding.dndCard.dndStartLabel.text = dnd.start.format()
            binding.dndCard.dndEndLabel.text = dnd.end.format()
        }

        viewModel.coffeeBrewerUiState.observe(viewLifecycleOwner) { uiState ->
            binding.coffeeBrewerCard.coffeeBrewerButton.isVisible = uiState.brewingState != CoffeeBrewingState.SERVED
            binding.coffeeBrewerCard.coffeeBrewerCancelButton.isVisible = uiState.brewingState != null

            when (uiState.brewingState) {
                null -> {
                    binding.coffeeBrewerCard.coffeeBrewerButton.setText(R.string.main_fragment_coffee_brewer_start)
                    binding.coffeeBrewerCard.coffeeBrewerButton.setOnClickListener {
                        viewModel.createCoffeeSession()
                    }
                }
                CoffeeBrewingState.GRINDING -> {
                    binding.coffeeBrewerCard.coffeeBrewerButton.setText(R.string.main_fragment_coffee_brewer_brew)
                    binding.coffeeBrewerCard.coffeeBrewerButton.setOnClickListener {
                        viewModel.continueCoffeeSession()
                    }
                }
                CoffeeBrewingState.BREWING -> {
                    binding.coffeeBrewerCard.coffeeBrewerButton.setText(R.string.main_fragment_coffee_brewer_serve)
                    binding.coffeeBrewerCard.coffeeBrewerButton.setOnClickListener {
                        viewModel.continueCoffeeSession()
                    }
                }
                CoffeeBrewingState.SERVED -> {}
            }

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
            val userIdInput = binding.deviceRegistrationCard.userIdInput.editText
            val userNameInput = binding.deviceRegistrationCard.userNameInput.editText
            val registerAsUserButton = binding.deviceRegistrationCard.registerUserButton
            val registerAsAnonymousButton = binding.deviceRegistrationCard.registerAnonymousButton

            if (userIdInput == null || userNameInput == null) return@observe

            if (deviceData?.userId != null) {
                userIdInput.setText(deviceData.userId)
                userIdInput.isFocusable = false

                userNameInput.setText(deviceData.userName)
                userNameInput.isFocusable = false

                registerAsUserButton.visibility = View.GONE
                registerAsAnonymousButton.visibility = View.VISIBLE

                return@observe
            }

            userIdInput.setText("")
            userIdInput.isFocusable = true

            userNameInput.setText("")
            userNameInput.isFocusable = true

            registerAsUserButton.visibility = View.VISIBLE
            registerAsAnonymousButton.visibility = View.GONE
        }

        viewModel.applicationInfo.observe(viewLifecycleOwner) { info ->
            binding.applicationInfoCard.nameStatusLabel.text = info.name
            binding.applicationInfoCard.identifierStatusLabel.text = info.identifier
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

    private fun enableLocationUpdates() {
        if (!ensureForegroundLocationPermission()) return
        if (!ensureBackgroundLocationPermission()) return
        if (!ensureBluetoothScanPermission()) return

        viewModel.updateLocationUpdatesStatus(true)
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

    private fun ensureForegroundLocationPermission(): Boolean {
        val permission = Manifest.permission.ACCESS_FINE_LOCATION
        val granted = ContextCompat.checkSelfPermission(
            requireContext(),
            permission
        ) == PackageManager.PERMISSION_GRANTED

        if (granted) return true

        if (shouldShowRequestPermissionRationale(permission)) {
            AlertDialog.Builder(requireContext())
                .setTitle(R.string.app_name)
                .setMessage(R.string.permission_foreground_location_rationale)
                .setCancelable(false)
                .setPositiveButton(R.string.dialog_ok_button) { _, _ ->
                    Timber.d("Requesting foreground location permission.")
                    pendingRationales.add(PermissionType.LOCATION)
                    foregroundLocationPermissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        )
                    )
                }
                .setNegativeButton(R.string.dialog_cancel_button) { _, _ ->
                    Timber.d("Foreground location permission rationale cancelled.")
                    binding.locationCard.locationSwitch.isChecked = false
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
                    viewModel.updateLocationUpdatesStatus(true)
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

        if (granted) return true

        if (shouldShowRequestPermissionRationale(permission)) {
            AlertDialog.Builder(requireContext())
                .setTitle(R.string.app_name)
                .setMessage(R.string.permission_bluetooth_scan_rationale)
                .setCancelable(false)
                .setPositiveButton(R.string.dialog_ok_button) { _, _ ->
                    Timber.d("Requesting bluetooth scan permission.")
                    bluetoothScanPermissionLauncher.launch(permission)
                }
                .setNegativeButton(R.string.dialog_cancel_button) { _, _ ->
                    Timber.d("Bluetooth scan permission rationale cancelled.")

                    // Enables location updates with whatever capabilities have been granted so far.
                    viewModel.updateLocationUpdatesStatus(true)
                }
                .show()

            return false
        }

        Timber.d("Requesting bluetooth scan permission.")
        bluetoothScanPermissionLauncher.launch(permission)

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

            PermissionType.LOCATION -> Manifest.permission.ACCESS_FINE_LOCATION
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
                    PermissionType.NOTIFICATIONS -> {
                        openNotificationsSettingsLauncher.launch(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", requireContext().packageName, null)
                        })
                    }

                    PermissionType.LOCATION -> {
                        openLocationSettingsLauncher.launch(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", requireContext().packageName, null)
                        })
                    }
                }
            }
            .setNegativeButton(R.string.dialog_cancel_button) { _, _ ->
                Timber.d("Redirect to OS Settings cancelled")
                when (permissionType) {
                    PermissionType.NOTIFICATIONS -> binding.notificationsCard.notificationsSwitch.isChecked = false
                    PermissionType.LOCATION -> binding.locationCard.locationSwitch.isChecked = false
                }
            }
            .show()
    }

    // End region

    enum class PermissionType {
        NOTIFICATIONS,
        LOCATION
    }
}
