package re.notifica.sample.ui.monetize.products

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import re.notifica.monetize.models.NotificareProduct
import re.notifica.sample.databinding.RowMonetizeProductBinding

class MonetizeProductsListAdapter(
    private val onProductPurchaseClicked: (NotificareProduct) -> Unit
) : ListAdapter<NotificareProduct, RecyclerView.ViewHolder>(ProductsDiffCallback()) {
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

        fun bind(product: NotificareProduct) {
            val details = product.storeDetails

            binding.productIdData.text = product.id
            binding.productNameData.text = product.name
            binding.productTypeData.text = product.type
            binding.priceLabelData.text = if (details != null) "${details.currencyCode} ${details.price}" else "---"

            binding.buyProductImage.setOnClickListener {
                onProductPurchaseClicked(product)
            }
        }
    }
}

private class ProductsDiffCallback : DiffUtil.ItemCallback<NotificareProduct>() {
    override fun areItemsTheSame(oldItem: NotificareProduct, newItem: NotificareProduct): Boolean {
        return oldItem == newItem
    }

    override fun areContentsTheSame(oldItem: NotificareProduct, newItem: NotificareProduct): Boolean {
        return oldItem == newItem
    }
}
