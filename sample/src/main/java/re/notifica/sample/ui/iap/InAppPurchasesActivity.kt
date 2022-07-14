package re.notifica.sample.ui.iap

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.MediatorLiveData
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import re.notifica.Notificare
import re.notifica.monetize.ktx.monetize
import re.notifica.monetize.models.NotificareProduct
import re.notifica.monetize.models.NotificarePurchase
import re.notifica.sample.databinding.ActivityInAppPurchasesBinding
import re.notifica.sample.databinding.RowProductBinding

class InAppPurchasesActivity : AppCompatActivity() {
    private lateinit var binding: ActivityInAppPurchasesBinding

    private val adapter = ProductsAdapter()

    private val purchasedProducts = MediatorLiveData<Pair<List<NotificareProduct>, List<NotificarePurchase>>>().apply {
        addSource(Notificare.monetize().observableProducts) { value = Pair(it, value?.second ?: listOf()) }
        addSource(Notificare.monetize().observablePurchases) { value = Pair(value?.first ?: listOf(), it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityInAppPurchasesBinding.inflate(layoutInflater).also {
            setContentView(it.root)
        }

        binding.list.layoutManager = LinearLayoutManager(this)
        binding.list.adapter = adapter

        purchasedProducts.observe(this) { combined ->
            val products = combined.first
            val purchases = combined.second

            val data: List<ListItem> = products.map { product ->
                ListItem(
                    product = product,
                    purchases = purchases.filter { it.productIdentifier == product.identifier }
                )
            }

            adapter.submitList(data)
        }
    }

    private fun onProductClicked(product: NotificareProduct) {
        Notificare.monetize().startPurchaseFlow(this, product)
    }


    private data class ListItem(
        val product: NotificareProduct,
        val purchases: List<NotificarePurchase>,
    )

    private inner class ProductsAdapter : ListAdapter<ListItem, ProductViewHolder>(
        ProductsAdapterDiffCallback()
    ) {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
            val inflater = LayoutInflater.from(parent.context)

            return ProductViewHolder(RowProductBinding.inflate(inflater, parent, false))
        }

        override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
            holder.bind(getItem(position))
        }
    }

    private inner class ProductsAdapterDiffCallback : DiffUtil.ItemCallback<ListItem>() {
        override fun areItemsTheSame(oldItem: ListItem, newItem: ListItem): Boolean {
            return oldItem.product.id == newItem.product.id
        }

        override fun areContentsTheSame(oldItem: ListItem, newItem: ListItem): Boolean {
            return oldItem == newItem
        }
    }

    private inner class ProductViewHolder(
        private val binding: RowProductBinding,
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ListItem) {
            val product = item.product
            val purchased = item.product.type == NotificareProduct.TYPE_ONE_TIME && item.purchases.isNotEmpty()

            binding.nameLabel.text = product.name
            binding.identifierLabel.text = product.identifier
            binding.descriptionLabel.text = product.storeDetails?.description ?: "---"
            binding.priceLabel.text = product.storeDetails?.oneTimePurchaseOfferDetails?.formattedPrice ?: "---"
            binding.purchasedImage.isVisible = purchased

            binding.root.setOnClickListener {
                onProductClicked(product)
            }
        }
    }
}
