package com.eclipse.browser

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.util.TypedValue
import android.view.*
import android.webkit.*
import android.widget.*
import java.io.ByteArrayInputStream
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : Activity() {

    private val HOME_URL  = "file:///android_asset/index.html"
    private val INCOG_URL = "file:///android_asset/index.html#incognito"

    private lateinit var rootLayout   : FrameLayout
    private lateinit var webContainer : FrameLayout
    private lateinit var bottomBar    : LinearLayout
    private lateinit var tabBadge     : TextView
    // Native overlays — shown ON TOP of WebView, work on any site
    private lateinit var nativeTabsOverlay : FrameLayout
    private lateinit var nativeMenuOverlay : FrameLayout

    private lateinit var mainWV  : WebView
    private lateinit var incogWV : WebView

    private var isIncognito = false
    private var tabCount    = 1

    private val activeWV get() = if (isIncognito) incogWV else mainWV

    // Tab data stored natively
    data class TabInfo(val id: Int, var url: String, var title: String, val incognito: Boolean = false)
    private val tabs = mutableListOf<TabInfo>()
    private var activeTabId   = 1
    private var tabIdCounter  = 1

    private val BLOCKED = setOf(
        "doubleclick.net","googlesyndication.com","adservice.google.com",
        "googleadservices.com","googleads.g.doubleclick.net",
        "pagead2.googlesyndication.com","analytics.google.com",
        "google-analytics.com","googletagmanager.com","scorecardresearch.com",
        "quantserve.com","chartbeat.com","hotjar.com","mixpanel.com",
        "adnxs.com","adsrvr.org","pubmatic.com","rubiconproject.com",
        "openx.net","criteo.com","criteo.net","taboola.com","outbrain.com",
        "revcontent.com","mgid.com","advertising.com","amazon-adsystem.com",
        "appnexus.com","indexexchange.com","sharethrough.com","triplelift.com",
        "connect.facebook.net","ads.twitter.com","ads.linkedin.com",
        "ad.youtube.com","ads.youtube.com","2mdn.net","fls.doubleclick.net",
        "mc.yandex.ru","moatads.com","yieldmo.com"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor     = Color.BLACK
        window.navigationBarColor = Color.parseColor("#08080F")
        buildLayout()
        setContentView(rootLayout)
        buildWebViews()
        buildBottomNav()
        buildNativeTabsOverlay()
        buildNativeMenuOverlay()
        tabs.add(TabInfo(1, "home", "New Tab"))
        mainWV.loadUrl(HOME_URL)
    }

    override fun onDestroy() { mainWV.destroy(); incogWV.destroy(); super.onDestroy() }

    @Deprecated("Deprecated")
    override fun onBackPressed() {
        when {
            nativeTabsOverlay.visibility == View.VISIBLE -> hideTabsOverlay()
            nativeMenuOverlay.visibility == View.VISIBLE -> hideMenuOverlay()
            activeWV.canGoBack() -> activeWV.goBack()
            else -> @Suppress("DEPRECATION") super.onBackPressed()
        }
    }

    // ── LAYOUT ───────────────────────────────────────────────────────────────
    private fun buildLayout() {
        rootLayout = FrameLayout(this).apply {
            setBackgroundColor(Color.BLACK)
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }

        // Bottom bar
        bottomBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity     = Gravity.CENTER_VERTICAL
            setBackgroundColor(Color.parseColor("#08080F"))
            setPadding(dp(4), dp(6), dp(4), dp(14))
        }
        val lpBar = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, dp(68)
        ).apply { gravity = Gravity.BOTTOM }
        rootLayout.addView(bottomBar, lpBar)

        // Web container above bottom bar
        webContainer = FrameLayout(this)
        val lpWeb = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        ).apply { bottomMargin = dp(68) }
        rootLayout.addView(webContainer, lpWeb)
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun buildWebViews() {
        val lp = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        )
        mainWV = WebView(this).apply {
            layoutParams = lp
            applyWVSettings(this, false)
            addJavascriptInterface(Bridge(), "Android")
            webViewClient  = makeWVClient()
            webChromeClient = WebChromeClient()
        }
        incogWV = WebView(this).apply {
            layoutParams = lp
            applyWVSettings(this, true)
            addJavascriptInterface(Bridge(), "Android")
            webViewClient  = makeWVClient()
            webChromeClient = WebChromeClient()
            visibility = View.GONE
        }
        webContainer.addView(mainWV)
        webContainer.addView(incogWV)
    }

    // ── BOTTOM NAV ────────────────────────────────────────────────────────────
    private fun dp(v: Int) = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP, v.toFloat(), resources.displayMetrics
    ).toInt()

    private fun navBtn(icon: String, color: String = "#77ffffff"): TextView {
        return TextView(this).apply {
            text = icon
            textSize = 20f
            setTextColor(Color.parseColor(color))
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f)
            isClickable = true; isFocusable = true
        }
    }

    private fun buildBottomNav() {
        // ← Back
        navBtn("←").also { btn ->
            btn.setOnClickListener { if (activeWV.canGoBack()) activeWV.goBack() }
            bottomBar.addView(btn)
        }
        // → Forward
        navBtn("→").also { btn ->
            btn.setOnClickListener { if (activeWV.canGoForward()) activeWV.goForward() }
            bottomBar.addView(btn)
        }
        // ⌂ Home
        val homeFrame = FrameLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f)
        }
        val homeBtn = TextView(this).apply {
            text = "⌂"; textSize = 22f
            setTextColor(Color.parseColor("#E8C840"))
            gravity = Gravity.CENTER
            isClickable = true; isFocusable = true
            layoutParams = FrameLayout.LayoutParams(dp(48), dp(48), Gravity.CENTER)
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#18E8C840"))
                setStroke(dp(1), Color.parseColor("#55E8C840"))
            }
        }
        homeBtn.setOnClickListener {
            if (activeWV.url?.startsWith("file://") == true) {
                activeWV.evaluateJavascript("try{navHome();}catch(e){}", null)
            } else {
                activeWV.loadUrl(if (isIncognito) INCOG_URL else HOME_URL)
            }
        }
        homeFrame.addView(homeBtn)
        bottomBar.addView(homeFrame)

        // ⧉ Tabs
        val tabsFrame = FrameLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f)
        }
        val tabsBtn = TextView(this).apply {
            text = "⧉"; textSize = 20f
            setTextColor(Color.parseColor("#77ffffff"))
            gravity = Gravity.CENTER
            isClickable = true; isFocusable = true
            layoutParams = FrameLayout.LayoutParams(dp(44), dp(44), Gravity.CENTER)
        }
        tabBadge = TextView(this).apply {
            textSize = 7.5f; setTextColor(Color.WHITE); gravity = Gravity.CENTER
            background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(Color.parseColor("#E8C840")) }
            layoutParams = FrameLayout.LayoutParams(dp(15), dp(15), Gravity.TOP or Gravity.END).apply { setMargins(0, dp(6), dp(4), 0) }
            visibility = View.GONE
        }
        tabsBtn.setOnClickListener { showTabsOverlay() }  // ← NATIVE, no home reload
        tabsFrame.addView(tabsBtn); tabsFrame.addView(tabBadge)
        bottomBar.addView(tabsFrame)

        // ⋮ Menu
        navBtn("⋮").also { btn ->
            btn.setOnClickListener { showMenuOverlay() }  // ← NATIVE, no home reload
            bottomBar.addView(btn)
        }
    }

    // ── NATIVE TABS OVERLAY ───────────────────────────────────────────────────
    private fun buildNativeTabsOverlay() {
        nativeTabsOverlay = FrameLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            visibility = View.GONE
            setBackgroundColor(Color.parseColor("#CC000000"))
        }
        nativeTabsOverlay.setOnClickListener { hideTabsOverlay() }

        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#0F0F18"))
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#0F0F18"))
                cornerRadii = floatArrayOf(dp(20).toFloat(), dp(20).toFloat(), dp(20).toFloat(), dp(20).toFloat(), 0f, 0f, 0f, 0f)
            }
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply { gravity = Gravity.BOTTOM; bottomMargin = dp(68) }
            setPadding(dp(16), dp(16), dp(16), dp(20))
        }
        panel.setOnClickListener { /* consume */ }

        // Header
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(44))
        }
        val titleTv = TextView(this).apply {
            text = "Tabs"; textSize = 16f; setTextColor(Color.WHITE)
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        val voidBtn = makeChipBtn("+ Void") { hideTabsOverlay(); newIncognitoTab() }
        val newBtn  = makeChipBtn("+ New")  { hideTabsOverlay(); newNativeTab() }
        val doneBtn = makeChipBtn("Done", gold = true) { hideTabsOverlay() }
        header.addView(titleTv); header.addView(voidBtn); header.addView(newBtn); header.addView(doneBtn)
        panel.addView(header)

        // Scrollable tab grid
        val scroll = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(220))
        }
        val tabGrid = LinearLayout(this).apply {
            id = View.generateViewId()
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        }
        scroll.addView(tabGrid)
        panel.addView(scroll)
        nativeTabsOverlay.addView(panel)

        rootLayout.addView(nativeTabsOverlay, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        ))
    }

    private fun showTabsOverlay() {
        renderNativeTabs()
        nativeTabsOverlay.visibility = View.VISIBLE
    }
    private fun hideTabsOverlay() { nativeTabsOverlay.visibility = View.GONE }

    private fun renderNativeTabs() {
        val overlay = nativeTabsOverlay
        val panel   = overlay.getChildAt(0) as LinearLayout
        val scroll  = panel.getChildAt(1) as ScrollView
        val grid    = scroll.getChildAt(0) as LinearLayout
        grid.removeAllViews()

        tabs.forEach { tab ->
            val isHome   = tab.url == "home"
            val domain   = if (isHome) if (tab.incognito) "🌑 Void" else "🌑 Eclipse"
                           else try { Uri.parse(tab.url).host?.replace("www.", "") ?: tab.url } catch (e: Exception) { tab.url.take(24) }
            val isActive = tab.id == activeTabId

            val row = FrameLayout(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, dp(52)
                ).apply { setMargins(0, 0, 0, dp(6)) }
                background = GradientDrawable().apply {
                    setColor(if (isActive) Color.parseColor("#22E8C840") else Color.parseColor("#15FFFFFF"))
                    cornerRadius = dp(12).toFloat()
                    if (isActive) setStroke(dp(1), Color.parseColor("#55E8C840"))
                }
                setPadding(dp(14), 0, dp(10), 0)
            }

            val textStack = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER_VERTICAL
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                ).apply { marginEnd = dp(44) }
            }
            val titleTv = TextView(this).apply {
                text = tab.title.take(32); textSize = 13f
                setTextColor(if (isActive) Color.parseColor("#E8C840") else Color.WHITE)
                maxLines = 1; ellipsize = android.text.TextUtils.TruncateAt.END
            }
            val domainTv = TextView(this).apply {
                text = domain.take(30); textSize = 11f
                setTextColor(Color.parseColor("#66FFFFFF")); maxLines = 1
            }
            textStack.addView(titleTv); textStack.addView(domainTv)

            val closeBtn = TextView(this).apply {
                text = "✕"; textSize = 14f
                setTextColor(Color.parseColor("#55FFFFFF"))
                gravity = Gravity.CENTER
                layoutParams = FrameLayout.LayoutParams(dp(36), dp(36), Gravity.END or Gravity.CENTER_VERTICAL)
            }

            row.addView(textStack); row.addView(closeBtn)

            row.setOnClickListener {
                hideTabsOverlay()
                switchToTab(tab.id)
            }
            closeBtn.setOnClickListener { removeTab(tab.id) }

            grid.addView(row)
        }
    }

    // ── NATIVE MENU OVERLAY ───────────────────────────────────────────────────
    private fun buildNativeMenuOverlay() {
        nativeMenuOverlay = FrameLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            visibility = View.GONE
            setBackgroundColor(Color.parseColor("#CC000000"))
        }
        nativeMenuOverlay.setOnClickListener { hideMenuOverlay() }

        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#0F0F18"))
                cornerRadii = floatArrayOf(dp(20).toFloat(), dp(20).toFloat(), dp(20).toFloat(), dp(20).toFloat(), 0f, 0f, 0f, 0f)
            }
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply { gravity = Gravity.BOTTOM; bottomMargin = dp(68) }
            setPadding(dp(12), dp(12), dp(12), dp(24))
        }
        panel.setOnClickListener { /* consume */ }

        // Handle bar
        val handle = View(this).apply {
            background = GradientDrawable().apply { setColor(Color.parseColor("#33FFFFFF")); cornerRadius = dp(3).toFloat() }
            layoutParams = LinearLayout.LayoutParams(dp(40), dp(4)).apply {
                gravity = Gravity.CENTER_HORIZONTAL; setMargins(0, 0, 0, dp(14))
            }
        }
        panel.addView(handle)

        // Menu items — full list
        val menuItems = listOf(
            Triple("＋", "New Tab")          { hideMenuOverlay(); newNativeTab() },
            Triple("🌑", "Void Mode")         { hideMenuOverlay(); newIncognitoTab() },
            Triple("─", "separator")          { },
            Triple("🛡", "Eclipse Shield")    { hideMenuOverlay(); triggerHomeJS("openAdPanel()") },
            Triple("♫", "Music Player")       { hideMenuOverlay(); triggerHomeJS("Music.open()") },
            Triple("─", "separator")          { },
            Triple("⏱", "History")            { hideMenuOverlay(); triggerHomeJS("openHistory()") },
            Triple("★", "Bookmarks")          { hideMenuOverlay(); triggerHomeJS("openBookmarks()") },
            Triple("＋★","Add Bookmark")      { hideMenuOverlay(); addBookmarkNative() },
            Triple("─", "separator")          { },
            Triple("⚙", "Customize")          { hideMenuOverlay(); triggerHomeJS("openCustomize()") },
            Triple("↗", "Share Page")         { hideMenuOverlay(); shareCurrentPage() },
            Triple("⎘", "Copy URL")           { hideMenuOverlay(); copyCurrentUrl() },
            Triple("↺", "Reload Page")        { hideMenuOverlay(); activeWV.reload() },
            Triple("🖥", "Desktop Mode")       { hideMenuOverlay(); toggleDesktopMode() },
            Triple("─", "separator")          { },
            Triple("🗑", "Clear History")      { hideMenuOverlay(); triggerHomeJS("Store.clearHistory();showToast('History cleared')") },
        )

        menuItems.forEach { (icon, label, action) ->
            if (label == "separator") {
                panel.addView(View(this).apply {
                    setBackgroundColor(Color.parseColor("#15FFFFFF"))
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(1)).apply {
                        setMargins(dp(12), dp(4), dp(12), dp(4))
                    }
                })
                return@forEach
            }
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(46))
                setPadding(dp(12), 0, dp(12), 0)
                isClickable = true; isFocusable = true
                background = with(android.graphics.drawable.StateListDrawable()) {
                    addState(intArrayOf(android.R.attr.state_pressed), GradientDrawable().apply {
                        setColor(Color.parseColor("#15FFFFFF")); cornerRadius = dp(10).toFloat()
                    })
                    addState(intArrayOf(), android.graphics.drawable.ColorDrawable(Color.TRANSPARENT))
                    this
                }
            }
            val iconTv = TextView(this).apply {
                text = icon; textSize = 16f; gravity = Gravity.CENTER
                setTextColor(Color.parseColor("#CCFFFFFF"))
                layoutParams = LinearLayout.LayoutParams(dp(32), dp(32))
            }
            val labelTv = TextView(this).apply {
                text = label; textSize = 14f
                setTextColor(Color.WHITE)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { marginStart = dp(10) }
            }
            row.addView(iconTv); row.addView(labelTv)
            row.setOnClickListener { action() }
            panel.addView(row)
        }

        nativeMenuOverlay.addView(panel)
        rootLayout.addView(nativeMenuOverlay, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        ))
    }

    private fun showMenuOverlay() { nativeMenuOverlay.visibility = View.VISIBLE }
    private fun hideMenuOverlay() { nativeMenuOverlay.visibility = View.GONE }

    // ── TAB MANAGEMENT ────────────────────────────────────────────────────────
    private fun newNativeTab() {
        val id = ++tabIdCounter
        tabs.add(TabInfo(id, "home", "New Tab"))
        activeTabId = id
        updateTabBadge()
        activeWV.loadUrl(HOME_URL)
    }

    private fun newIncognitoTab() {
        val id = ++tabIdCounter
        tabs.add(TabInfo(id, "home", "Void Tab", incognito = true))
        activeTabId = id
        enterIncognito()
        updateTabBadge()
    }

    private fun switchToTab(tabId: Int) {
        activeTabId = tabId
        val tab = tabs.find { it.id == tabId } ?: return
        if (tab.incognito) {
            enterIncognito()
        } else {
            if (isIncognito) exitIncognito()
            if (tab.url == "home") activeWV.loadUrl(HOME_URL)
            else activeWV.loadUrl(tab.url)
        }
        updateTabBadge()
    }

    private fun removeTab(tabId: Int) {
        if (tabs.size <= 1) { runOnUiThread { showToastNative("Cannot close last tab") }; return }
        tabs.removeAll { it.id == tabId }
        if (activeTabId == tabId) {
            activeTabId = tabs.last().id
            switchToTab(activeTabId)
        }
        renderNativeTabs()
        updateTabBadge()
    }

    private fun updateTabBadge() {
        val count = tabs.size
        if (count > 1) {
            tabBadge.text = count.toString(); tabBadge.visibility = View.VISIBLE
        } else {
            tabBadge.visibility = View.GONE
        }
    }

    // ── INCOGNITO ─────────────────────────────────────────────────────────────
    private fun enterIncognito() {
        isIncognito = true
        mainWV.visibility  = View.GONE
        incogWV.visibility = View.VISIBLE
        CookieManager.getInstance().setAcceptCookie(false)
        CookieManager.getInstance().removeAllCookies(null)
        incogWV.clearCache(true); incogWV.clearHistory()
        incogWV.loadUrl(INCOG_URL)
    }

    private fun exitIncognito() {
        isIncognito = false
        incogWV.loadUrl("about:blank")
        incogWV.clearCache(true); incogWV.clearHistory()
        CookieManager.getInstance().removeAllCookies(null)
        incogWV.visibility = View.GONE
        CookieManager.getInstance().setAcceptCookie(true)
        mainWV.visibility = View.VISIBLE
    }

    // ── MENU ACTIONS ──────────────────────────────────────────────────────────
    // Runs JS on home page — navigates home first if on external site
    private fun triggerHomeJS(js: String) {
        if (activeWV.url?.startsWith("file://") == true) {
            activeWV.evaluateJavascript("try{$js}catch(e){}", null)
        } else {
            // Go home then inject
            _pendingJs = js
            activeWV.loadUrl(if (isIncognito) INCOG_URL else HOME_URL)
        }
    }
    private var _pendingJs: String? = null

    private fun addBookmarkNative() {
        val url = activeWV.url ?: ""
        if (url.isEmpty() || url.startsWith("file://")) { showToastNative("Navigate to a page first"); return }
        val title = activeWV.title ?: url
        triggerHomeJS("try{Store.addBookmark('${url.replace("'","\\'")}','${title.replace("'","\\'")}','🔖');showToast('★ Bookmarked')}catch(e){}")
    }

    private fun shareCurrentPage() {
        val url   = activeWV.url ?: return
        val title = activeWV.title ?: url
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, url)
            putExtra(Intent.EXTRA_SUBJECT, title)
        }
        startActivity(Intent.createChooser(intent, "Share via"))
    }

    private fun copyCurrentUrl() {
        val url = activeWV.url ?: return
        val cm  = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText("URL", url))
        showToastNative("URL copied")
    }

    private var isDesktopMode = false
    private fun toggleDesktopMode() {
        isDesktopMode = !isDesktopMode
        val ua = if (isDesktopMode)
            "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
        else
            "Mozilla/5.0 (Linux; Android 12; Pixel) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
        mainWV.settings.userAgentString = ua
        incogWV.settings.userAgentString = ua
        activeWV.reload()
        showToastNative(if (isDesktopMode) "Desktop mode ON" else "Mobile mode ON")
    }

    private fun showToastNative(msg: String) {
        runOnUiThread { Toast.makeText(this, msg, Toast.LENGTH_SHORT).show() }
    }

    // ── WEBVIEW CLIENT ────────────────────────────────────────────────────────
    @SuppressLint("SetJavaScriptEnabled")
    private fun applyWVSettings(wv: WebView, incognito: Boolean) {
        wv.settings.apply {
            javaScriptEnabled                = true
            domStorageEnabled                = !incognito
            @Suppress("DEPRECATION") allowUniversalAccessFromFileURLs = true
            @Suppress("DEPRECATION") allowFileAccessFromFileURLs      = true
            allowFileAccess                  = true
            mediaPlaybackRequiresUserGesture = false
            mixedContentMode                 = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            cacheMode = if (incognito) WebSettings.LOAD_NO_CACHE else WebSettings.LOAD_DEFAULT
            useWideViewPort = true; loadWithOverviewMode = true
            loadsImagesAutomatically = true; builtInZoomControls = true
            displayZoomControls = false; setSupportZoom(true)
            userAgentString = "Mozilla/5.0 (Linux; Android 12; Pixel) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
        }
        wv.setBackgroundColor(Color.BLACK)
    }

    private fun emptyResponse() = WebResourceResponse("text/plain","utf-8", ByteArrayInputStream(ByteArray(0)))

    private fun makeWVClient(): WebViewClient = object : WebViewClient() {

        // KEY FIX: return false so WebView handles navigation itself → back/forward stack preserved
        override fun shouldOverrideUrlLoading(wv: WebView?, req: WebResourceRequest?): Boolean = false

        override fun onPageFinished(wv: WebView?, url: String?) {
            super.onPageFinished(wv, url)

            // Run pending JS after home load
            _pendingJs?.let { js ->
                if (url?.startsWith("file://") == true) {
                    _pendingJs = null
                    wv?.postDelayed({ wv.evaluateJavascript("try{$js}catch(e){}", null) }, 350)
                }
            }

            // Update tab title
            val tab = tabs.find { it.id == activeTabId }
            if (tab != null) {
                if (url?.startsWith("file://") == true) {
                    tab.url = "home"; tab.title = "New Tab"
                } else if (url != null) {
                    tab.url   = url
                    tab.title = wv?.title?.takeIf { it.isNotBlank() }
                        ?: try { Uri.parse(url).host ?: url } catch (e: Exception) { url.take(28) }
                }
            }
        }

        override fun shouldInterceptRequest(wv: WebView?, req: WebResourceRequest?): WebResourceResponse? {
            val url = req?.url?.toString()?.lowercase() ?: return null
            for (domain in BLOCKED) { if (url.contains(domain)) return emptyResponse() }
            if ((url.contains("youtube.com")||url.contains("googlevideo.com")) &&
                (url.contains("pagead")||url.contains("instream_ad")||url.contains("doubleclick"))) return emptyResponse()
            return super.shouldInterceptRequest(wv, req)
        }
    }

    // ── JS BRIDGE ─────────────────────────────────────────────────────────────
    inner class Bridge {
        @JavascriptInterface fun openUrl(url: String)    = runOnUiThread { activeWV.loadUrl(url) }
        @JavascriptInterface fun goHome()                = runOnUiThread { activeWV.loadUrl(if (isIncognito) INCOG_URL else HOME_URL) }
        @JavascriptInterface fun goBack()                = runOnUiThread { if (activeWV.canGoBack()) activeWV.goBack() }
        @JavascriptInterface fun goForward()             = runOnUiThread { if (activeWV.canGoForward()) activeWV.goForward() }
        @JavascriptInterface fun openIncognito()         = runOnUiThread { enterIncognito() }
        @JavascriptInterface fun exitIncognito()         = runOnUiThread { this@MainActivity.exitIncognito() }
        @JavascriptInterface fun updateTabCount(n: Int)  = runOnUiThread { updateTabBadge() }
        @JavascriptInterface fun isIncognitoMode()       = isIncognito
        @JavascriptInterface fun getCurrentUrl()         = activeWV.url ?: ""
        @JavascriptInterface fun fetchUrl(url: String, cbId: String) {
            Thread {
                try {
                    val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                        requestMethod = "GET"
                        setRequestProperty("User-Agent","Mozilla/5.0 (Linux; Android 12) AppleWebKit/537.36 Chrome/120.0 Mobile Safari/537.36")
                        setRequestProperty("Accept","text/html,application/xml,*/*")
                        connectTimeout = 12000; readTimeout = 12000; instanceFollowRedirects = true
                    }
                    val bytes   = (if (conn.responseCode in 200..299) conn.inputStream else conn.errorStream)?.readBytes() ?: ByteArray(0)
                    val encoded = Base64.encodeToString(bytes, Base64.NO_WRAP)
                    runOnUiThread { activeWV.evaluateJavascript("try{EclFetchCallback('$cbId','$encoded',null)}catch(e){}", null) }
                } catch (e: Exception) {
                    val err = (e.message ?: "error").replace("'","\\'").take(100)
                    runOnUiThread { activeWV.evaluateJavascript("try{EclFetchCallback('$cbId',null,'$err')}catch(e){}", null) }
                }
            }.start()
        }
    }

    // Helper for chip buttons
    private fun makeChipBtn(label: String, gold: Boolean = false, action: () -> Unit): TextView {
        return TextView(this).apply {
            text = label; textSize = 12f
            setTextColor(if (gold) Color.parseColor("#E8C840") else Color.parseColor("#AAFFFFFF"))
            setPadding(dp(10), dp(4), dp(10), dp(4))
            background = GradientDrawable().apply {
                setColor(if (gold) Color.parseColor("#22E8C840") else Color.parseColor("#18FFFFFF"))
                cornerRadius = dp(8).toFloat()
                if (gold) setStroke(dp(1), Color.parseColor("#44E8C840"))
            }
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, dp(30)).apply { marginStart = dp(6) }
            gravity = Gravity.CENTER
            setOnClickListener { action() }
        }
    }
}
