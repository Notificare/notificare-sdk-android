package re.notifica.sample.user.inbox.ui.inbox

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import re.notifica.inbox.user.models.NotificareUserInboxItem
import re.notifica.sample.user.inbox.R
import re.notifica.sample.user.inbox.core.BaseFragment
import re.notifica.sample.user.inbox.databinding.FragmentInboxBinding

internal class InboxFragment : BaseFragment() {
    private lateinit var binding: FragmentInboxBinding
    private val viewModel: UserInboxViewModel by viewModels()
    private val adapter = InboxAdapter(::onInboxItemClicked, ::onInboxItemLongPressed)

    override val baseViewModel: UserInboxViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentInboxBinding.inflate(inflater, container, false)
        setupList()
        setupObservers()

        refresh()

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupMenu()
    }

    private fun setupMenu() {
        (requireActivity() as MenuHost).addMenuProvider(
            object : MenuProvider {
                override fun onPrepareMenu(menu: Menu) {
                    // Handle for example visibility of menu items
                }

                override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                    menuInflater.inflate(R.menu.inbox, menu)
                }

                override fun onMenuItemSelected(item: MenuItem): Boolean {
                    when (item.itemId) {
                        R.id.refresh -> refresh()
                    }

                    return true
                }
            },
            viewLifecycleOwner,
            Lifecycle.State.RESUMED
        )
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

    private fun onInboxItemClicked(item: NotificareUserInboxItem) {
        viewModel.open(requireActivity(), item)
    }

    private fun onInboxItemLongPressed(item: NotificareUserInboxItem) {
        InboxItemActionsBottomSheet(
            onOpenClicked = { onInboxItemClicked(item) },
            onMarkAsReadClicked = { markItemAsRead(item) },
            onRemoveClicked = { removeItem(item) }
        ).show(childFragmentManager, "options-bottom-sheet")
    }

    private fun markItemAsRead(item: NotificareUserInboxItem) {
        viewModel.markAsRead(item)
    }

    private fun removeItem(item: NotificareUserInboxItem) {
        viewModel.remove(item)
    }

    private fun refresh() {
        viewModel.refresh()
    }
}
