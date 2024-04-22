package re.notifica.push.ui.notifications.fragments

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.widget.FrameLayout
import androidx.core.view.isVisible
import re.notifica.Notificare
import re.notifica.internal.common.onMainThread
import re.notifica.push.ui.databinding.NotificareNotificationVideoFragmentBinding
import re.notifica.push.ui.ktx.pushUIInternal
import re.notifica.push.ui.notifications.fragments.base.NotificationFragment
import re.notifica.push.ui.utils.NotificationWebViewClient

public class NotificareVideoFragment : NotificationFragment() {

    private lateinit var binding: NotificareNotificationVideoFragmentBinding
    private var customView: View? = null
    private var customViewCallback: WebChromeClient.CustomViewCallback? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
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
        binding.webView.webViewClient = NotificationWebViewClient(notification, callback)

        val content = notification.content.firstOrNull()
        val html = when (content?.type) {
            "re.notifica.content.YouTube" -> getYouTubeVideoHtml(content.data as String)
            "re.notifica.content.Vimeo" -> getVimeoVideoHtml(content.data as String)
            "re.notifica.content.HTML5Video" -> getHtml5VideoHtml(content.data as String)
            else -> {
                onMainThread {
                    Notificare.pushUIInternal().lifecycleListeners.forEach {
                        it.get()?.onNotificationFailedToPresent(
                            notification
                        )
                    }
                }

                return
            }
        }

        binding.webView.loadDataWithBaseURL(null, html, "text/html", "utf-8", null)
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

    private fun getYouTubeVideoHtml(videoId: String): String {
        return """
            <!DOCTYPE html>
            <html>
            <head>
              <meta name="viewport" content="initial-scale=1, maximum-scale=1">
              <style>
                body {
                  margin: 0;
                }
                
                #player {
                  width: 100vw;
                  height: 100vh;
                }
              </style>
            </head>
            <body>
            <iframe src="https://www.youtube-nocookie.com/embed/$videoId?enablejsapi=1"
                    id="player"
                    frameborder="0"
                    webkitallowfullscreen
                    mozallowfullscreen
                    allowfullscreen
            ></iframe>
            
            <script type="text/javascript">
              var tag = document.createElement("script");
              tag.src = "https://www.youtube.com/iframe_api";
              
              var firstScriptTag = document.getElementsByTagName("script")[0];
              firstScriptTag.parentNode.insertBefore(tag, firstScriptTag);
              
              var player;
              
              function onYouTubeIframeAPIReady() {
                player = new YT.Player("player", {
                  events: {
                    "onReady": onPlayerReady
                  }
                });
              }
              
              function onPlayerReady(event) {
                event.target.playVideo();
              }
            </script>
            </body>
            </html>
        """.trimIndent()
    }

    private fun getVimeoVideoHtml(videoId: String): String {
        return """
            <!DOCTYPE html>
            <html>
            <head>
              <meta name="viewport" content="initial-scale=1, maximum-scale=1">
              <style>
                body {
                  margin: 0;
                }
                
                #player {
                  width: 100vw;
                  height: 100vh;
                }
              </style>
            </head>
            <body>
            <iframe src="https://player.vimeo.com/video/$videoId?autoplay=1"
                    id="player"
                    frameborder="0"
                    webkitallowfullscreen
                    mozallowfullscreen
                    allowfullscreen
            ></iframe>
            </body>
            </html>
        """.trimIndent()
    }

    private fun getHtml5VideoHtml(videoId: String): String {
        return """
            <!DOCTYPE html>
            <html>
            <head>
              <meta name="viewport" content="initial-scale=1, maximum-scale=1">
              <style>
                body {
                  margin: 0;
                }
                
                #player {
                  width: 100vw;
                  height: 100vh;
                }
              </style>
            </head>
            <body>
            <video id="player" autoplay controls preload>
              <source src="$videoId" type="video/mp4" />
            </video>
            </body>
            </html>
        """.trimIndent()
    }

    public inner class VideoChromeClient : WebChromeClient() {
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
}
