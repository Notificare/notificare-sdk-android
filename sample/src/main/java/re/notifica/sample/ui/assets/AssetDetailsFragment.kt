package re.notifica.sample.ui.assets

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.BundleCompat
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import re.notifica.assets.models.NotificareAsset
import re.notifica.sample.R
import re.notifica.sample.databinding.FragmentAssetDetailsBinding

class AssetDetailsFragment : Fragment() {
    private lateinit var binding: FragmentAssetDetailsBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentAssetDetailsBinding.inflate(inflater, container, false)

        loadAssetDetails()

        return binding.root
    }

    private fun loadAssetDetails() {
        val asset: NotificareAsset = arguments?.let {
            BundleCompat.getParcelable(it, "asset", NotificareAsset::class.java)
        } ?: return

        if (asset.url.isNullOrEmpty()) {
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

        val noDataString = getString(R.string.assets_no_avalable_data)

        binding.assetDetailsTitleData.text = asset.title
        binding.assetDetailsDescriptionData.text = asset.description ?: noDataString
        binding.assetDetailsKeyData.text = asset.key ?: noDataString
        binding.assetDetailsUrlData.text = asset.url ?: noDataString
        binding.assetDetailsButtomLabelData.text = asset.button?.label ?: noDataString
        binding.assetDetailsButtomActionData.text = asset.button?.action ?: noDataString
        binding.assetDetailsMetaFileNameData.text = asset.metaData?.originalFileName ?: noDataString
        binding.assetDetailsMetaFileTypeData.text = asset.metaData?.contentType ?: noDataString
        binding.assetDetailsMetaFileLengthData.text = asset.metaData?.contentLength.toString()
    }
}
