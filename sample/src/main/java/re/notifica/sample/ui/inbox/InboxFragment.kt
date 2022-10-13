package re.notifica.sample.ui.inbox

import android.os.Bundle
import android.view.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import re.notifica.Notificare
import re.notifica.inbox.ktx.inbox
import re.notifica.inbox.models.NotificareInboxItem
import re.notifica.sample.R
import re.notifica.sample.databinding.FragmentInboxBinding
import re.notifica.sample.models.BaseFragment

class InboxFragment : BaseFragment() {
    private lateinit var binding: FragmentInboxBinding
    private val viewModel: InboxViewModel by viewModels()
    private val adapter = InboxAdapter(::onInboxItemClicked, ::onInboxItemLongPressed)

    override val baseViewModel: InboxViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.inbox, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.refresh -> onRefreshClicked()
            R.id.read_all -> onReadAllClicked()
            R.id.remove_all -> onRemoveAllClicked()
            else -> return super.onOptionsItemSelected(item)
        }

        return true
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentInboxBinding.inflate(inflater, container, false)

        Notificare.inbox().observableBadge.observe(requireActivity()) { badge ->
            baseViewModel.showSnackBar("Unread count: $badge")

            if (badge != Notificare.inbox().badge) {
                AlertDialog.Builder(requireContext())
                    .setMessage("Badge mismatch.\nLiveData = $badge\nNotificareInbox.badge = ${Notificare.inbox().badge}")
                    .show()
            }
        }

        setupList()
        setupObservers()

        return binding.root
    }

    private fun setupList() {
        binding.inboxList.layoutManager = LinearLayoutManager(requireContext())
        binding.inboxList.addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))
        binding.inboxList.adapter = adapter
    }

    private fun setupObservers() {
        viewModel.items.observe(viewLifecycleOwner) { items ->
            adapter.submitList(items)
        }
    }

    private fun onInboxItemClicked(item: NotificareInboxItem) {
        viewModel.onInboxItemClicked(requireActivity(), item)
    }

    private fun onInboxItemLongPressed(item: NotificareInboxItem) {
        InboxItemActionsBottomSheet(
            onOpenClicked = { onInboxItemClicked(item) },
            onMarkAsReadClicked = { onMarkItemAsReadClicked(item) },
            onRemoveClicked = { onRemoveItemClicked(item) }
        ).show(childFragmentManager, "options-bottom-sheet")
    }

    private fun onMarkItemAsReadClicked(item: NotificareInboxItem) {
        viewModel.onMarkItemAsReadClicked(item)
    }

    private fun onRemoveItemClicked(item: NotificareInboxItem) {
        viewModel.onRemoveItemClicked(item)
    }

    private fun onReadAllClicked() {
        viewModel.onReadAllClicked()
    }

    private fun onRemoveAllClicked() {
        viewModel.onRemoveAllClicked()
    }

    private fun onRefreshClicked() {
        viewModel.onRefreshClicked()
    }
}
