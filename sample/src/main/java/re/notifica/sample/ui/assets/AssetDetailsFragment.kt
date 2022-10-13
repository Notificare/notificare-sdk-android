package re.notifica.sample.ui.assets

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import re.notifica.assets.models.NotificareAsset
import re.notifica.sample.R
import re.notifica.sample.databinding.FragmentAssetDetailsBinding

class AssetDetailsFragment : Fragment() {
    private lateinit var binding: FragmentAssetDetailsBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentAssetDetailsBinding.inflate(inflater, container, false)

        setupAssetDetails()

        return binding.root
    }

    private fun setupAssetDetails() {
        val asset: NotificareAsset? = arguments?.getParcelable("asset")
        val noData = getString(R.string.assets_no_avalable_data)

        if (asset == null) {
            return
        }

        if (asset.url == null) {
            binding.assetImage.setImageResource(R.drawable.ic_baseline_text_fields_24)
        } else {
            when (asset.metaData?.contentType) {
                "image/jpeg", "image/gif", "image/png" -> {
                    val imgUrl = asset.url
                    Glide.with(binding.root.context).load(imgUrl).into(binding.assetImage)
                }
                "video/mp4" -> {
                    binding.assetImage.setImageResource(R.drawable.ic_baseline_video_camera_back_24)
                }
                "application/pdf" -> {
                    binding.assetImage.setImageResource(R.drawable.ic_baseline_picture_as_pdf_24)
                }
                "application/json" -> {
                    binding.assetImage.setImageResource(R.drawable.ic_baseline_data_object_24)
                }
                "text/javascript" -> {
                    binding.assetImage.setImageResource(R.drawable.ic_baseline_javascript_24)
                }
                "text/css" -> {
                    binding.assetImage.setImageResource(R.drawable.ic_baseline_css_24)
                }
                "text/html" -> {
                    binding.assetImage.setImageResource(R.drawable.ic_baseline_html_24)
                }
            }
        }

        binding.assetDetailsTitleData.text = asset.title
        binding.assetDetailsDescriptionData.text = asset.description ?: noData
        binding.assetDetailsKeyData.text = asset.key ?: noData
        binding.assetDetailsUrlData.text = asset.url ?: noData
        binding.assetDetailsButtomLabelData.text = asset.button?.label ?: noData
        binding.assetDetailsButtomActionData.text = asset.button?.action ?: noData
        binding.assetDetailsMetaFileNameData.text = asset.metaData?.originalFileName ?: noData
        binding.assetDetailsMetaFileTypeData.text = asset.metaData?.contentType ?: noData
        binding.assetDetailsMetaFileLengthData.text = asset.metaData?.contentLength.toString()
    }
}
