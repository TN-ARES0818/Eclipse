package com.eclipse.browser.ui.screens

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.net.Uri
import android.webkit.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.eclipse.browser.ui.theme.*
import java.io.ByteArrayInputStream

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebViewScreen(
    url: String,
    adBlockOn: Boolean,
    accentColor: Color,
    onPageLoaded: (url: String, title: String) -> Unit,
    onNavigateHome: () -> Unit,
    blockedDomains: Set<String>,
    onWebViewState: (canGoBack: Boolean, canGoForward: Boolean, goBack: () -> Unit, goForward: () -> Unit) -> Unit = { _, _, _, _ -> },
    modifier: Modifier = Modifier
) {
    var progress by remember { mutableFloatStateOf(0f) }
    var pageTitle by remember { mutableStateOf("") }
    var pageDomain by remember { mutableStateOf("") }
    var webView by remember { mutableStateOf<WebView?>(null) }

    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(300),
        label = "webProgress"
    )

    Column(modifier = modifier.fillMaxSize()) {
        // URL bar at top
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(BottomBarBg)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Back
            Text(
                text = "←",
                color = TextMuted,
                fontSize = 18.sp,
                modifier = Modifier
                    .clickable { webView?.let { if (it.canGoBack()) it.goBack() else onNavigateHome() } }
                    .padding(8.dp)
            )

            // Domain display
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp)
            ) {
                if (pageTitle.isNotBlank()) {
                    Text(
                        text = pageTitle,
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontFamily = Outfit,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = pageDomain,
                    color = TextMuted,
                    fontSize = 11.sp,
                    fontFamily = SpaceMono,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Close
            Text(
                text = "✕",
                color = TextMuted,
                fontSize = 16.sp,
                modifier = Modifier
                    .clickable { onNavigateHome() }
                    .padding(8.dp)
            )
        }

        // Progress bar
        if (animatedProgress > 0f && animatedProgress < 1f) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedProgress)
                    .height(2.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(accentColor, accentColor.copy(alpha = 0.4f))
                        )
                    )
            )
        }

        // WebView
        AndroidView(
            factory = { context ->
                WebView(context).apply {
                    setBackgroundColor(android.graphics.Color.BLACK)
                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        useWideViewPort = true
                        loadWithOverviewMode = true
                        loadsImagesAutomatically = true
                        builtInZoomControls = true
                        displayZoomControls = false
                        setSupportZoom(true)
                        mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                        cacheMode = WebSettings.LOAD_DEFAULT
                        mediaPlaybackRequiresUserGesture = false
                        userAgentString = "Mozilla/5.0 (Linux; Android 12; Pixel) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
                    }

                    webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean = false

                        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                            super.onPageStarted(view, url, favicon)
                            progress = 0.1f
                        }

                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            progress = 1f
                            pageTitle = view?.title ?: ""
                            pageDomain = try {
                                Uri.parse(url ?: "").host?.replace("www.", "") ?: ""
                            } catch (_: Exception) { "" }
                            if (url != null) {
                                onPageLoaded(url, pageTitle)
                            }
                            view?.let { wv ->
                                onWebViewState(
                                    wv.canGoBack(),
                                    wv.canGoForward(),
                                    { wv.goBack() },
                                    { wv.goForward() }
                                )
                            }
                        }

                        override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
                            if (!adBlockOn) return super.shouldInterceptRequest(view, request)
                            val reqUrl = request?.url?.toString()?.lowercase() ?: return null
                            for (domain in blockedDomains) {
                                if (reqUrl.contains(domain)) {
                                    return WebResourceResponse("text/plain", "utf-8", ByteArrayInputStream(ByteArray(0)))
                                }
                            }
                            return super.shouldInterceptRequest(view, request)
                        }
                    }

                    webChromeClient = object : WebChromeClient() {
                        override fun onProgressChanged(view: WebView?, newProgress: Int) {
                            progress = newProgress / 100f
                        }
                    }

                    loadUrl(url)
                    webView = this
                }
            },
            update = { wv ->
                if (wv.url != url) {
                    wv.loadUrl(url)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        )
    }
}
