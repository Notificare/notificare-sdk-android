package re.notifica.push.ui.fragments

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.core.view.isVisible
import re.notifica.push.ui.databinding.NotificareNotificationVideoFragmentBinding
import re.notifica.push.ui.fragments.base.NotificationFragment

class NotificareVideoFragment : NotificationFragment() {

    private lateinit var binding: NotificareNotificationVideoFragmentBinding
    private var customView: View? = null
    private var customViewCallback: WebChromeClient.CustomViewCallback? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = NotificareNotificationVideoFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.webView.settings.javaScriptEnabled = true
        binding.webView.settings.mediaPlaybackRequiresUserGesture = false
        binding.webView.settings.loadWithOverviewMode = true
        binding.webView.settings.useWideViewPort = true
        binding.webView.settings.builtInZoomControls = true
        binding.webView.webChromeClient = VideoChromeClient()
        binding.webView.webViewClient = WebViewClient()

        val content = notification.content.firstOrNull()
        val html = when (content?.type) {
            "re.notifica.content.YouTube" -> String.format(youTubeVideoHTML, content.data)
            "re.notifica.content.Vimeo" -> String.format(vimeoVideoHTML, content.data)
            "re.notifica.content.HTML5Video" -> String.format(html5Video, content.data)
            else -> null
        }

        if (html != null) {
            binding.webView.loadDataWithBaseURL(null, html, "text/html", "utf-8", null)
        }
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)

        if (savedInstanceState != null) {
            binding.webView.restoreState(savedInstanceState)
        }
    }

    override fun onResume() {
        super.onResume()
        binding.webView.onResume()
    }

    override fun onPause() {
        super.onPause()
        binding.webView.onPause()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        binding.webView.saveState(outState)
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.webView.destroy()
    }

    inner class VideoChromeClient : WebChromeClient() {
        override fun onShowCustomView(view: View, callback: CustomViewCallback) {
            // if a view already exists then immediately terminate the new one
            if (customView != null) {
                callback.onCustomViewHidden()
                return
            }

            binding.webView.isVisible = false

            view.layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )

            customView = view
            customViewCallback = callback

            binding.fullscreenView.addView(view)
            binding.fullscreenView.isVisible = true

            this@NotificareVideoFragment.callback.onNotificationFragmentCanHideActionBar()
        }

        override fun onHideCustomView() {
            val customView = customView ?: return

            // Hide the custom view.
            customView.isVisible = false

            // Remove the custom view from its container.
            binding.fullscreenView.removeView(customView)
            binding.fullscreenView.isVisible = false

            this@NotificareVideoFragment.customView = null
            customViewCallback?.onCustomViewHidden()

            // Show the content view.
            binding.webView.isVisible = true
            callback.onNotificationFragmentShouldShowActionBar()
        }
    }

    companion object {
        private const val youTubeVideoHTML =
            "<!DOCTYPE html><html><head><meta name=\"viewport\" content=\"initial-scale=1, maximum-scale=1\"><style>body{margin:0px 0px 0px 0px;} #player{width: 100vw; height: 100vh;}</style></head><body><div id=\"player\"></div><script>var tag = document.createElement('script'); tag.src = 'https://www.youtube.com/player_api'; var firstScriptTag = document.getElementsByTagName('script')[0]; firstScriptTag.parentNode.insertBefore(tag, firstScriptTag); var player; function onYouTubePlayerAPIReady() { player = new YT.Player('player', { width:window.innerWidth, height:window.innerHeight, videoId:'%s', events: { onReady: onPlayerReady } }); } function onPlayerReady(event) { event.target.playVideo(); } function resizePlayer(width, height) {player.setSize(width, height);}</script></body></html>"

        private const val vimeoVideoHTML =
            "<!DOCTYPE html><html><head><meta name=\"viewport\" content=\"initial-scale=1, maximum-scale=1\"><style>body{margin:0px 0px 0px 0px;} #player{width: 100vw; height: 100vh;}</style></head><body><iframe id=\"player\" src=\"https://player.vimeo.com/video/%s?autoplay=1\" frameborder=\"0\" webkitallowfullscreen mozallowfullscreen allowfullscreen></iframe><script>function resizePlayer(width, height) {var player = document.getElementById('player'); player.width = width; player.height = height;}</script></body> </html>"

        private const val html5Video =
            "<!DOCTYPE html><html><head><meta name=\"viewport\" content=\"initial-scale=1, maximum-scale=1\"><style>body{margin:0px 0px 0px 0px;} #player{width: 100vw; height: 100vh;}</style></head><body><video id=\"player\" autoplay controls preload><source src=\"%s\" type=\"video/mp4\"/></video><script>function resizePlayer(width, height) {var player = document.getElementById('player'); player.height = height;}</script></body></html>"
    }
}
