package re.notifica.iam.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import re.notifica.iam.R
import re.notifica.iam.databinding.NotificareInAppMessageCardFragmentBinding
import re.notifica.iam.internal.ktx.getOrientationConstrainedImage
import re.notifica.iam.models.NotificareInAppMessage
import re.notifica.iam.ui.base.InAppMessagingBaseFragment

public open class InAppMessagingCardFragment : InAppMessagingBaseFragment() {
    private lateinit var binding: NotificareInAppMessageCardFragmentBinding

    override val animatedView: View
        get() = binding.cardView

    override val enterAnimation: Int = R.anim.notificare_iam_card_enter_animation
    override val exitAnimation: Int = R.anim.notificare_iam_card_exit_animation

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = NotificareInAppMessageCardFragmentBinding.inflate(inflater, container, false)
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

        binding.primaryActionButton.isVisible = message.primaryAction?.label != null
        binding.primaryActionButton.text = message.primaryAction?.label
        binding.primaryActionButton.setOnClickListener {
            handleActionClicked(NotificareInAppMessage.ActionType.PRIMARY)
        }

        binding.secondaryActionButton.isVisible = message.secondaryAction?.label != null
        binding.secondaryActionButton.text = message.secondaryAction?.label
        // TODO: Change text color based on the destructive property
        // binding.secondaryActionButton.setTextColor()
        binding.secondaryActionButton.setOnClickListener {
            handleActionClicked(NotificareInAppMessage.ActionType.SECONDARY)
        }

        binding.root.setOnClickListener {
            dismiss()
        }

        if (savedInstanceState == null) {
            animate(Transition.ENTER)
        }
    }
}
