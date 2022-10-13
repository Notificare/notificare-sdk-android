package re.notifica.sample.ui.inbox

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import re.notifica.sample.databinding.InboxItemActionsBottomSheetBinding

class InboxItemActionsBottomSheet(
    private val onOpenClicked: () -> Unit,
    private val onMarkAsReadClicked: () -> Unit,
    private val onRemoveClicked: () -> Unit,
) : BottomSheetDialogFragment() {
    private lateinit var binding: InboxItemActionsBottomSheetBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = InboxItemActionsBottomSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.open.setOnClickListener {
            onOpenClicked()
            dismiss()
        }

        binding.markAsRead.setOnClickListener {
            onMarkAsReadClicked()
            dismiss()
        }

        binding.delete.setOnClickListener {
            onRemoveClicked()
            dismiss()
        }
    }
}
