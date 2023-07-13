package re.notifica.sample.ui.monetize.purchases

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import re.notifica.monetize.models.NotificarePurchase
import re.notifica.sample.databinding.RowMonetizePurchaseBinding

class MonetizePurchasesListAdapter :
    ListAdapter<NotificarePurchase, RecyclerView.ViewHolder>(PurchasesDiffCallback()) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return ItemViewHolder(
            RowMonetizePurchaseBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position)
        (holder as ItemViewHolder).bind(item)
    }

    private inner class ItemViewHolder(
        private val binding: RowMonetizePurchaseBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: NotificarePurchase) {
            binding.monetizePurchaseIdData.text = item.id
            binding.monetizePurchaseTokenData.text = item.token
        }
    }
}

private class PurchasesDiffCallback : DiffUtil.ItemCallback<NotificarePurchase>() {
    override fun areItemsTheSame(oldItem: NotificarePurchase, newItem: NotificarePurchase): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: NotificarePurchase, newItem: NotificarePurchase): Boolean {
        return oldItem == newItem
    }
}
