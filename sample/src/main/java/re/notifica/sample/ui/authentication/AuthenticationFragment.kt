package re.notifica.sample.ui.authentication

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import com.google.android.material.chip.Chip
import re.notifica.Notificare
import re.notifica.authentication.ktx.authentication
import re.notifica.authentication.models.NotificareUserPreference
import re.notifica.sample.R
import re.notifica.sample.databinding.FragmentAuthenticationBinding
import re.notifica.sample.ktx.showBasicAlert
import re.notifica.sample.models.BaseFragment

class AuthenticationFragment : BaseFragment() {
    private lateinit var binding: FragmentAuthenticationBinding
    private val viewModel: AuthenticationViewModel by viewModels()
    override val baseViewModel: AuthenticationViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentAuthenticationBinding.inflate(inflater, container, false)
        binding.passwordRecoverTokenInput.editText?.setText(
            Notificare.authentication().parsePasswordResetToken(requireActivity().intent)
        )
        binding.validateUserTokenInput.editText?.setText(
            Notificare.authentication().parseValidateUserToken(requireActivity().intent)
        )

        setupListeners()
        setupObservers()

        return binding.root
    }

    private fun setupListeners() {
        binding.createUserAccountButton.setOnClickListener {
            val email = binding.createAccountEmailInput.editText?.text
            val password = binding.createAccountPasswordInput.editText?.text
            val name = binding.createAccountUserNameInput.editText?.text

            if (email.isNullOrEmpty() || password.isNullOrEmpty() || name.isNullOrEmpty()) {
                showBasicAlert(requireContext(), "Input data missing")

                return@setOnClickListener
            }

            viewModel.createAccount(email.toString(), password.toString(), name.toString())
        }

        binding.loginButton.setOnClickListener {
            val email = binding.loginAccountEmail.editText?.text
            val password = binding.loginAccountPassword.editText?.text

            if (email.isNullOrEmpty() || password.isNullOrEmpty()) {
                showBasicAlert(requireContext(), "Input data missing")

                return@setOnClickListener
            }

            viewModel.userLogin(email.toString(), password.toString())
        }

        binding.logoutButton.setOnClickListener {
            viewModel.userLogout()
        }

        binding.generatePushEmailButton.setOnClickListener {
            viewModel.generatePushEmail()
        }

        binding.sendPasswordResetButton.setOnClickListener {
            val email = binding.passwordRecoverEmailInput.editText?.text
            if (email.isNullOrEmpty()) {
                showBasicAlert(requireContext(), "Email is missing")

                return@setOnClickListener
            }

            viewModel.sendPasswordReset(email.toString())
        }

        binding.resetPasswordButton.setOnClickListener {
            val password = binding.passwordRecoverNewPasswordInput.editText?.text
            val resetToken = binding.passwordRecoverTokenInput.editText?.text

            if (password.isNullOrEmpty() || resetToken.isNullOrEmpty()) {
                showBasicAlert(requireContext(), "Data missing")

                return@setOnClickListener
            }

            viewModel.resetPassword(password.toString(), resetToken.toString())
        }

        binding.changePasswordButton.setOnClickListener {
            val newPassword = binding.changePasswordInput.editText?.text
            viewModel.changePassword(newPassword.toString())
        }
    }

    private fun setupObservers() {
        viewModel.currentUser.observe(viewLifecycleOwner) { user ->
            val userExist = user?.id
            binding.currentUserNoDataLabel.isVisible = userExist.isNullOrEmpty()

            binding.createUserAccountButton.isEnabled = userExist.isNullOrEmpty()
            binding.validateUserButton.isEnabled = userExist.isNullOrEmpty()
            binding.loginButton.isEnabled = userExist.isNullOrEmpty()
            binding.logoutButton.isEnabled = !userExist.isNullOrEmpty()
            binding.generatePushEmailButton.isEnabled = !userExist.isNullOrEmpty()
            binding.sendPasswordResetButton.isEnabled = userExist.isNullOrEmpty()
            binding.resetPasswordButton.isEnabled = userExist.isNullOrEmpty()
            binding.changePasswordButton.isEnabled = !userExist.isNullOrEmpty()

            if (user == null) {
                binding.currentUserInformation.isVisible = false

                val oldChips = binding.userSegmentsChipGroup.childCount
                if (oldChips > 0) {
                    binding.userSegmentsChipGroup.removeAllViews()
                }

                return@observe
            }

            binding.currentUserIdData.text = user.id
            binding.currentUserNameData.text = user.name
            binding.currentUserPushEmailData.text = user.pushEmailAddress ?: getString(R.string.assets_no_avalable_data)
            binding.currentUserSegmentsData.text = user.segments.toString()
            binding.currentUserRegistrationDateData.text = user.registrationDate.toString()
            binding.currentUserLastActiveData.text = user.lastActive.toString()
            binding.currentUserInformation.isVisible = true

            user.segments.forEach { segmentId ->
                val chip = this.layoutInflater.inflate(R.layout.chip_input, binding.root, false) as Chip
                val segment = viewModel.fetchedSegments.value?.find {
                    it.id == segmentId
                }
                chip.text = segment?.name ?: "No name"
                chip.isCloseIconVisible = true

                chip.setOnClickListener {
                    viewModel.removeUserSegment(segmentId)
                    binding.userSegmentsChipGroup.removeAllViews()
                }

                binding.userSegmentsChipGroup.addView(chip)
            }
        }

        viewModel.fetchedSegments.observe(viewLifecycleOwner) { segments ->
            segments.forEach { segment ->
                val chip = this.layoutInflater.inflate(R.layout.chip_input, binding.root, false) as Chip
                chip.text = segment.name
                chip.chipIcon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_baseline_add_24)

                chip.setOnClickListener {
                    binding.userSegmentsChipGroup.removeAllViews()
                    viewModel.addUserSegment(segment)
                }

                binding.fetchedSegmentsChipGroup.addView(chip)
            }
        }

        viewModel.fetchedPreferences.observe(viewLifecycleOwner) { preferences ->
            var selectedPreference: NotificareUserPreference? = null
            var selectedOption: NotificareUserPreference.Option? = null

            val preferencesLabel = preferences.map { preference ->
                preference.label
            }

            val preferenceAdapter = ArrayAdapter(requireContext(), R.layout.item_preferences_list, preferencesLabel)

            (binding.preferenceChoiceInput.editText as AutoCompleteTextView).setAdapter(preferenceAdapter)
            (binding.preferenceChoiceInput.editText as AutoCompleteTextView).setOnItemClickListener { _, _, choicePosition, _ ->
                selectedPreference = preferences[choicePosition]
                val optionsLabel = preferences[choicePosition].options.map { option ->
                    option.label
                }

                val optionAdapter = ArrayAdapter(requireContext(), R.layout.item_preferences_list, optionsLabel)
                (binding.preferenceOptionChoiceInput.editText as AutoCompleteTextView).setAdapter(optionAdapter)
                (binding.preferenceOptionChoiceInput.editText as AutoCompleteTextView).setOnItemClickListener { _, _, optionPosition, _ ->
                    selectedOption = selectedPreference?.options?.get(optionPosition)
                }
            }

            binding.addToUserPreferencesButton.setOnClickListener {
                val preference = selectedPreference
                val option = selectedOption
                if (preference == null || option == null) {
                    showBasicAlert(requireContext(), "Data is missing")

                    return@setOnClickListener
                }

                viewModel.addUserSegmentToPreferences(preference, option)
            }

            binding.removeFromUserPreferencesButton.setOnClickListener {
                val preference = selectedPreference
                val option = selectedOption
                if (preference == null || option == null) {
                    showBasicAlert(requireContext(), "Data is missing")

                    return@setOnClickListener
                }

                viewModel.removeSegmentFromPreferences(preference, option)
            }
        }
    }
}
