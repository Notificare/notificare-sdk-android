package re.notifica.sample.user.inbox.ui.inbox

import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import re.notifica.inbox.user.models.NotificareUserInboxItem
import re.notifica.sample.user.inbox.R
import re.notifica.sample.user.inbox.ktx.dp
import re.notifica.sample.user.inbox.databinding.RowInboxItemBinding
import java.util.*

class InboxAdapter(
    private val onInboxItemClicked: (NotificareUserInboxItem) -> Unit,
    private val onInboxItemLongPressed: (NotificareUserInboxItem) -> Unit,
) : ListAdapter<NotificareUserInboxItem, RecyclerView.ViewHolder>(InboxDiffCallback()) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return ItemViewHolder(
            RowInboxItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position)
        (holder as ItemViewHolder).bind(item)
    }

    private inner class ItemViewHolder(
        private val binding: RowInboxItemBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: NotificareUserInboxItem) {
            Glide.with(binding.attachmentImage)
                .load(item.notification.attachments.firstOrNull()?.uri)
                .placeholder(R.drawable.shape_inbox_attachment_placeholder)
                .apply(RequestOptions.bitmapTransform(RoundedCorners(8.dp.toInt())))
                .into(binding.attachmentImage)

            binding.attachmentImage.isVisible = item.notification.attachments.isNotEmpty()

            binding.title.text = item.notification.title ?: "---"
            binding.message.text = item.notification.message
            binding.type.text = item.notification.type
            binding.timeAgo.text = DateUtils.getRelativeTimeSpanString(
                item.time.time,
                Calendar.getInstance().timeInMillis,
                DateUtils.MINUTE_IN_MILLIS
            )

            binding.readStatus.isInvisible = item.opened

            binding.root.setOnClickListener {
                item.also { onInboxItemClicked(it) }
            }

            binding.root.setOnLongClickListener {
                item.also { onInboxItemLongPressed(it) }
                return@setOnLongClickListener true
            }
        }
    }
}

private class InboxDiffCallback : DiffUtil.ItemCallback<NotificareUserInboxItem>() {
    override fun areItemsTheSame(oldItem: NotificareUserInboxItem, newItem: NotificareUserInboxItem): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: NotificareUserInboxItem, newItem: NotificareUserInboxItem): Boolean {
        return oldItem == newItem
    }
}
