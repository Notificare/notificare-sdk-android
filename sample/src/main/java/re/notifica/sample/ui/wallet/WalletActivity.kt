package re.notifica.sample.ui.wallet

import android.os.Bundle
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import re.notifica.Notificare
import re.notifica.loyalty.ktx.loyalty
import re.notifica.loyalty.models.NotificarePass
import re.notifica.sample.databinding.ActivityWalletBinding
import re.notifica.sample.databinding.WalletItemRowBinding
import java.util.*

class WalletActivity : AppCompatActivity() {
    private lateinit var binding: ActivityWalletBinding
    private lateinit var adapter: WalletAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWalletBinding.inflate(layoutInflater).also {
            setContentView(it.root)
        }

        binding.list.layoutManager = LinearLayoutManager(this)
        binding.list.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))
        binding.list.adapter = WalletAdapter().also { adapter = it }

        Notificare.loyalty().observablePasses.observe(this) { passes ->
            adapter.data = passes
        }
    }

    private inner class WalletAdapter : RecyclerView.Adapter<WalletAdapter.ViewHolder>() {

        var data = listOf<NotificarePass>()
            set(value) {
                field = value
                notifyDataSetChanged()
            }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            return ViewHolder(parent)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.update(data[position])
        }

        override fun getItemCount(): Int {
            return data.size
        }

        private inner class ViewHolder(parent: ViewGroup) : RecyclerView.ViewHolder(
            WalletItemRowBinding.inflate(LayoutInflater.from(parent.context), parent, false).root
        ) {
            private val binding = WalletItemRowBinding.bind(itemView)

            fun update(pass: NotificarePass) {
                binding.logo.isVisible = pass.icon != null
                Glide.with(binding.logo)
                    .load(pass.icon)
                    .into(binding.logo)

                binding.serial.text = pass.serial
                binding.description.text = pass.description

                binding.lastUpdated.text = DateUtils.getRelativeTimeSpanString(
                    pass.date.time,
                    Calendar.getInstance().timeInMillis,
                    DateUtils.MINUTE_IN_MILLIS
                )

                binding.root.setOnClickListener {
                    Notificare.loyalty().present(this@WalletActivity, pass)
                }
            }
        }
    }
}
