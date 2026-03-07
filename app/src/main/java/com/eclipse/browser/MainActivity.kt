package com.eclipse.browser

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.eclipse.browser.ui.components.BottomNavBar
import com.eclipse.browser.ui.components.EclipseToast
import com.eclipse.browser.ui.screens.*
import com.eclipse.browser.ui.sheets.*
import com.eclipse.browser.ui.viewmodel.HomeViewModel
import com.eclipse.browser.ui.viewmodel.Screen

class MainActivity : ComponentActivity() {

    private val viewModel: HomeViewModel by viewModels()

    companion object {
        val BLOCKED_DOMAINS = setOf(
            "doubleclick.net", "googlesyndication.com", "googleadservices.com",
            "google-analytics.com", "adservice.google.com", "pagead2.googlesyndication.com",
            "ads.yahoo.com", "analytics.yahoo.com", "ad.doubleclick.net",
            "facebook.com/tr", "connect.facebook.net/en_US/fbevents",
            "amazon-adsystem.com", "aax.amazon-adsystem.com",
            "ads-api.twitter.com", "analytics.twitter.com",
            "adsserver.", "adserver.", "adtech.", "adnxs.com",
            "advertising.com", "outbrain.com", "taboola.com",
            "scorecardresearch.com", "quantserve.com", "criteo.com",
            "hotjar.com", "mouseflow.com", "fullstory.com",
            "mixpanel.com", "segment.com", "amplitude.com",
            "newrelic.com", "nr-data.net", "sentry.io",
            "pubmatic.com", "openx.net", "rubiconproject.com",
            "casalemedia.com", "indexexchange.com", "smartadserver.com",
            "moatads.com", "doubleverify.com", "adsafeprotected.com"
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            EclipseApp(viewModel)
        }
    }
}

@Composable
fun EclipseApp(viewModel: HomeViewModel) {
    val state by viewModel.uiState.collectAsState()

    var canGoBack by remember { mutableStateOf(false) }
    var canGoForward by remember { mutableStateOf(false) }
    var webViewGoBack by remember { mutableStateOf<(() -> Unit)?>(null) }
    var webViewGoForward by remember { mutableStateOf<(() -> Unit)?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Main content area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .statusBarsPadding()
            ) {
                when (state.currentScreen) {
                    Screen.HOME -> {
                        HomeScreen(
                            state = state,
                            onSearch = { query ->
                                val url = viewModel.buildSearchUrl(query)
                                viewModel.navigateTo(url)
                            },
                            onTabSelected = { viewModel.setActiveSearchTab(it) },
                            onSiteClick = { viewModel.navigateTo(it) },
                            onSiteLongPress = { viewModel.removeQuickSite(it) },
                            onAddSiteClick = { viewModel.showSheet("customize") }
                        )
                    }

                    Screen.WEBVIEW -> {
                        state.webViewUrl?.let { url ->
                            WebViewScreen(
                                url = url,
                                adBlockOn = state.adBlockOn,
                                accentColor = state.accentColor,
                                onPageLoaded = { loadedUrl, title ->
                                    viewModel.updateTabInfo(loadedUrl, title)
                                    viewModel.addToHistory(loadedUrl, title)
                                },
                                onNavigateHome = { viewModel.goHome() },
                                blockedDomains = MainActivity.BLOCKED_DOMAINS,
                                onWebViewState = { back, forward, goBackFn, goForwardFn ->
                                    canGoBack = back
                                    canGoForward = forward
                                    webViewGoBack = goBackFn
                                    webViewGoForward = goForwardFn
                                }
                            )
                        }
                    }

                    Screen.INCOGNITO -> {
                        IncognitoScreen(
                            onSearch = { query ->
                                val url = viewModel.buildSearchUrl(query)
                                viewModel.navigateTo(url)
                            },
                            onExit = { viewModel.exitIncognito() }
                        )
                    }
                }
            }

            // Bottom Navigation Bar
            BottomNavBar(
                accentColor = state.accentColor,
                tabCount = state.tabs.size,
                canGoBack = canGoBack || state.currentScreen == Screen.WEBVIEW,
                canGoForward = canGoForward,
                isIncognito = state.isIncognito,
                onBack = {
                    if (canGoBack) webViewGoBack?.invoke()
                    else if (state.currentScreen == Screen.WEBVIEW) viewModel.goHome()
                },
                onForward = { if (canGoForward) webViewGoForward?.invoke() },
                onHome = { viewModel.goHome() },
                onTabs = { viewModel.showSheet("tabs") },
                onMenu = { viewModel.showSheet("menu") }
            )
        }

        // Toast overlay
        state.toastMessage?.let { msg ->
            EclipseToast(message = msg)
        }

        // Bottom Sheets
        if (state.showCustomize) {
            CustomizeSheet(
                accentColor = state.accentColor,
                accentHex = state.accentHex,
                bgTheme = state.bgTheme,
                searchEngine = state.searchEngine,
                uiStyle = state.uiStyle,
                particlesOn = state.particlesOn,
                starsOn = state.starsOn,
                orbOn = state.orbOn,
                weatherOn = state.weatherOn,
                onAccentChange = { hex, light -> viewModel.setAccentColor(hex, light) },
                onThemeChange = { viewModel.setTheme(it) },
                onEngineChange = { viewModel.setSearchEngine(it) },
                onUiStyleChange = { viewModel.setUiStyle(it) },
                onToggleParticles = { viewModel.toggleParticles(it) },
                onToggleStars = { viewModel.toggleStars(it) },
                onToggleOrb = { viewModel.toggleOrb(it) },
                onToggleWeather = { viewModel.toggleWeather(it) },
                onDismiss = { viewModel.hideSheet("customize") }
            )
        }

        if (state.showTabs) {
            TabsSheet(
                tabs = state.tabs,
                activeTabId = state.activeTabId,
                accentColor = state.accentColor,
                onTabClick = { viewModel.switchToTab(it) },
                onCloseTab = { viewModel.closeTab(it) },
                onNewTab = { viewModel.newTab() },
                onDismiss = { viewModel.hideSheet("tabs") }
            )
        }

        if (state.showMenu) {
            MenuSheet(
                accentColor = state.accentColor,
                isIncognito = state.isIncognito,
                adBlockOn = state.adBlockOn,
                adsBlocked = state.adsBlocked,
                trackersBlocked = state.trackersBlocked,
                onToggleAdBlock = { viewModel.toggleAdBlock(it) },
                onShowCustomize = { viewModel.showSheet("customize") },
                onShowHistory = { viewModel.showSheet("history") },
                onShowBookmarks = { viewModel.showSheet("bookmarks") },
                onShowAdPanel = { viewModel.showSheet("adpanel") },
                onShowAbout = { viewModel.showSheet("about") },
                onEnterIncognito = { viewModel.enterIncognito() },
                onExitIncognito = { viewModel.exitIncognito() },
                onDismiss = { viewModel.hideSheet("menu") }
            )
        }

        if (state.showHistory) {
            HistorySheet(
                history = state.history,
                accentColor = state.accentColor,
                onItemClick = { viewModel.navigateTo(it) },
                onDeleteItem = { viewModel.deleteHistoryItem(it) },
                onClearAll = { viewModel.clearHistory() },
                onDismiss = { viewModel.hideSheet("history") }
            )
        }

        if (state.showBookmarks) {
            BookmarksSheet(
                bookmarks = state.bookmarks,
                accentColor = state.accentColor,
                onItemClick = { viewModel.navigateTo(it) },
                onDeleteItem = { viewModel.removeBookmark(it) },
                onDismiss = { viewModel.hideSheet("bookmarks") }
            )
        }

        if (state.showAbout) {
            AboutSheet(
                accentColor = state.accentColor,
                onDismiss = { viewModel.hideSheet("about") }
            )
        }

        if (state.showAdPanel) {
            AdPanelSheet(
                adBlockOn = state.adBlockOn,
                adsBlocked = state.adsBlocked,
                trackersBlocked = state.trackersBlocked,
                accentColor = state.accentColor,
                onToggleAdBlock = { viewModel.toggleAdBlock(it) },
                onDismiss = { viewModel.hideSheet("adpanel") }
            )
        }
    }
}
