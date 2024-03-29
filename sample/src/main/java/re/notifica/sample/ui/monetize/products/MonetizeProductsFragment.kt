package re.notifica.sample.ui.monetize.products

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import re.notifica.Notificare
import re.notifica.monetize.ktx.monetize
import re.notifica.monetize.models.NotificareProduct
import re.notifica.sample.databinding.FragmentMonetizeProductsBinding

class MonetizeProductsFragment : Fragment() {
    private lateinit var binding: FragmentMonetizeProductsBinding
    private val viewModel: MonetizeProductsViewModel by viewModels()
    private val adapterLiveData = MonetizeProductsListAdapter(::onProductPurchaseClicked)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentMonetizeProductsBinding.inflate(layoutInflater, container, false)

        setupList()
        setupObservers()

        return binding.root
    }

    private fun setupList() {
        binding.monetizeProductsLiveDataList.layoutManager = LinearLayoutManager(requireContext())
        binding.monetizeProductsLiveDataList.addItemDecoration(
            DividerItemDecoration(
                requireContext(),
                DividerItemDecoration.VERTICAL
            )
        )

        binding.monetizeProductsLiveDataList.adapter = adapterLiveData
    }

    private fun setupObservers() {
        viewModel.productsList.observe(viewLifecycleOwner) { products ->
            adapterLiveData.submitList(products)
        }
    }

    private fun onProductPurchaseClicked(product: NotificareProduct) {
        Notificare.monetize().startPurchaseFlow(requireActivity(), product)
    }
}
