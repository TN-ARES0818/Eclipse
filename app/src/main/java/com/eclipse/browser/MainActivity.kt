package com.eclipse.browser

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.*
import android.util.Base64
import android.util.TypedValue
import android.view.*
import android.webkit.*
import android.widget.*
import java.io.ByteArrayInputStream
import java.net.HttpURLConnection
import java.net.URL

@SuppressLint("SetJavaScriptEnabled")
class MainActivity : Activity() {

    private val HOME_URL  = "file:///android_asset/index.html"
    private val INCOG_URL = "file:///android_asset/index.html#incognito"
    private val DESKTOP_UA = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
    private val MOBILE_UA = "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"

    // ── Views ──────────────────────────────────────────────────────────────
    private lateinit var webContainer : FrameLayout
    private lateinit var tabBadge     : TextView
    private lateinit var menuOverlay  : FrameLayout

    // ── WebViews ───────────────────────────────────────────────────────────
    private lateinit var mainWV  : WebView
    private lateinit var incogWV : WebView

    // ── State ──────────────────────────────────────────────────────────────
    private var isIncognito = false
    private var desktopMode = false
    private var pendingJs   : String? = null
    private var tabCount    = 1
    private var menuVisible = false

    private val activeWV get() = if (isIncognito) incogWV else mainWV

    // ── Ad blocklist ───────────────────────────────────────────────────────
    private val BLOCKED = setOf(
        "doubleclick.net","googlesyndication.com","adservice.google.com",
        "googleadservices.com","googleads.g.doubleclick.net",
        "pagead2.googlesyndication.com","partner.googleadservices.com",
        "analytics.google.com","google-analytics.com","googletagmanager.com",
        "stats.g.doubleclick.net","scorecardresearch.com","quantserve.com",
        "chartbeat.com","hotjar.com","mixpanel.com","amplitude.com",
        "adnxs.com","adsrvr.org","pubmatic.com","rubiconproject.com",
        "openx.net","criteo.com","criteo.net","taboola.com","outbrain.com",
        "revcontent.com","mgid.com","advertising.com","adblade.com",
        "amazon-adsystem.com","appnexus.com","districtm.io",
        "indexexchange.com","sharethrough.com","smartadserver.com",
        "triplelift.com","connect.facebook.net","ads.twitter.com",
        "ads.linkedin.com","ad.youtube.com","ads.youtube.com","2mdn.net",
        "fls.doubleclick.net","mc.yandex.ru","krux.net","bluekai.com",
        "addthis.com","zedo.com","moatads.com","yieldmo.com"
    )

    // ═══════════════════════════════════════════════════════════════════════
    //  LIFECYCLE
    // ═══════════════════════════════════════════════════════════════════════
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        hideSystemUI()

        setContentView(R.layout.activity_main)
        webContainer = findViewById(R.id.web_container)
        menuOverlay = findViewById(R.id.menu_overlay)

        buildWebViews()
        buildBottomNav()
        buildNativeMenu()   // ← Kotlin menu that works on ANY page
        applyWindowInsets()

        mainWV.loadUrl(HOME_URL)
    }

    override fun onDestroy() { mainWV.destroy(); incogWV.destroy(); super.onDestroy() }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            hideSystemUI()
        }
    }

    @Deprecated("Deprecated")
    override fun onBackPressed() {
        if (menuVisible) { hideNativeMenu(); return }
        if (activeWV.canGoBack()) activeWV.goBack()
        else @Suppress("DEPRECATION") super.onBackPressed()
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  LAYOUT
    // ═══════════════════════════════════════════════════════════════════════

    private fun hideSystemUI() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_FULLSCREEN)
    }

    // Apply system insets so bottom bar respects gesture navigation
    private fun applyWindowInsets() {
        findViewById<FrameLayout>(R.id.web_container).setOnApplyWindowInsetsListener { _, insets ->
            val systemBottom = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                insets.getInsets(android.view.WindowInsets.Type.systemBars()).bottom
            } else {
                @Suppress("DEPRECATION") insets.systemWindowInsetBottom
            }
            val topInset = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                insets.getInsets(android.view.WindowInsets.Type.systemBars()).top
            } else {
                @Suppress("DEPRECATION") insets.systemWindowInsetTop
            }
            val bottomNav = findViewById<LinearLayout>(R.id.bottom_nav)
            val lpBar = bottomNav.layoutParams as FrameLayout.LayoutParams
            lpBar.height = dp(56) + systemBottom
            bottomNav.layoutParams = lpBar
            bottomNav.setPadding(dp(4), dp(4), dp(4), systemBottom + dp(4))

            val lpWeb = webContainer.layoutParams as FrameLayout.LayoutParams
            lpWeb.topMargin = topInset
            webContainer.layoutParams = lpWeb
            insets
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  WEBVIEWS
    // ═══════════════════════════════════════════════════════════════════════
    private fun applyWVSettings(wv: WebView, incognito: Boolean) {
        wv.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = !incognito
            @Suppress("DEPRECATION") allowUniversalAccessFromFileURLs = true
            @Suppress("DEPRECATION") allowFileAccessFromFileURLs      = true
            allowFileAccess                  = true
            mediaPlaybackRequiresUserGesture = false
            mixedContentMode                 = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            cacheMode = if (incognito) WebSettings.LOAD_NO_CACHE else WebSettings.LOAD_DEFAULT
            useWideViewPort        = true
            loadWithOverviewMode   = true
            loadsImagesAutomatically = true
            builtInZoomControls    = true
            displayZoomControls    = false
            setSupportZoom(true)
            userAgentString = if (desktopMode) DESKTOP_UA else MOBILE_UA
        }
        wv.setBackgroundColor(Color.BLACK)
    }

    private fun emptyResponse() =
        WebResourceResponse("text/plain","utf-8", ByteArrayInputStream(ByteArray(0)))

    private fun makeWVClient(): WebViewClient = object : WebViewClient() {
        override fun shouldOverrideUrlLoading(wv: WebView?, req: WebResourceRequest?): Boolean {
            val url = req?.url?.toString() ?: return false
            wv?.loadUrl(url)
            return true
        }

        override fun onPageFinished(wv: WebView?, url: String?) {
            super.onPageFinished(wv, url)
            pendingJs?.let { js ->
                if (url?.startsWith("file://") == true) {
                    pendingJs = null
                    wv?.postDelayed({ wv.evaluateJavascript(js, null) }, 400)
                }
            }
            if (url?.startsWith("file://") == true) {
                wv?.evaluateJavascript(
                    "try{var t=App&&App.tabs&&App.tabs.find(function(x){return x.id===App.activeTabId;});" +
                    "if(t){t.url='home';t.title='New Tab';" +
                    "if(typeof Store!=='undefined')Store.saveTabs(App.tabs);}}catch(e){}", null)
            }
            if (!isIncognito && url != null && !url.startsWith("file://")) {
                val title   = (wv?.title ?: "").replace("'","\\'").take(80)
                val safeUrl = url.replace("'","\\'").take(300)
                wv?.evaluateJavascript(
                    "try{if(typeof Store!=='undefined')Store.addHistory('$safeUrl','$title');}catch(e){}", null)
            }
        }

        override fun shouldInterceptRequest(wv: WebView?, req: WebResourceRequest?): WebResourceResponse? {
            val url = req?.url?.toString()?.lowercase() ?: return null
            for (d in BLOCKED) { if (url.contains(d)) return emptyResponse() }
            if ((url.contains("youtube.com") || url.contains("googlevideo.com")) &&
                (url.contains("pagead") || url.contains("instream_ad") ||
                 url.contains("api/stats/ads") || url.contains("doubleclick")))
                return emptyResponse()
            return super.shouldInterceptRequest(wv, req)
        }
    }

    private fun buildWebViews() {
        val lp = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)

        mainWV = WebView(this).apply {
            layoutParams = lp
            applyWVSettings(this, false)
            addJavascriptInterface(Bridge(), "Android")
            webViewClient   = makeWVClient()
            webChromeClient = WebChromeClient()
            setDownloadListener { url, ua, cd, mime, _ -> startDownload(url, ua, cd, mime) }
        }

        incogWV = WebView(this).apply {
            layoutParams = lp
            applyWVSettings(this, true)
            addJavascriptInterface(Bridge(), "Android")
            webViewClient   = makeWVClient()
            webChromeClient = WebChromeClient()
            visibility = View.GONE
            setDownloadListener { url, ua, cd, mime, _ -> startDownload(url, ua, cd, mime) }
        }

        webContainer.addView(mainWV)
        webContainer.addView(incogWV)
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  DOWNLOAD HANDLER
    // ═══════════════════════════════════════════════════════════════════════
    private fun startDownload(url: String, ua: String, contentDisposition: String, mimeType: String) {
        try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
                checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 1001)
            }
            val filename = URLUtil.guessFileName(url, contentDisposition, mimeType)
            val req = DownloadManager.Request(Uri.parse(url)).apply {
                setMimeType(mimeType)
                addRequestHeader("User-Agent", ua)
                addRequestHeader("Cookie", CookieManager.getInstance().getCookie(url) ?: "")
                setTitle(filename)
                setDescription("Eclipse Browser Download")
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename)
                allowScanningByMediaScanner()
            }
            val dm = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            dm.enqueue(req)
            runOnUiThread { showToastNative("⬇ Downloading $filename") }
        } catch (e: Exception) {
            // Fallback: open in browser
            runOnUiThread {
                try {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                } catch (ex: Exception) {
                    showToastNative("Download failed: ${e.message}")
                }
            }
        }
    }

    private fun showToastNative(msg: String) {
        android.widget.Toast.makeText(this, msg, android.widget.Toast.LENGTH_SHORT).show()
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  BOTTOM NAV — always visible, 5 buttons
    // ═══════════════════════════════════════════════════════════════════════
    private fun dp(v: Int) = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP, v.toFloat(), resources.displayMetrics).toInt()

    private fun buildBottomNav() {
        findViewById<ImageView>(R.id.nav_back).setOnClickListener {
            if (activeWV.canGoBack()) activeWV.goBack()
        }
        findViewById<ImageView>(R.id.nav_forward).setOnClickListener {
            if (activeWV.canGoForward()) activeWV.goForward()
        }
        findViewById<ImageView>(R.id.nav_home).setOnClickListener {
            if (menuVisible) { hideNativeMenu(); return@setOnClickListener }
            if (activeWV.url?.startsWith("file://") == true) {
                activeWV.evaluateJavascript("try{navHome();}catch(e){}", null)
            } else {
                activeWV.loadUrl(if (isIncognito) INCOG_URL else HOME_URL)
            }
        }
        findViewById<FrameLayout>(R.id.nav_tabs).setOnClickListener {
            openTabsAction()
        }
        tabBadge = findViewById(R.id.nav_tabs_badge)
        findViewById<ImageView>(R.id.nav_menu).setOnClickListener {
            toggleNativeMenu()
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  NATIVE KOTLIN MENU — works on ANY page, no home navigation
    // ═══════════════════════════════════════════════════════════════════════
    private lateinit var menuSheet : LinearLayout

    private fun buildNativeMenu() {
        // Dim backdrop
        menuOverlay.setBackgroundColor(Color.parseColor("#99000000"))

        menuSheet = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#0A0A18"))
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#0C0C1E"))
                cornerRadii = floatArrayOf(dp(22).toFloat(), dp(22).toFloat(),
                    dp(22).toFloat(), dp(22).toFloat(), 0f, 0f, 0f, 0f)
            }
            elevation = dp(8).toFloat()
            setPadding(0, dp(8), 0, dp(20))
        }
        val lp = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT,
            Gravity.BOTTOM)
        menuSheet.setOnClickListener { /* consume, don't close */ }

        // Handle bar
        val handle = View(this).apply {
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#33ffffff"))
                cornerRadius = dp(2).toFloat()
            }
            layoutParams = LinearLayout.LayoutParams(dp(36), dp(4)).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                topMargin = dp(4); bottomMargin = dp(12)
            }
        }
        // Center handle in a parent
        val handleRow = FrameLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(20))
        }
        handleRow.addView(handle, FrameLayout.LayoutParams(dp(36), dp(4), Gravity.CENTER))
        menuSheet.addView(handleRow)

        // Menu items
        val items = listOf(
            Triple("🆕", "New Tab",         Runnable { hideNativeMenu(); openNewTab() }),
            Triple("↻", "Refresh",         Runnable { hideNativeMenu(); activeWV.reload() }),
            Triple("🌑", "Void Mode",        Runnable { hideNativeMenu(); openVoidMode() }),
            Triple("🖥️", "Desktop Mode",     Runnable { hideNativeMenu(); toggleDesktopMode() }),
            Triple("🛡️", "Eclipse Shield",   Runnable { hideNativeMenu(); openShieldFromNative() }),
            Triple("🎵", "Music Player",     Runnable { hideNativeMenu(); openMusicFromNative() }),
            Triple("📜", "History",          Runnable { hideNativeMenu(); openHistoryFromNative() }),
            Triple("⭐", "Bookmarks",        Runnable { hideNativeMenu(); openBookmarksFromNative() }),
            Triple("⭐+", "Add Bookmark",    Runnable { hideNativeMenu(); addBookmarkFromNative() }),
            Triple("⬇️", "Downloads",        Runnable { hideNativeMenu(); openDownloads() }),
            Triple("⚙️", "Customize",        Runnable { hideNativeMenu(); openCustomizeFromNative() }),
            Triple("🗑️", "Clear History",    Runnable { hideNativeMenu(); clearHistoryFromNative() }),
        )

        items.forEachIndexed { i, (icon, label, action) ->
            if (i == 4 || i == 9) menuSheet.addView(makeDivider())
            menuSheet.addView(makeMenuItem(icon, label, action))
        }

        menuOverlay.addView(menuSheet, lp)
    }

    private fun makeDivider(): View = View(this).apply {
        setBackgroundColor(Color.parseColor("#15ffffff"))
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(1)).apply {
            setMargins(dp(16), dp(4), dp(16), dp(4))
        }
    }

    private fun makeMenuItem(icon: String, label: String, action: Runnable): View {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(20), dp(14), dp(20), dp(14))
            isClickable = true; isFocusable = true
            setOnClickListener { action.run() }
        }
        val iconTv = TextView(this).apply {
            text = icon; textSize = 18f; gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(dp(32), dp(32))
        }
        val labelTv = TextView(this).apply {
            text = label
            setTextColor(Color.parseColor("#EEF0FF"))
            textSize = 15f
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT).apply { marginStart = dp(14) }
            typeface = android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.NORMAL)
        }
        row.addView(iconTv); row.addView(labelTv)
        return row
    }

    private fun toggleNativeMenu() { if (menuVisible) hideNativeMenu() else showNativeMenu() }

    private fun showNativeMenu() {
        menuOverlay.visibility = View.VISIBLE
        menuOverlay.alpha = 0f
        menuOverlay.animate().alpha(1f).setDuration(200).start()
        menuSheet.translationY = dp(400).toFloat()
        menuSheet.animate().translationY(0f).setDuration(280)
            .setInterpolator(android.view.animation.DecelerateInterpolator(2f)).start()
        menuVisible = true
    }

    private fun hideNativeMenu() {
        menuSheet.animate().translationY(dp(500).toFloat()).setDuration(220)
            .setInterpolator(android.view.animation.AccelerateInterpolator()).start()
        menuOverlay.animate().alpha(0f).setDuration(200).withEndAction {
            menuOverlay.visibility = View.GONE
        }.start()
        menuVisible = false
    }

    // Menu action helpers — call JS if on home page, otherwise show native toast / do native action
    private fun callJs(js: String) {
        if (activeWV.url?.startsWith("file://") == true) {
            activeWV.evaluateJavascript(js, null)
        } else {
            pendingJs = js
            activeWV.loadUrl(if (isIncognito) INCOG_URL else HOME_URL)
        }
    }
    private fun openNewTab()            = callJs("try{newTab();}catch(e){}")
    private fun openVoidMode()          = callJs("try{newIncognitoTab();}catch(e){}")
    private fun openShieldFromNative()  = callJs("try{openAdPanel();}catch(e){}")
    private fun openMusicFromNative()   = callJs("try{Music.open();}catch(e){}")
    private fun openHistoryFromNative() = callJs("try{openHistory();}catch(e){}")
    private fun openBookmarksFromNative()= callJs("try{openBookmarks();}catch(e){}")
    private fun addBookmarkFromNative() = callJs("try{addBookmarkCurrent();}catch(e){}")
    private fun openCustomizeFromNative()= callJs("try{Customize.open();}catch(e){}")
    private fun clearHistoryFromNative()= callJs("try{Store.clearHistory();showToast('History cleared');}catch(e){}")

    private fun toggleDesktopMode() {
        desktopMode = !desktopMode
        val newUA = if (desktopMode) DESKTOP_UA else MOBILE_UA
        mainWV.settings.userAgentString = newUA
        incogWV.settings.userAgentString = newUA
        // Reload current page if it's not a local file
        val currentUrl = activeWV.url
        if (currentUrl != null && !currentUrl.startsWith("file://")) {
            activeWV.reload()
        }
        showToastNative(if (desktopMode) "🖥️ Desktop Mode ON" else "📱 Mobile Mode ON")
    }

    private fun openDownloads() {
        try {
            startActivity(Intent(DownloadManager.ACTION_VIEW_DOWNLOADS))
        } catch (e: Exception) {
            showToastNative("Downloads folder: /sdcard/Download")
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  TABS
    // ═══════════════════════════════════════════════════════════════════════
    private fun openTabsAction() {
        if (activeWV.url?.startsWith("file://") == true) {
            activeWV.evaluateJavascript("try{openTabsPanel();}catch(e){}", null)
        } else {
            pendingJs = "try{openTabsPanel();}catch(e){}"
            activeWV.loadUrl(if (isIncognito) INCOG_URL else HOME_URL)
        }
    }

    private fun setTabBadge(count: Int) {
        tabCount = count
        if (count > 1) { tabBadge.text = count.toString(); tabBadge.visibility = View.VISIBLE }
        else tabBadge.visibility = View.GONE
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  INCOGNITO
    // ═══════════════════════════════════════════════════════════════════════
    private fun enterIncognito() {
        isIncognito = true
        mainWV.visibility = View.GONE; incogWV.visibility = View.VISIBLE
        CookieManager.getInstance().setAcceptCookie(false)
        CookieManager.getInstance().removeAllCookies(null)
        incogWV.clearCache(true); incogWV.clearHistory()
        incogWV.loadUrl(INCOG_URL)
        runOnUiThread { setTabBadge(tabCount + 1) }
    }

    private fun exitIncognito() {
        isIncognito = false
        incogWV.loadUrl("about:blank"); incogWV.clearCache(true); incogWV.clearHistory()
        CookieManager.getInstance().removeAllCookies(null)
        incogWV.visibility = View.GONE
        CookieManager.getInstance().setAcceptCookie(true)
        mainWV.visibility = View.VISIBLE
        runOnUiThread { setTabBadge(tabCount) }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  JS BRIDGE
    // ═══════════════════════════════════════════════════════════════════════
    inner class Bridge {
        @JavascriptInterface fun openUrl(url: String) = runOnUiThread { activeWV.loadUrl(url) }
        @JavascriptInterface fun goHome() = runOnUiThread { activeWV.loadUrl(if (isIncognito) INCOG_URL else HOME_URL) }
        @JavascriptInterface fun goBack() = runOnUiThread { if (activeWV.canGoBack()) activeWV.goBack() }
        @JavascriptInterface fun goForward() = runOnUiThread { if (activeWV.canGoForward()) activeWV.goForward() }
        @JavascriptInterface fun openIncognito() = runOnUiThread { enterIncognito() }
        @JavascriptInterface fun exitIncognito() = runOnUiThread { this@MainActivity.exitIncognito() }
        @JavascriptInterface fun updateTabCount(count: Int) = runOnUiThread { setTabBadge(count) }
        @JavascriptInterface fun isIncognitoMode(): Boolean = isIncognito
        @JavascriptInterface fun getCurrentUrl(): String = activeWV.url ?: ""

        @JavascriptInterface
        fun fetchUrl(url: String, callbackId: String) {
            Thread {
                try {
                    val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                        requestMethod = "GET"
                        setRequestProperty("User-Agent", activeWV.settings.userAgentString)
                        setRequestProperty("Accept", "text/html,application/xml,*/*")
                        connectTimeout = 15000; readTimeout = 15000
                        instanceFollowRedirects = true
                    }
                    val bytes = (if (conn.responseCode in 200..299) conn.inputStream else conn.errorStream)
                        ?.readBytes() ?: ByteArray(0)
                    val enc = Base64.encodeToString(bytes, Base64.NO_WRAP)
                    runOnUiThread {
                        activeWV.evaluateJavascript("try{EclFetchCallback('$callbackId','$enc',null);}catch(e){}", null)
                    }
                } catch (e: Exception) {
                    val err = (e.message ?: "error").replace("'", "\\'").take(120)
                    runOnUiThread {
                        activeWV.evaluateJavascript("try{EclFetchCallback('$callbackId',null,'$err');}catch(e){}", null)
                    }
                }
            }.start()
        }
    }
}
