package com.eclipse.browser.ui.viewmodel

import android.app.Application
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.eclipse.browser.data.StorageManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

data class TabInfo(
    val id: Int,
    var url: String,
    var title: String,
    val incognito: Boolean = false
)

data class QuickSite(val url: String, val label: String)

data class BookmarkItem(val url: String, val title: String, val time: Long)

data class HistoryItem(val url: String, val title: String, val time: Long)

data class WeatherData(
    val temp: String = "--°",
    val condition: String = "",
    val icon: String = "01d",
    val city: String = ""
)

data class EclipseUiState(
    val greeting: String = "Good Morning",
    val greetingWord: String = "Morning",
    val clock: String = "--:-- --",
    val date: String = "",
    val isNight: Boolean = false,
    val accentColor: Color = Color(0xFFFF6B1A),
    val accentColorLight: Color = Color(0xFFFFB347),
    val accentHex: String = "#FF6B1A",
    val accentLightHex: String = "#FFB347",
    val bgTheme: String = "eclipse",
    val searchEngine: String = "duckduckgo",
    val particlesOn: Boolean = true,
    val starsOn: Boolean = true,
    val orbOn: Boolean = true,
    val weatherOn: Boolean = true,
    val uiStyle: String = "normal",
    val adBlockOn: Boolean = true,
    val adsBlocked: Int = 0,
    val trackersBlocked: Int = 0,
    val weather: WeatherData = WeatherData(),
    val quickSites: List<QuickSite> = emptyList(),
    val bookmarks: List<BookmarkItem> = emptyList(),
    val history: List<HistoryItem> = emptyList(),
    val toastMessage: String? = null,
    val currentScreen: Screen = Screen.HOME,
    val isIncognito: Boolean = false,
    val tabs: List<TabInfo> = listOf(TabInfo(1, "home", "New Tab")),
    val activeTabId: Int = 1,
    val activeSearchTab: String = "all",
    val webViewUrl: String? = null,
    val showCustomize: Boolean = false,
    val showTabs: Boolean = false,
    val showMenu: Boolean = false,
    val showHistory: Boolean = false,
    val showBookmarks: Boolean = false,
    val showAbout: Boolean = false,
    val showAdPanel: Boolean = false,
)

enum class Screen { HOME, WEBVIEW, INCOGNITO }

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    val storage = StorageManager(application)
    private val client = OkHttpClient()
    private var tabIdCounter = 1

    private val _uiState = MutableStateFlow(EclipseUiState())
    val uiState: StateFlow<EclipseUiState> = _uiState.asStateFlow()

    val engines = mapOf(
        "duckduckgo" to "https://html.duckduckgo.com/html/?q=",
        "google" to "https://www.google.com/search?q=",
        "bing" to "https://www.bing.com/search?q=",
        "brave" to "https://search.brave.com/search?q="
    )

    init {
        updateClock()
        collectSettings()
        startClockUpdater()
        fetchWeather()
    }

    private fun collectSettings() {
        viewModelScope.launch {
            storage.accentColor.collect { hex ->
                val color = parseColor(hex)
                _uiState.update { it.copy(accentColor = color, accentHex = hex) }
            }
        }
        viewModelScope.launch {
            storage.accentColorLight.collect { hex ->
                val color = parseColor(hex)
                _uiState.update { it.copy(accentColorLight = color, accentLightHex = hex) }
            }
        }
        viewModelScope.launch { storage.bgTheme.collect { v -> _uiState.update { it.copy(bgTheme = v) } } }
        viewModelScope.launch { storage.searchEngine.collect { v -> _uiState.update { it.copy(searchEngine = v) } } }
        viewModelScope.launch { storage.particlesOn.collect { v -> _uiState.update { it.copy(particlesOn = v) } } }
        viewModelScope.launch { storage.starsOn.collect { v -> _uiState.update { it.copy(starsOn = v) } } }
        viewModelScope.launch { storage.orbOn.collect { v -> _uiState.update { it.copy(orbOn = v) } } }
        viewModelScope.launch { storage.weatherOn.collect { v -> _uiState.update { it.copy(weatherOn = v) } } }
        viewModelScope.launch { storage.uiStyle.collect { v -> _uiState.update { it.copy(uiStyle = v) } } }
        viewModelScope.launch { storage.adBlockOn.collect { v -> _uiState.update { it.copy(adBlockOn = v) } } }
        viewModelScope.launch { storage.adsBlocked.collect { v -> _uiState.update { it.copy(adsBlocked = v) } } }
        viewModelScope.launch { storage.trackersBlocked.collect { v -> _uiState.update { it.copy(trackersBlocked = v) } } }
        viewModelScope.launch {
            storage.quickSites.collect { json ->
                val sites = parseQuickSites(json)
                _uiState.update { it.copy(quickSites = sites) }
            }
        }
        viewModelScope.launch {
            storage.bookmarks.collect { json ->
                val items = parseBookmarks(json)
                _uiState.update { it.copy(bookmarks = items) }
            }
        }
        viewModelScope.launch {
            storage.history.collect { json ->
                val items = parseHistory(json)
                _uiState.update { it.copy(history = items) }
            }
        }
    }

    private fun startClockUpdater() {
        viewModelScope.launch {
            while (true) {
                delay(30_000)
                updateClock()
            }
        }
    }

    fun updateClock() {
        val now = Calendar.getInstance()
        val h = now.get(Calendar.HOUR_OF_DAY)
        val m = now.get(Calendar.MINUTE)
        val ampm = if (h >= 12) "PM" else "AM"
        val h12 = if (h % 12 == 0) 12 else h % 12
        val clock = "$h12:${m.toString().padStart(2, '0')} $ampm"

        val (greeting, word) = when {
            h in 5..11 -> "Good Morning" to "Morning"
            h in 12..16 -> "Good Afternoon" to "Afternoon"
            h in 17..20 -> "Good Evening" to "Evening"
            else -> "Good Night" to "Night"
        }
        val isNight = h >= 21 || h < 5

        val days = arrayOf("Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")
        val months = arrayOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
        val dayName = days[now.get(Calendar.DAY_OF_WEEK) - 1]
        val monthName = months[now.get(Calendar.MONTH)]
        val dayOfMonth = now.get(Calendar.DAY_OF_MONTH)
        val dateStr = "$dayName · $monthName $dayOfMonth"

        _uiState.update {
            it.copy(
                greeting = greeting,
                greetingWord = word,
                clock = clock,
                date = dateStr,
                isNight = isNight
            )
        }
    }

    fun fetchWeather() {
        viewModelScope.launch {
            try {
                val apiKey = "01b6f7ef5529fb2a06c717f4df5ade5b"
                val url = "https://api.openweathermap.org/data/2.5/weather?q=London&appid=$apiKey&units=metric"
                val request = Request.Builder().url(url).build()
                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: return@launch
                    val json = JSONObject(body)
                    val main = json.getJSONObject("main")
                    val temp = main.getDouble("temp").toInt().toString() + "°"
                    val weather = json.getJSONArray("weather").getJSONObject(0)
                    val condition = weather.getString("main")
                    val icon = weather.getString("icon")
                    val city = json.getString("name")
                    _uiState.update {
                        it.copy(weather = WeatherData(temp, condition, icon, city))
                    }
                }
            } catch (_: Exception) {
                // Weather unavailable — show default
            }
        }
    }

    fun setAccentColor(hex: String, lightHex: String) {
        viewModelScope.launch {
            storage.save(StorageManager.ACCENT_COLOR, hex)
            storage.save(StorageManager.ACCENT_COLOR_LIGHT, lightHex)
        }
    }

    fun setTheme(theme: String) {
        viewModelScope.launch { storage.save(StorageManager.BG_THEME, theme) }
    }

    fun setSearchEngine(engine: String) {
        viewModelScope.launch { storage.save(StorageManager.SEARCH_ENGINE, engine) }
    }

    fun toggleParticles(on: Boolean) {
        viewModelScope.launch { storage.save(StorageManager.PARTICLES_ON, on) }
    }

    fun toggleStars(on: Boolean) {
        viewModelScope.launch { storage.save(StorageManager.STARS_ON, on) }
    }

    fun toggleOrb(on: Boolean) {
        viewModelScope.launch { storage.save(StorageManager.ORB_ON, on) }
    }

    fun toggleWeather(on: Boolean) {
        viewModelScope.launch { storage.save(StorageManager.WEATHER_ON, on) }
    }

    fun setUiStyle(style: String) {
        viewModelScope.launch { storage.save(StorageManager.UI_STYLE, style) }
    }

    fun toggleAdBlock(on: Boolean) {
        viewModelScope.launch { storage.save(StorageManager.AD_BLOCK_ON, on) }
    }

    fun buildSearchUrl(query: String): String {
        val state = _uiState.value
        val engine = state.searchEngine
        val tab = state.activeSearchTab
        val base = engines[engine] ?: engines["duckduckgo"]!!
        var url = base + java.net.URLEncoder.encode(query, "UTF-8")

        when (engine) {
            "duckduckgo" -> when (tab) {
                "news" -> url += "&iar=news&ia=news"
                "images" -> url += "&iar=images&iax=images&ia=images"
                "videos" -> url += "&iar=videos&iax=videos&ia=videos"
                "ai" -> url = "https://duckduckgo.com/?q=${java.net.URLEncoder.encode(query, "UTF-8")}&ia=chat"
            }
            "google" -> when (tab) {
                "news" -> url += "&tbm=nws"
                "images" -> url += "&tbm=isch"
                "videos" -> url += "&tbm=vid"
            }
            "bing" -> when (tab) {
                "images" -> url += "&qft=filterui:photo-photo"
                "videos" -> url += "&qft=filterui:video-video"
            }
        }
        return url
    }

    fun navigateTo(url: String) {
        _uiState.update { it.copy(webViewUrl = url, currentScreen = Screen.WEBVIEW) }
        addToHistory(url, "")
    }

    fun goHome() {
        _uiState.update { it.copy(currentScreen = if (it.isIncognito) Screen.INCOGNITO else Screen.HOME, webViewUrl = null) }
        updateClock()
    }

    fun enterIncognito() {
        val id = ++tabIdCounter
        val newTabs = _uiState.value.tabs + TabInfo(id, "home", "Void Tab", incognito = true)
        _uiState.update { it.copy(isIncognito = true, currentScreen = Screen.INCOGNITO, tabs = newTabs, activeTabId = id) }
    }

    fun exitIncognito() {
        val tabs = _uiState.value.tabs.filter { !it.incognito }
        val activeId = tabs.lastOrNull()?.id ?: 1
        _uiState.update { it.copy(isIncognito = false, currentScreen = Screen.HOME, tabs = tabs, activeTabId = activeId) }
    }

    fun newTab() {
        val id = ++tabIdCounter
        val newTabs = _uiState.value.tabs + TabInfo(id, "home", "New Tab")
        _uiState.update { it.copy(tabs = newTabs, activeTabId = id, currentScreen = Screen.HOME, webViewUrl = null) }
    }

    fun closeTab(tabId: Int) {
        val tabs = _uiState.value.tabs.toMutableList()
        if (tabs.size <= 1) {
            showToast("Cannot close last tab")
            return
        }
        tabs.removeAll { it.id == tabId }
        val activeId = if (_uiState.value.activeTabId == tabId) tabs.last().id else _uiState.value.activeTabId
        _uiState.update { it.copy(tabs = tabs, activeTabId = activeId) }
    }

    fun switchToTab(tabId: Int) {
        val tab = _uiState.value.tabs.find { it.id == tabId } ?: return
        if (tab.incognito) {
            _uiState.update { it.copy(activeTabId = tabId, isIncognito = true, currentScreen = Screen.INCOGNITO) }
        } else {
            if (tab.url == "home") {
                _uiState.update { it.copy(activeTabId = tabId, isIncognito = false, currentScreen = Screen.HOME, webViewUrl = null) }
            } else {
                _uiState.update { it.copy(activeTabId = tabId, isIncognito = false, currentScreen = Screen.WEBVIEW, webViewUrl = tab.url) }
            }
        }
    }

    fun updateTabInfo(url: String, title: String) {
        val tabs = _uiState.value.tabs.toMutableList()
        val idx = tabs.indexOfFirst { it.id == _uiState.value.activeTabId }
        if (idx >= 0) {
            tabs[idx] = tabs[idx].copy(url = url, title = title)
            _uiState.update { it.copy(tabs = tabs) }
        }
    }

    fun setActiveSearchTab(tab: String) {
        _uiState.update { it.copy(activeSearchTab = tab) }
    }

    // Quick Sites
    fun removeQuickSite(index: Int) {
        viewModelScope.launch {
            val sites = _uiState.value.quickSites.toMutableList()
            if (index in sites.indices) {
                sites.removeAt(index)
                storage.save(StorageManager.QUICK_SITES, quickSitesToJson(sites))
            }
        }
    }

    fun addQuickSite(url: String, label: String) {
        viewModelScope.launch {
            val sites = _uiState.value.quickSites.toMutableList()
            val normalizedUrl = if (url.startsWith("http")) url else "https://$url"
            val name = label.ifBlank {
                try { java.net.URI(normalizedUrl).host?.replace("www.", "") ?: url.take(15) } catch (_: Exception) { url.take(15) }
            }
            sites.add(QuickSite(normalizedUrl, name))
            storage.save(StorageManager.QUICK_SITES, quickSitesToJson(sites))
            showToast("$name added")
        }
    }

    // Bookmarks
    fun addBookmark(url: String, title: String) {
        viewModelScope.launch {
            val bookmarks = _uiState.value.bookmarks.toMutableList()
            if (bookmarks.any { it.url == url }) {
                showToast("Already bookmarked")
                return@launch
            }
            bookmarks.add(BookmarkItem(url, title.ifBlank { url }, System.currentTimeMillis()))
            storage.save(StorageManager.BOOKMARKS, bookmarksToJson(bookmarks))
            showToast("★ Bookmarked")
        }
    }

    fun removeBookmark(url: String) {
        viewModelScope.launch {
            val bookmarks = _uiState.value.bookmarks.filter { it.url != url }
            storage.save(StorageManager.BOOKMARKS, bookmarksToJson(bookmarks))
            showToast("Removed")
        }
    }

    // History
    fun addToHistory(url: String, title: String) {
        if (url.startsWith("file://") || _uiState.value.isIncognito) return
        viewModelScope.launch {
            val history = _uiState.value.history.toMutableList()
            history.removeAll { it.url == url }
            history.add(0, HistoryItem(url, title.ifBlank { url }, System.currentTimeMillis()))
            if (history.size > 150) history.subList(150, history.size).clear()
            storage.save(StorageManager.HISTORY, historyToJson(history))
        }
    }

    fun deleteHistoryItem(index: Int) {
        viewModelScope.launch {
            val history = _uiState.value.history.toMutableList()
            if (index in history.indices) {
                history.removeAt(index)
                storage.save(StorageManager.HISTORY, historyToJson(history))
            }
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            storage.clearHistory()
            showToast("History cleared")
        }
    }

    // Sheets
    fun showSheet(sheet: String) {
        _uiState.update {
            when (sheet) {
                "customize" -> it.copy(showCustomize = true)
                "tabs" -> it.copy(showTabs = true)
                "menu" -> it.copy(showMenu = true)
                "history" -> it.copy(showHistory = true)
                "bookmarks" -> it.copy(showBookmarks = true)
                "about" -> it.copy(showAbout = true)
                "adpanel" -> it.copy(showAdPanel = true)
                else -> it
            }
        }
    }

    fun hideSheet(sheet: String) {
        _uiState.update {
            when (sheet) {
                "customize" -> it.copy(showCustomize = false)
                "tabs" -> it.copy(showTabs = false)
                "menu" -> it.copy(showMenu = false)
                "history" -> it.copy(showHistory = false)
                "bookmarks" -> it.copy(showBookmarks = false)
                "about" -> it.copy(showAbout = false)
                "adpanel" -> it.copy(showAdPanel = false)
                else -> it
            }
        }
    }

    fun showToast(msg: String) {
        _uiState.update { it.copy(toastMessage = msg) }
        viewModelScope.launch {
            delay(2200)
            _uiState.update { it.copy(toastMessage = null) }
        }
    }

    fun incrementAdStats() {
        viewModelScope.launch {
            val ads = _uiState.value.adsBlocked + 1
            val trackers = _uiState.value.trackersBlocked + 1
            storage.save(StorageManager.ADS_BLOCKED, ads)
            storage.save(StorageManager.TRACKERS_BLOCKED, trackers)
        }
    }

    // Parsing helpers
    private fun parseColor(hex: String): Color {
        return try {
            Color(android.graphics.Color.parseColor(hex))
        } catch (_: Exception) {
            Color(0xFFFF6B1A)
        }
    }

    private fun parseQuickSites(json: String): List<QuickSite> {
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).map { i ->
                val obj = arr.getJSONObject(i)
                QuickSite(obj.getString("url"), obj.getString("label"))
            }
        } catch (_: Exception) { emptyList() }
    }

    private fun parseBookmarks(json: String): List<BookmarkItem> {
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).map { i ->
                val obj = arr.getJSONObject(i)
                BookmarkItem(
                    obj.getString("url"),
                    obj.optString("title", obj.getString("url")),
                    obj.optLong("time", 0)
                )
            }
        } catch (_: Exception) { emptyList() }
    }

    private fun parseHistory(json: String): List<HistoryItem> {
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).map { i ->
                val obj = arr.getJSONObject(i)
                HistoryItem(
                    obj.getString("url"),
                    obj.optString("title", obj.getString("url")),
                    obj.optLong("time", 0)
                )
            }
        } catch (_: Exception) { emptyList() }
    }

    private fun quickSitesToJson(sites: List<QuickSite>): String {
        val arr = JSONArray()
        sites.forEach { s ->
            arr.put(JSONObject().put("url", s.url).put("label", s.label))
        }
        return arr.toString()
    }

    private fun bookmarksToJson(items: List<BookmarkItem>): String {
        val arr = JSONArray()
        items.forEach { b ->
            arr.put(JSONObject().put("url", b.url).put("title", b.title).put("time", b.time))
        }
        return arr.toString()
    }

    private fun historyToJson(items: List<HistoryItem>): String {
        val arr = JSONArray()
        items.forEach { h ->
            arr.put(JSONObject().put("url", h.url).put("title", h.title).put("time", h.time))
        }
        return arr.toString()
    }
}
