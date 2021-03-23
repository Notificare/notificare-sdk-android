package re.notifica.sample.ui.inbox

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import re.notifica.sample.databinding.InboxItemActionsBottomSheetBinding

class InboxItemActionsBottomSheet : BottomSheetDialogFragment() {

    private lateinit var binding: InboxItemActionsBottomSheetBinding
    var listener: Listener? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = InboxItemActionsBottomSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.open.setOnClickListener {
            dismissAllowingStateLoss()
            listener?.onOpenClicked()
        }

        binding.markAsRead.setOnClickListener {
            dismissAllowingStateLoss()
            listener?.onMarkAsReadClicked()
        }

        binding.delete.setOnClickListener {
            dismissAllowingStateLoss()
            listener?.onDeleteClicked()
        }
    }

    interface Listener {

        fun onOpenClicked()

        fun onMarkAsReadClicked()

        fun onDeleteClicked()
    }
}
