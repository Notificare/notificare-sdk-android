package re.notifica.sample.ui.events

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.viewModels
import com.google.android.material.textfield.TextInputLayout
import re.notifica.sample.R
import re.notifica.sample.databinding.FragmentEventsBinding
import re.notifica.sample.databinding.RowEventFieldBinding
import re.notifica.sample.models.BaseFragment

class EventsFragment : BaseFragment() {
    private lateinit var binding: FragmentEventsBinding
    private val viewModel: EventsViewModel by viewModels()

    override val baseViewModel: EventsViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentEventsBinding.inflate(inflater, container, false)

        setupListeners()

        return binding.root
    }

    private fun setupListeners() {
        binding.addFieldImg.setOnClickListener {
            viewModel.userFieldsCount = viewModel.userFieldsCount + 1
            val rowNumber = viewModel.userFieldsCount
            val newField = RowEventFieldBinding.inflate(layoutInflater)
            newField.inputKey.tag = "key$rowNumber"
            newField.inputEventValue.tag = "value$rowNumber"

            binding.fieldLinaerLayout.addView(newField.root)
        }

        binding.registerEventDataButton.setOnClickListener {
            val eventName = binding.inputEventName.editText?.text.toString()

            if (eventName.isEmpty()) {
                AlertDialog.Builder(requireContext())
                    .setTitle(R.string.app_name)
                    .setMessage("Enter event name.")
                    .setCancelable(false)
                    .setNegativeButton(R.string.dialog_ok_button) { _, _ ->
                    }
                    .show()

                return@setOnClickListener
            }

            val fieldsMap = mutableMapOf<String, String>()
            val rows = viewModel.userFieldsCount
            if (rows > 0) {
                for (row in 1..rows) {
                    val key = binding.fieldLinaerLayout.findViewWithTag<TextInputLayout>("key$row")
                    val value = binding.fieldLinaerLayout.findViewWithTag<TextInputLayout>("value$row")

                    if (!key.editText?.text.isNullOrEmpty()) {
                        fieldsMap[key.editText?.text.toString()] = value.editText?.text.toString()
                    }
                }
            }

            viewModel.registerEvent(eventName, fieldsMap)
            binding.fieldLinaerLayout.removeAllViews()
            binding.inputEventName.editText?.text?.clear()
        }
    }
}
