package re.notifica.push.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import re.notifica.internal.NotificareUtils
import re.notifica.models.NotificareNotification
import re.notifica.push.ui.databinding.NotificareNotificationImageFragmentBinding

class NotificareImageFragment : NotificationFragment() {

    private lateinit var binding: NotificareNotificationImageFragmentBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = NotificareNotificationImageFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (isAdded) {
            binding.pager.adapter = ImageAdapter(notification, this)
        }
    }

    class ImageAdapter(
        private val notification: NotificareNotification,
        fragment: Fragment
    ) : FragmentStateAdapter(fragment) {

        override fun createFragment(position: Int): Fragment {
            return ImageChildFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(SAVED_STATE_CONTENT, notification.content[position])
                }
            }
        }

        override fun getItemCount(): Int {
            return notification.content.size
        }
    }

    class ImageChildFragment : Fragment() {
        private lateinit var content: NotificareNotification.Content

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)

            content = savedInstanceState?.getParcelable(SAVED_STATE_CONTENT)
                ?: arguments?.getParcelable(SAVED_STATE_CONTENT)
                        ?: throw IllegalArgumentException("Missing required notification content parameter.")
        }

        override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View {
            return ImageView(inflater.context)
        }

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)

            NotificareUtils.loadImage(content.data as String, view as ImageView)
        }

        override fun onSaveInstanceState(outState: Bundle) {
            super.onSaveInstanceState(outState)
            outState.putParcelable(SAVED_STATE_CONTENT, content)
        }
    }

    companion object {
        private const val SAVED_STATE_CONTENT = "re.notifica.ui.Content"
    }
}
