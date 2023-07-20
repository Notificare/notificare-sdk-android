package re.notifica.sample.ui.assets

import android.os.Bundle
import android.view.*
import androidx.appcompat.widget.SearchView
import androidx.core.os.bundleOf
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import re.notifica.assets.models.NotificareAsset
import re.notifica.sample.R
import re.notifica.sample.databinding.FragmentAssetsBinding
import re.notifica.sample.models.BaseFragment

class AssetsFragment : BaseFragment() {
    private lateinit var binding: FragmentAssetsBinding
    private val viewModel: AssetsViewModel by viewModels()
    private val listAdapter = AssetsListAdapter(::onAssetClicked)

    override val baseViewModel: AssetsViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentAssetsBinding.inflate(inflater, container, false)

        setupList()
        setupObservers()

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupMenu()
    }

    private fun setupMenu() {
        (requireActivity() as MenuHost).addMenuProvider(object : MenuProvider {
            override fun onPrepareMenu(menu: Menu) {
                // Handle for example visibility of menu items
            }

            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.assets_menu, menu)
            }

            override fun onMenuItemSelected(item: MenuItem): Boolean {
                when (item.itemId) {
                    R.id.assets_search -> {
                        val searchView = item.actionView as SearchView

                        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                            override fun onQueryTextSubmit(query: String): Boolean {
                                getAssets(query)

                                return false
                            }

                            override fun onQueryTextChange(s: String): Boolean {
                                return false
                            }
                        })
                    }
                }

                return true
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private fun setupList() {
        binding.assetsRecycleList.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.assetsRecycleList.adapter = listAdapter
    }

    private fun setupObservers() {
        viewModel.assets.observe(viewLifecycleOwner) { assets ->
            binding.noDataLabel.isVisible = assets.isEmpty()
            listAdapter.submitList(assets)
        }
    }

    private fun getAssets(group: String) {
        viewModel.fetchAssets(group)
    }

    private fun onAssetClicked(asset: NotificareAsset) {
        val bundle = bundleOf("asset" to asset)
        findNavController().navigate(R.id.action_assetsFragment_to_assetDetailsFragment, bundle)
    }
}
