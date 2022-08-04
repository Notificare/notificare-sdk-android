package re.notifica.iam.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import re.notifica.iam.R
import re.notifica.iam.databinding.NotificareInAppMessageFullscreenFragmentBinding
import re.notifica.iam.internal.ktx.getOrientationConstrainedImage
import re.notifica.iam.models.NotificareInAppMessage
import re.notifica.iam.ui.base.InAppMessagingBaseFragment

public open class InAppMessagingFullscreenFragment : InAppMessagingBaseFragment() {
    private lateinit var binding: NotificareInAppMessageFullscreenFragmentBinding

    override val animatedView: View
        get() = binding.cardView

    override val enterAnimation: Int = R.anim.notificare_iam_fullscreen_enter_animation
    override val exitAnimation: Int = R.anim.notificare_iam_fullscreen_exit_animation

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = NotificareInAppMessageFullscreenFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val image = message.getOrientationConstrainedImage(requireContext())

        binding.imageView.isVisible = image != null
        Glide.with(binding.imageView)
            .load(image)
            // TODO:  .placeholder()
            .into(binding.imageView)

        binding.titleView.isVisible = message.title != null
        binding.titleView.text = message.title

        binding.messageView.isVisible = message.message != null
        binding.messageView.text = message.message

        binding.cardView.setOnClickListener {
            handleActionClicked(NotificareInAppMessage.ActionType.PRIMARY)
        }

        binding.root.setOnClickListener {
            dismiss()
        }

        animate(Transition.ENTER)
    }
}
