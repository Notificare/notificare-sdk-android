package re.notifica.sample.ui.monetize

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import re.notifica.sample.databinding.FragmentMonetizePurchasesBinding

class MonetizePurchasesFragment : Fragment() {
    private lateinit var binding: FragmentMonetizePurchasesBinding
    private val viewModel: MonetizeViewModel by viewModels()
    private val adapterLiveData = MonetizePurchasesListAdapter()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentMonetizePurchasesBinding.inflate(layoutInflater, container, false)

        setupLists()
        setupObservers()

        return binding.root
    }

    private fun setupLists() {
        binding.monetizePurchasesLiveDataList.layoutManager = LinearLayoutManager(requireContext())
        binding.monetizePurchasesLiveDataList.addItemDecoration(
            DividerItemDecoration(
                requireContext(),
                DividerItemDecoration.VERTICAL
            )
        )
        binding.monetizePurchasesLiveDataList.adapter = adapterLiveData
    }

    private fun setupObservers() {
        viewModel.purchasesList.observe(viewLifecycleOwner) { purchases ->
            adapterLiveData.submitList(purchases)
        }
    }
}
