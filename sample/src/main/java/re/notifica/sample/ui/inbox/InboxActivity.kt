package re.notifica.sample.ui.inbox

import android.os.Bundle
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import re.notifica.inbox.NotificareInbox
import re.notifica.inbox.models.NotificareInboxItem
import re.notifica.push.ui.NotificarePushUI
import re.notifica.sample.R
import re.notifica.sample.databinding.ActivityInboxBinding
import re.notifica.sample.databinding.RowInboxItemBinding
import java.util.*

class InboxActivity : AppCompatActivity() {

    private lateinit var binding: ActivityInboxBinding
    private lateinit var adapter: InboxAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityInboxBinding.inflate(layoutInflater).also {
            setContentView(it.root)
        }

        binding.list.layoutManager = LinearLayoutManager(this)
        binding.list.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))
        binding.list.adapter = InboxAdapter().also { adapter = it }

        NotificareInbox.observableItems.observe(this) {
            adapter.data = it.toList()
        }

        NotificareInbox.observableBadge.observe(this) { badge ->
            Snackbar.make(binding.root, "Unread count: $badge", Snackbar.LENGTH_LONG).show()

            if (badge != NotificareInbox.badge) {
                AlertDialog.Builder(this)
                    .setMessage("Badge mismatch.\nLiveData = $badge\nNotificareInbox.badge = ${NotificareInbox.badge}")
                    .show()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.inbox, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.refresh -> NotificareInbox.refresh()
            R.id.read_all -> GlobalScope.launch { NotificareInbox.markAllAsRead() }
            R.id.remove_all -> GlobalScope.launch { NotificareInbox.clear() }
            else -> return super.onOptionsItemSelected(item)
        }

        return true
    }

    private inner class InboxAdapter : RecyclerView.Adapter<InboxAdapter.ViewHolder>() {

        var data = listOf<NotificareInboxItem>()
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

        override fun getItemCount(): Int = data.size


        private inner class ViewHolder(parent: ViewGroup) : RecyclerView.ViewHolder(
            RowInboxItemBinding.inflate(LayoutInflater.from(parent.context), parent, false).root
        ) {
            private val binding = RowInboxItemBinding.bind(itemView)

            fun update(item: NotificareInboxItem) {
                binding.image.isVisible = item.notification.attachments.isNotEmpty()

                binding.title.text = item.notification.title ?: "---"
                binding.message.text = item.notification.message
                binding.type.text = item.notification.type

                binding.timeAgo.text = DateUtils.getRelativeTimeSpanString(
                    item.time.time,
                    Calendar.getInstance().timeInMillis,
                    DateUtils.MINUTE_IN_MILLIS
                )

                binding.readStatus.isVisible = !item.opened

                binding.root.setOnClickListener {
                    GlobalScope.launch {
                        val notification = NotificareInbox.open(item)
                        NotificarePushUI.presentNotification(this@InboxActivity, notification)
                    }
                }

                binding.root.setOnLongClickListener {
                    val bottomSheet = InboxItemActionsBottomSheet().apply {
                        listener = object : InboxItemActionsBottomSheet.Listener {
                            override fun onOpenClicked() {
                                GlobalScope.launch {
                                    val notification = NotificareInbox.open(item)
                                    NotificarePushUI.presentNotification(this@InboxActivity, notification)
                                }
                            }

                            override fun onMarkAsReadClicked() {
                                GlobalScope.launch {
                                    NotificareInbox.markAsRead(item)
                                }
                            }

                            override fun onDeleteClicked() {
                                GlobalScope.launch {
                                    NotificareInbox.remove(item)
                                }
                            }
                        }
                    }

                    bottomSheet.show(this@InboxActivity.supportFragmentManager, "action-sheet")

                    true
                }
            }
        }
    }
}