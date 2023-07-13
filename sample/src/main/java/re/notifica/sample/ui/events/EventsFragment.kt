package re.notifica.sample.ui.events

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.viewModels
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
            addEventDataField()
        }

        binding.registerEventButton.setOnClickListener {
            registerCustomEvent()
        }
    }

    private fun addEventDataField() {
        val field = EventField()
        viewModel.eventDataFields.add(field)

        val fieldView = RowEventFieldBinding.inflate(layoutInflater)

        fieldView.inputKey.doOnTextChanged { text, _, _, _ ->
            field.key = text.toString()
        }

        fieldView.inputValue.doOnTextChanged { text, _, _, _ ->
            field.value = text.toString()
        }

        binding.fieldsLinearLayout.addView(fieldView.root)
    }

    private fun registerCustomEvent() {
        val eventName = binding.inputEventName.editText?.text.toString()

        if (eventName.isEmpty()) {
            AlertDialog.Builder(requireContext())
                .setTitle(R.string.app_name)
                .setMessage("Enter event name.")
                .setCancelable(false)
                .setPositiveButton(R.string.dialog_ok_button, null)
                .show()

            return
        }

        viewModel.registerEvent(eventName)
        binding.fieldsLinearLayout.removeAllViews()
        binding.inputEventName.editText?.text?.clear()
    }
}
