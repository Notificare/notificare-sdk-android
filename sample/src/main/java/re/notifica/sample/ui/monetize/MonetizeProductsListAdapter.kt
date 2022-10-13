package re.notifica.sample.ui.monetize

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import re.notifica.monetize.models.NotificareProduct
import re.notifica.sample.databinding.RowMonetizeProductBinding
import re.notifica.sample.ui.monetize.MonetizeViewModel.ProductItem

class MonetizeProductsListAdapter(
    private val onProductClicked: (NotificareProduct) -> Unit
) : ListAdapter<ProductItem, RecyclerView.ViewHolder>(ProductsDiffCallback()) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return ItemViewHolder(
            RowMonetizeProductBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position)
        (holder as ItemViewHolder).bind(item)
    }

    private inner class ItemViewHolder(
        private val binding: RowMonetizeProductBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ProductItem) {
            val product = item.product
            val purchased = item.product.type == NotificareProduct.TYPE_ONE_TIME && item.purchases.isNotEmpty()

            binding.productIdData.text = product.id
            binding.productNameData.text = product.name
            binding.productTypeData.text = product.type

            val details = product.storeDetails
            if (details != null) {
                binding.priceLabelData.text = "${details.currencyCode} ${details.price}"
            } else {
                binding.priceLabelData.text = "---"
            }

            binding.productAlreadyBoughtImage.isVisible = purchased
            binding.buyProductImage.isVisible = !purchased

            binding.buyProductImage.setOnClickListener {
                onProductClicked(item.product)
            }
        }
    }
}

private class ProductsDiffCallback : DiffUtil.ItemCallback<ProductItem>() {
    override fun areItemsTheSame(oldItem: ProductItem, newItem: ProductItem): Boolean {
        return oldItem == newItem
    }

    override fun areContentsTheSame(oldItem: ProductItem, newItem: ProductItem): Boolean {
        return oldItem == newItem
    }
}
