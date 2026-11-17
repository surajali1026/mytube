package com.mytube.app

import android.annotation.SuppressLint
import android.app.Activity
import android.content.pm.ActivityInfo
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.ui.viewinterop.AndroidView

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun YouTubeFullscreenPlayer(videoId: String, onClose: () -> Unit, onEnded: () -> Unit) {
    BackHandler(onBack = onClose)
    val activity = LocalContext.current as? Activity
    DisposableEffect(activity) {
        val window = activity?.window
        val oldOrientation = activity?.requestedOrientation
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        if (window != null) {
            WindowCompat.setDecorFitsSystemWindows(window, false)
            WindowInsetsControllerCompat(window, window.decorView).hide(WindowInsetsCompat.Type.systemBars())
        }
        onDispose {
            activity?.requestedOrientation = oldOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            if (window != null) {
                WindowCompat.setDecorFitsSystemWindows(window, true)
                WindowInsetsControllerCompat(window, window.decorView).show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }
    var webView by remember { mutableStateOf<WebView?>(null) }
    DisposableEffect(Unit) {
        onDispose {
            webView?.apply { stopLoading(); loadUrl("about:blank"); removeAllViews(); destroy() }
            webView = null
        }
    }
    Box(Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
        AndroidView(
            modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f),
            factory = { context ->
                WebView(context).apply {
                    layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.mediaPlaybackRequiresUserGesture = false
                    webChromeClient = WebChromeClient()
                    webViewClient = WebViewClient()
                    addJavascriptInterface(object {
                        @android.webkit.JavascriptInterface
                        fun ended() { post { onEnded() } }
                    }, "MyTubeBridge")
                    val html = """
                        <!doctype html><html><head>
                        <meta name="viewport" content="width=device-width,initial-scale=1">
                        <style>html,body,#player{margin:0;background:#000;width:100%;height:100%;overflow:hidden}</style>
                        </head><body><div id="player"></div>
                        <script src="https://www.youtube.com/iframe_api"></script>
                        <script>
                        var player;
                        function onYouTubeIframeAPIReady() {
                          player = new YT.Player('player', {
                            width:'100%',height:'100%',videoId:'${videoId}',
                            playerVars:{autoplay:1,playsinline:1,rel:0,controls:1},
                            events:{onStateChange:function(e){
                              if(e.data===YT.PlayerState.ENDED){MyTubeBridge.ended();}
                            }}
                          });
                        }
                        </script></body></html>
                    """.trimIndent()
                    loadDataWithBaseURL("https://www.youtube.com", html, "text/html", "UTF-8", null)
                    webView = this
                }
            },
            update = {}
        )
        IconButton(onClick = onClose, modifier = Modifier.align(Alignment.TopStart).padding(12.dp).background(Color(0x88000000), MaterialTheme.shapes.large)) {
            Icon(Icons.Default.Close, contentDescription = "Close player", tint = Color.White)
        }
    }
}
