package re.notifica.sample.ui.iap

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import re.notifica.Notificare
import re.notifica.monetize.ktx.monetize
import re.notifica.monetize.models.NotificareProduct
import re.notifica.sample.databinding.ActivityInAppPurchasesBinding
import re.notifica.sample.databinding.RowProductBinding

class InAppPurchasesActivity : AppCompatActivity() {
    private lateinit var binding: ActivityInAppPurchasesBinding

    private val adapter = ProductsAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityInAppPurchasesBinding.inflate(layoutInflater).also {
            setContentView(it.root)
        }

        binding.list.layoutManager = LinearLayoutManager(this)
        binding.list.adapter = adapter

        Notificare.monetize().observableProducts.observe(this) { products ->
            adapter.submitList(products)
        }
    }

    private fun onProductClicked(product: NotificareProduct) {
        Notificare.monetize().startPurchaseFlow(this, product)
    }


    private inner class ProductsAdapter : ListAdapter<NotificareProduct, ProductViewHolder>(
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

    private inner class ProductsAdapterDiffCallback : DiffUtil.ItemCallback<NotificareProduct>() {
        override fun areItemsTheSame(oldItem: NotificareProduct, newItem: NotificareProduct): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: NotificareProduct, newItem: NotificareProduct): Boolean {
            return oldItem == newItem
        }
    }

    private inner class ProductViewHolder(
        private val binding: RowProductBinding,
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(product: NotificareProduct) {
            binding.nameLabel.text = product.name
            binding.identifierLabel.text = product.identifier
            binding.descriptionLabel.text = product.storeDetails?.description ?: "---"
            binding.priceLabel.text = product.storeDetails?.oneTimePurchaseOfferDetails?.formattedPrice ?: "---"

            binding.root.setOnClickListener {
                onProductClicked(product)
            }
        }
    }
}
